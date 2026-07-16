package de.loems.app.battle

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import de.loems.app.domain.EvolutionPath
import de.loems.app.domain.LoemBattle
import de.loems.app.domain.LoemBattleResult
import de.loems.app.domain.LoemBattleSnapshot
import de.loems.app.domain.LoemElement
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.Base64
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class LocalBattleOpponent(
    val id: String,
    val displayName: String,
)

data class PendingLocalChallenge(
    val opponentName: String,
    val element: LoemElement,
    val strength: Int,
    val defense: Int,
)

data class LocalBattleEvent(
    val id: String,
    val result: LoemBattleResult,
)

data class LocalBattleUiState(
    val visible: Boolean = false,
    val busy: Boolean = false,
    val opponents: List<LocalBattleOpponent> = emptyList(),
    val pendingChallenge: PendingLocalChallenge? = null,
    val resultEvent: LocalBattleEvent? = null,
    val error: String? = null,
)

class LocalBattleManager(
    context: Context,
    private val onBattleResult: suspend (LocalBattleEvent) -> Unit = {},
) {
    private val nsdManager = context.getSystemService(NsdManager::class.java)
    private val multicastLock =
        (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
            .createMulticastLock("loems-battle")
            .apply { setReferenceCounted(false) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val deviceToken = UUID.randomUUID().toString().take(8)
    private val services = linkedMapOf<String, NsdServiceInfo>()
    private val _state = MutableStateFlow(LocalBattleUiState())
    val state: StateFlow<LocalBattleUiState> = _state.asStateFlow()

    @Volatile
    private var localSnapshot: LoemBattleSnapshot? = null
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var pendingDecision: CompletableDeferred<Boolean>? = null

    @Volatile
    private var registeredServiceName: String? = null

    fun updateSnapshot(snapshot: LoemBattleSnapshot) {
        localSnapshot = snapshot
    }

    fun start(snapshot: LoemBattleSnapshot) {
        if (_state.value.visible) return
        if (snapshot.evolution < 1) {
            _state.value = LocalBattleUiState(error = "Kämpfe sind erst nach der ersten Evolution möglich.")
            return
        }
        if (snapshot.health < LoemBattle.MIN_BATTLE_HEALTH) {
            _state.value = LocalBattleUiState(
                error = "Dein Löm braucht mindestens 15 % Gesundheit für Kämpfe.",
            )
            return
        }
        localSnapshot = snapshot
        try {
            if (!multicastLock.isHeld) multicastLock.acquire()
            val server = ServerSocket(0)
            serverSocket = server
            acceptJob = scope.launch { acceptLoop(server) }
            registerService(snapshot, server.localPort)
            startDiscovery()
            _state.value = LocalBattleUiState(visible = true)
        } catch (error: Exception) {
            stop()
            _state.value = LocalBattleUiState(error = error.userMessage())
        }
    }

    fun stop() {
        registrationListener?.let { runCatching { nsdManager.unregisterService(it) } }
        discoveryListener?.let { runCatching { nsdManager.stopServiceDiscovery(it) } }
        registrationListener = null
        discoveryListener = null
        registeredServiceName = null
        acceptJob?.cancel()
        acceptJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        if (multicastLock.isHeld) multicastLock.release()
        synchronized(services) { services.clear() }
        pendingDecision?.complete(false)
        pendingDecision = null
        _state.value = LocalBattleUiState()
    }

    fun challenge(opponentId: String) {
        val snapshot = localSnapshot ?: return
        val service = synchronized(services) { services[opponentId] } ?: return
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, error = null, resultEvent = null)
        scope.launch {
            val resolved = runCatching { resolve(service) }.getOrNull()
            if (resolved == null) {
                _state.value = _state.value.copy(busy = false, error = "Gegner nicht erreichbar.")
                return@launch
            }
            runCatching {
                val matchId = UUID.randomUUID().toString()
                Socket(resolved.host, resolved.port).use { socket ->
                    socket.soTimeout = 35_000
                    val writer = socket.writer()
                    val reader = socket.reader()
                    writer.writeLine(BattleProtocol.challenge(matchId, snapshot))
                    when (val response = reader.readLine()) {
                        "DECLINED" -> _state.value = _state.value.copy(
                            busy = false,
                            error = "Die Herausforderung wurde abgelehnt.",
                        )
                        null -> error("Verbindung wurde beendet.")
                        else -> {
                            val result = BattleProtocol.result(response, snapshot.name)
                                ?: error("Ungültige Kampfantwort.")
                            val event = LocalBattleEvent(matchId, result)
                            onBattleResult(event)
                            _state.value = _state.value.copy(
                                busy = false,
                                resultEvent = event,
                            )
                        }
                    }
                }
            }.onFailure { error ->
                _state.value = _state.value.copy(busy = false, error = error.userMessage())
            }
        }
    }

    fun respondToChallenge(accept: Boolean) {
        pendingDecision?.complete(accept)
    }

    fun consumeResult(eventId: String) {
        if (_state.value.resultEvent?.id == eventId) {
            _state.value = _state.value.copy(resultEvent = null)
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun close() {
        stop()
        scope.cancel()
    }

    private suspend fun acceptLoop(server: ServerSocket) {
        while (!server.isClosed) {
            val socket = runCatching { server.accept() }.getOrNull() ?: break
            scope.launch { handleIncoming(socket) }
        }
    }

    private suspend fun handleIncoming(socket: Socket) {
        socket.use {
            socket.soTimeout = 35_000
            val reader = socket.reader()
            val writer = socket.writer()
            val request = BattleProtocol.parseChallenge(reader.readLine()) ?: return
            if (
                request.snapshot.evolution < 1 ||
                request.snapshot.health < LoemBattle.MIN_BATTLE_HEALTH ||
                (localSnapshot?.evolution ?: 0) < 1 ||
                (localSnapshot?.health ?: 0) < LoemBattle.MIN_BATTLE_HEALTH
            ) {
                writer.writeLine("DECLINED")
                return
            }
            if (pendingDecision != null) {
                writer.writeLine("DECLINED")
                return
            }
            val decision = CompletableDeferred<Boolean>()
            pendingDecision = decision
            _state.value = _state.value.copy(
                pendingChallenge = PendingLocalChallenge(
                    opponentName = request.snapshot.name,
                    element = request.snapshot.element,
                    strength = request.snapshot.strength,
                    defense = request.snapshot.defense,
                ),
                error = null,
            )
            val accepted = withTimeoutOrNull(30_000) { decision.await() } == true
            pendingDecision = null
            _state.value = _state.value.copy(pendingChallenge = null)
            if (!accepted) {
                writer.writeLine("DECLINED")
                return
            }
            val host = localSnapshot ?: run {
                writer.writeLine("DECLINED")
                return
            }
            val hostResult = LoemBattle.resolve(host, request.snapshot, request.matchId)
            val event = LocalBattleEvent(request.matchId, hostResult)
            onBattleResult(event)
            writer.writeLine(BattleProtocol.resultForClient(hostResult, host, request.snapshot))
            _state.value = _state.value.copy(
                resultEvent = event,
            )
        }
    }

    private fun registerService(snapshot: LoemBattleSnapshot, port: Int) {
        val info = NsdServiceInfo().apply {
            serviceName = "Loems-${snapshot.name.take(14)}-$deviceToken"
            serviceType = SERVICE_TYPE
            setPort(port)
            setAttribute(DEVICE_TOKEN_ATTRIBUTE, deviceToken)
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                registeredServiceName = serviceInfo.serviceName
                synchronized(services) { services.remove(serviceInfo.serviceName) }
                publishOpponents()
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                _state.value = _state.value.copy(error = "WLAN-Sichtbarkeit konnte nicht gestartet werden.")
            }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }
        registrationListener = listener
        nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    private fun startDiscovery() {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != SERVICE_TYPE || isOwnService(serviceInfo)) return
                synchronized(services) { services[serviceInfo.serviceName] = serviceInfo }
                publishOpponents()
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                synchronized(services) { services.remove(serviceInfo.serviceName) }
                publishOpponents()
            }
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _state.value = _state.value.copy(error = "WLAN-Suche konnte nicht gestartet werden.")
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }
        discoveryListener = listener
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    private fun publishOpponents() {
        val opponents = synchronized(services) {
            services.keys.map { serviceName ->
                LocalBattleOpponent(
                    id = serviceName,
                    displayName = serviceName.removePrefix("Loems-").substringBeforeLast('-'),
                )
            }
        }
        _state.value = _state.value.copy(opponents = opponents)
    }

    private fun isOwnService(serviceInfo: NsdServiceInfo): Boolean {
        val advertisedToken = serviceInfo.attributes[DEVICE_TOKEN_ATTRIBUTE]
            ?.toString(Charsets.UTF_8)
        return isOwnBattleService(
            serviceName = serviceInfo.serviceName,
            advertisedToken = advertisedToken,
            deviceToken = deviceToken,
            registeredServiceName = registeredServiceName,
        )
    }

    private suspend fun resolve(service: NsdServiceInfo): NsdServiceInfo? =
        suspendCancellableCoroutine { continuation ->
            val completed = AtomicBoolean(false)
            nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    if (completed.compareAndSet(false, true)) continuation.resume(serviceInfo)
                }
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    if (completed.compareAndSet(false, true)) continuation.resume(null)
                }
            })
        }

    private fun Socket.reader() = BufferedReader(InputStreamReader(getInputStream(), Charsets.UTF_8))
    private fun Socket.writer() = BufferedWriter(OutputStreamWriter(getOutputStream(), Charsets.UTF_8))
    private fun BufferedWriter.writeLine(value: String) {
        write(value)
        newLine()
        flush()
    }

    private fun Throwable.userMessage(): String = message?.takeIf { it.isNotBlank() }
        ?: "Lokale Verbindung fehlgeschlagen."

    private companion object {
        const val SERVICE_TYPE = "_loems-battle._tcp."
        const val DEVICE_TOKEN_ATTRIBUTE = "device"
    }
}

internal fun isOwnBattleService(
    serviceName: String,
    advertisedToken: String?,
    deviceToken: String,
    registeredServiceName: String?,
): Boolean = advertisedToken == deviceToken ||
    serviceName == registeredServiceName ||
    serviceName.contains("-$deviceToken")

private data class ChallengeRequest(
    val matchId: String,
    val snapshot: LoemBattleSnapshot,
)

private object BattleProtocol {
    fun challenge(matchId: String, snapshot: LoemBattleSnapshot): String = listOf(
        "CHALLENGE",
        matchId,
        encode(snapshot.name),
        snapshot.element.name,
        snapshot.evolution,
        snapshot.evolutionPath.name,
        snapshot.strength,
        snapshot.defense,
        snapshot.wins,
        snapshot.health,
    ).joinToString("|")

    fun parseChallenge(line: String?): ChallengeRequest? = runCatching {
        val parts = line?.split('|') ?: return null
        if (parts.size != 10 || parts[0] != "CHALLENGE") return null
        ChallengeRequest(
            matchId = parts[1],
            snapshot = LoemBattleSnapshot(
                name = decode(parts[2]).take(20),
                element = LoemElement.valueOf(parts[3]),
                evolution = parts[4].toInt().coerceIn(0, 3),
                evolutionPath = EvolutionPath.valueOf(parts[5]),
                strength = parts[6].toInt().coerceIn(1, 999),
                defense = parts[7].toInt().coerceIn(1, 999),
                wins = parts[8].toInt().coerceIn(0, 999_999),
                health = parts[9].toInt().coerceIn(0, 100),
            ),
        )
    }.getOrNull()

    fun resultForClient(
        hostResult: LoemBattleResult,
        host: LoemBattleSnapshot,
        client: LoemBattleSnapshot,
    ): String = listOf(
        "RESULT",
        encode(host.name),
        (!hostResult.won).toString(),
        hostResult.opponentPower,
        hostResult.localPower,
        LoemBattle.elementModifier(client.element, host.element),
        host.element.name,
        1f - hostResult.localWinChance,
        client.defense,
    ).joinToString("|")

    fun result(line: String, localName: String): LoemBattleResult? = runCatching {
        val parts = line.split('|')
        if (parts.size != 9 || parts[0] != "RESULT") return null
        val won = parts[2].toBooleanStrict()
        LoemBattleResult(
            opponentName = decode(parts[1]).take(20).ifBlank { localName },
            opponentElement = LoemElement.valueOf(parts[6]),
            won = won,
            localPower = parts[3].toFloat(),
            opponentPower = parts[4].toFloat(),
            localElementModifier = parts[5].toFloat(),
            localWinChance = parts[7].toFloat().coerceIn(0f, 1f),
            localDefense = parts[8].toInt().coerceIn(0, 999),
        )
    }.getOrNull()

    private fun encode(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun decode(value: String): String =
        String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
}

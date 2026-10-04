# Kampfanimationen — Abschlussprüfung, 13.09.2026

## Umfang

18 neue Zustände: Angriff, Treffer, Doppelangriff, Doppeltreffer, Sieg und Niederlage für Wurst-Löm sowie beide Trübsal-Zauberhaufen-Varianten. Alle sind im normalen Kampf und in der Debug-Auswahl zugeordnet. Männliche und weibliche Zauberhaufen behalten identische Spielwerte und Zeiten.

Zusätzlich wurden der abgeschnittene rechte Flügel beim Ultra-Raumriss-Doppelangriff neu gezeichnet, die Hautfarben dieses Angriffs und des Urkröten-Angriffs korrigiert und fünf Erzmagier-Posen farblich bereinigt. Die beiden gelben Erzmagier-Trefferblitze sind entfernt. Die zuvor falsch am männlichen Sturmkaiser eingetragene weibliche Niederlage ist jetzt ausschließlich der Sturmkaiserin zugeordnet.

Die vollständige Liste aller 19 Formen steht in [animation-inventory.md](animation-inventory.md). Alle 18 kampffähigen Formen verfügen über sechs Kampfzustände; das junge Löm kann nicht kämpfen.

## Grafiken und Prüfung

- Produktionsformat: transparentes, verlustfreies WebP. Neue Wurst-/Zauberhaufen-Bögen: 1536×1024, sechs 512px-Zellen; Raumriss: 2048×1536, zwölf Zellen.
- Es handelt sich um Key-Pose-Animationen: eigene Zustandszeichnungen mit kontrollierten Bewegungen und wiederholten Bildern, nicht um sechs bzw. zwölf neu gezeichnete Bewegungsphasen. Doppelaktionen enthalten zwei Impulse. Sieg und Niederlage spielen einmal und halten die Endpose.
- Feste Spielzuschnitte: Wurst `(48,128,416,336)`, Zauberhaufen `(48,48,416,416)`, Ultra `(48,96,416,368)`, Urkröte `(48,176,416,288)`, Weltschlange `(48,160,416,304)`, Erzmagier `(48,48,416,416)`; jeweils mit Zellversatz. Derselbe Ausschnitt bleibt innerhalb einer Folge unverändert.
- Die 18 neuen Bögen sowie die 16 Raumriss-Bögen werden mit mindestens 48px Zellrand, Alpha-Schwelle 0, kontrollierten Kampfbewegungen und zusätzlichem Spielzuschnitt-Test geprüft. Verstecktes RGB unter Alpha 0 wird bereinigt; WebP-Rücklesen ist pixelidentisch.
- Visuelle Prüfung: vollständige Bildfolgen auf dunklem Grund und Test mit exakt der grünen Laufzeit-Einfärbung. Keine abgeschnittenen Flügel/Hüte, fremden Bildzellen, Hintergrundmuster oder eigenen Geschosse in den geprüften Zuständen. Farben von Kleidung und Portalen bleiben erhalten. Niedrige Angriffs-/Niederlagenposen ändern bewusst die Gesamthöhe; der Ultra-Doppelangriff besitzt eine breitere, niedrigere Flügelsilhouette.
- Anatomie: Wurst ein Kopf, zwei Augen (fernes verdeckt), vier Beine (ferne perspektivisch teilweise verdeckt), ein Zahn, kurzer runder Hinterkörper. Zauberhaufen ein Kopf/Körper, zwei Augen, zwei Arme/Hände, zwei Füße, Hut und ein rechts gehaltener Stab mit einem unteren Kristall; weiblich mit Wimpern. Raumriss-Drachen/Kröte ein Kopf, zwei Augen, zwei Hörner, zwei Flügel, vier Beine, ein Schwanz. Erzmagier zwei Arme/Hände, zwei Füße, ein Hut, Stab links und gehaltenes Ornament rechts. Wiederholte Bilder behalten die Anatomie ihrer jeweiligen gezeichneten Pose bei.
- Die numerischen Generatorberichte enthalten keine automatische visuelle Freigabe. Diese manuelle Prüfung ergänzt sie; Messwerte allein reichen ausdrücklich nicht.

Prüfberichte: [Wurst](../art/qa/bad-battle/frame-report.md), [Zauberhaufen](../art/qa/gloom-battle/frame-report.md), [Raumriss](../art/qa/projectiles/frame-report.md).

GIF-Galerien: [Wurst](../art/qa/bad-battle/index.html), [Zauberhaufen](../art/qa/gloom-battle/index.html), [Raumriss](../art/qa/projectiles/index.html). Sieg/Niederlage halten ihr letztes Bild; zum Wiederholen die Seite neu laden.

## Herkunft und Prompt-Satz

Verwendet: die Skills `sprite-generator` und `imagegen`, mit eingebauter Bildgenerierung (kein API-/CLI-Fallback). Ausgewählte Quellen liegen unter `art/intermediate/loem_bad_{state}_canonical.png`, `loem_gloom_wizard_poop_{gender}_{state}_canonical.png` und den bestehenden Raumriss-Namen; die endgültigen Bögen liegen in `app/src/main/res/drawable-nodpi/`.

Gemeinsamer Generierungsauftrag: dieselbe Figur und Anatomie wie die Idle-Referenz, vollständige Silhouette und transparente Ränder, neutrale graue Haut (gleiche RGB-Kanäle, 110–215), originale farbige Details. Keine abgefeuerten Projektile, Strahlen, Energiekugeln, Schussspuren oder Symbole. Zustände nur über Haltung, Gliedmaßen, Mund und Rückstoß ausdrücken. Transparenter RGBA-Hintergrund.

| Zustand | Wurst-Löm | Zauberhaufen |
|---|---|---|
| Angriff | Geduckter Kopfstoß, offener Mund | Entschlossene Haltung, Faust neben Brust |
| Treffer | Zurückweichen, Auge zu, Grimasse | Hand an Wange, zusammengekniffene Augen |
| Doppelangriff | Stärkerer Ausfallschritt, Vorderpfote vor | Stärkere Hocke, Faust vor, rufender Mund |
| Doppeltreffer | Tief geduckt, Kopf gesenkt | Sitzender Rückstoß, Hand am Bauch |
| Sieg | Freudig aufgerichtet, Vorderpfote gehoben | Lächeln, freie Faust gehoben |
| Niederlage | Flach auf Bauch, Kopf unten, Auge geschlossen | Zusammengesackt sitzend, Augen geschlossen, Stab tief |

Korrekturauftrag Raumriss: ausschließlich rosige/beige Haut in neutrales Grau umzeichnen, farbige Ausstattung bewahren. Ultra-Flügel vollständig neu zeichnen, dann mit freien Rändern freistellen. Erfolgreicher Freistellungsauftrag: „Isolate the wizard/toad/dragon on a transparent background“, bei breiten Figuren ergänzt um kleine zentrierte Größe und breite freie Ränder. RGB-Schachbrett- und abgeschnittene Zwischenergebnisse wurden verworfen, nicht als fertige Assets übernommen.

## Build

Abgeschlossen: `testDebugUnitTest assembleDebug` erfolgreich; 111 Tests, keine Fehler. Der zusätzliche Zuordnungs-/Dateiabgleich `tools/verify_battle_animation_completion.py` besteht: 18 neue Zustände korrekt in Gameplay und Debug, richtige Geschlechterzuordnung der Niederlagen, 34 geprüfte Bögen identisch zu den Produktionsdateien, keine duplizierten Zustandsdateien.

Debug-APK: `app/build/outputs/apk/debug/app-debug.apk`, erstellt am 13.09.2026 um 15:17 Uhr (Europe/Berlin), 90.658.813 Bytes. SHA-256: `DAFCF82F028FD3AC5543DDCC8865C79A4335CACA08F69CAEFFC55F858B5D0CFF`.

Die abschließende Zuordnungsprüfung hat außerdem eine zunächst falsche Wurst-/Baby-Zuordnung erkannt; sie wurde korrigiert und die APK danach erneut gebaut. Die neue Regression prüft diesen Fall ausdrücklich. Auf einem angeschlossenen Android-Gerät oder Emulator wurde dieser Stand nicht durchgespielt.

Alle neuen und überarbeiteten GIFs gemeinsam: [Vorschaugalerie](../art/qa/battle-completion.html).

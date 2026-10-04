package de.loems.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.loems.app.domain.LoemColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

/** A source rectangle inside a sprite sheet. */
internal data class SpriteFrame(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

internal enum class SpriteAnchor {
    BOTTOM_CENTER,
}

/**
 * Complete playback metadata for one animation clip.
 *
 * Legacy sheets contain tightly trimmed rectangles of different sizes. They are expanded to a
 * shared, bottom-centred crop at runtime so the body anchor, ground line and scale stay stable.
 */
internal data class SpriteClip(
    @param:DrawableRes val spriteResource: Int,
    val frames: List<SpriteFrame>,
    val frameDurationMillis: Long = 180L,
    val loop: Boolean = true,
    val anchor: SpriteAnchor = SpriteAnchor.BOTTOM_CENTER,
    val subtleBreathing: Boolean = false,
) {
    init {
        require(frames.isNotEmpty()) { "A sprite clip needs at least one frame" }
        require(frameDurationMillis > 0L) { "Frame duration must be positive" }
    }

    val stableFrames: List<SpriteFrame> = stabilizeSpriteFrames(frames)

    fun frameIndexAt(elapsedMillis: Long): Int {
        val rawIndex = (elapsedMillis / frameDurationMillis).toInt()
        return if (loop) rawIndex % stableFrames.size else rawIndex.coerceAtMost(stableFrames.lastIndex)
    }

    fun holdingLastFrame(): SpriteClip = copy(
        frames = List(frames.size) { frames.last() },
        loop = false,
    )
}

internal data class LoemSpritePresentation(
    val evolution: Int,
    val clip: SpriteClip,
    val sizeDp: Int,
)

internal data class SpriteRenderSize(val width: Int, val height: Int)

internal fun fitSpriteSize(
    sourceWidth: Int,
    sourceHeight: Int,
    availableWidth: Float,
    availableHeight: Float,
    verticalScale: Float = 1f,
): SpriteRenderSize {
    require(sourceWidth > 0 && sourceHeight > 0)
    val fitScale = min(availableWidth / sourceWidth, availableHeight / sourceHeight)
    return SpriteRenderSize(
        width = (sourceWidth * fitScale).roundToInt(),
        height = (sourceHeight * fitScale * verticalScale).roundToInt(),
    )
}

/**
 * Expands each legacy trim to the clip's maximum dimensions, centred horizontally on the old trim
 * and aligned to its bottom edge. The legacy sheets do not all use the same cell dimensions, so
 * their measured frame positions remain the source of truth.
 */
internal fun stabilizeSpriteFrames(
    frames: List<SpriteFrame>,
): List<SpriteFrame> {
    if (frames.isEmpty()) return emptyList()

    val stableWidth = frames.maxOf { it.width }
    val stableHeight = frames.maxOf { it.height }
    return frames.map { frame ->
        SpriteFrame(
            x = (frame.x + (frame.width - stableWidth) / 2).coerceAtLeast(0),
            y = (frame.y + frame.height - stableHeight).coerceAtLeast(0),
            width = stableWidth,
            height = stableHeight,
        )
    }
}

@Composable
internal fun LoemSprite(
    presentation: LoemSpritePresentation,
    color: LoemColor,
    animationKey: Int = 0,
    sizeDpOverride: Int? = null,
    animate: Boolean = true,
) {
    val context = LocalContext.current
    val clip = presentation.clip
    var frameIndex by remember(clip.spriteResource, animationKey) { mutableIntStateOf(0) }
    val spriteFrames by produceState<List<ImageBitmap>?>(
        initialValue = null,
        color,
        clip.spriteResource,
        clip.stableFrames,
        animate,
    ) {
        value = withContext(Dispatchers.Default) {
            val decoded = BitmapFactory.decodeResource(context.resources, clip.spriteResource)
            val recolored = recolorBody(decoded, color)
            if (decoded !== recolored) decoded.recycle()
            val framesToDecode = if (animate) clip.stableFrames else clip.stableFrames.take(1)
            val isolatedFrames = framesToDecode.map { frame ->
                Bitmap.createBitmap(
                    recolored,
                    frame.x,
                    frame.y,
                    frame.width,
                    frame.height,
                ).asImageBitmap()
            }
            isolatedFrames
        }
    }

    LaunchedEffect(spriteFrames, clip, animationKey, animate) {
        frameIndex = 0
        if (!animate || spriteFrames == null || clip.stableFrames.size == 1) return@LaunchedEffect
        val startedAt = withFrameNanos { it }
        while (clip.loop || frameIndex < clip.stableFrames.lastIndex) {
            withFrameNanos { now ->
                frameIndex = clip.frameIndexAt((now - startedAt) / 1_000_000L)
            }
        }
    }

    Canvas(
        modifier = Modifier.size(
            (sizeDpOverride ?: (presentation.sizeDp + presentation.evolution * 8)).dp,
        ),
    ) {
        spriteFrames?.let { frames ->
            val currentFrame = frames[frameIndex]
            val breathingScale = if (clip.subtleBreathing) {
                listOf(1f, 1.006f, 1.012f, 1.012f, 1.006f, 1f)[frameIndex % 6]
            } else {
                1f
            }
            val destination = fitSpriteSize(
                sourceWidth = currentFrame.width,
                sourceHeight = currentFrame.height,
                availableWidth = size.width,
                availableHeight = size.height,
                verticalScale = breathingScale,
            )
            val destinationOffset = when (clip.anchor) {
                SpriteAnchor.BOTTOM_CENTER -> IntOffset(
                    x = ((size.width - destination.width) / 2f).roundToInt(),
                    y = size.height.roundToInt() - destination.height,
                )
            }
            drawImage(
                image = currentFrame,
                dstOffset = destinationOffset,
                dstSize = IntSize(destination.width, destination.height),
            )
        }
    }
}

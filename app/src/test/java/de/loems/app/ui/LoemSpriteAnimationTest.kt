package de.loems.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoemSpriteAnimationTest {
    @Test
    fun `legacy trims become equal bottom anchored crops`() {
        val original = listOf(
            SpriteFrame(129, 136, 366, 324),
            SpriteFrame(570, 136, 350, 320),
            SpriteFrame(1017, 562, 360, 331),
        )

        val stable = stabilizeSpriteFrames(original)

        assertTrue(stable.all { it.width == 366 && it.height == 331 })
        original.zip(stable).forEach { (before, after) ->
            assertEquals(before.y + before.height, after.y + after.height)
        }
    }

    @Test
    fun `stable crops preserve horizontal centres`() {
        val original =
            listOf(
                SpriteFrame(114, 88, 397, 370),
                SpriteFrame(552, 87, 382, 372),
                SpriteFrame(982, 86, 395, 372),
            )
        val stable = stabilizeSpriteFrames(
            original,
        )

        original.zip(stable).forEach { (before, after) ->
            assertTrue(kotlin.math.abs((before.x + before.width / 2) - (after.x + after.width / 2)) <= 1)
        }
    }

    @Test
    fun `clip playback loops or holds its final frame`() {
        val frames = List(6) { index -> SpriteFrame(index * 512, 0, 400, 300) }
        val looping = SpriteClip(spriteResource = 1, frames = frames, frameDurationMillis = 100L)
        val once = looping.copy(loop = false)

        assertEquals(2, looping.frameIndexAt(200L))
        assertEquals(0, looping.frameIndexAt(600L))
        assertEquals(5, once.frameIndexAt(10_000L))
    }

    @Test
    fun `renderer preserves source aspect ratio`() {
        val destination = fitSpriteSize(
            sourceWidth = 416,
            sourceHeight = 320,
            availableWidth = 220f,
            availableHeight = 220f,
        )

        assertEquals(220, destination.width)
        assertEquals(169, destination.height)
    }
}

package com.watchmusic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentColorTest {

    @Test
    fun clampAccent_keepsAlphaAndMovesIntoReadableRange() {
        val result = ColorMath.clampAccent(0x7C5CFF)
        assertEquals(0xFF, (result ushr 24) and 0xFF)

        val hsl = ColorMath.rgbToHsl(
            ((result shr 16) and 0xFF) / 255f,
            ((result shr 8) and 0xFF) / 255f,
            (result and 0xFF) / 255f,
        )
        assertTrue("saturation=${hsl[1]}", hsl[1] in 0.44f..0.96f)
        assertTrue("lightness=${hsl[2]}", hsl[2] in 0.54f..0.79f)
    }

    @Test
    fun clampAccent_handlesGrayscaleAndBlack() {
        val black = ColorMath.clampAccent(0xFF000000.toInt())
        assertEquals(0xFF, (black ushr 24) and 0xFF)

        val white = ColorMath.clampAccent(0xFFFFFFFF.toInt())
        assertEquals(0xFF, (white ushr 24) and 0xFF)
    }

    @Test
    fun rgbToHsl_matchesPrimaryColors() {
        val red = ColorMath.rgbToHsl(1f, 0f, 0f)
        assertEquals(0f, red[0], 0.001f)
        assertEquals(1f, red[1], 0.001f)
        assertEquals(0.5f, red[2], 0.001f)

        val green = ColorMath.rgbToHsl(0f, 1f, 0f)
        assertEquals(1f / 3f, green[0], 0.001f)
    }

    @Test
    fun hslToArgb_roundTripsSaturatedColor() {
        val argb = ColorMath.hslToArgb(1f / 3f, 1f, 0.5f)
        assertEquals(0xFF, (argb ushr 24) and 0xFF)
        assertEquals(0x00, (argb shr 16) and 0xFF)
        assertEquals(0xFF, (argb shr 8) and 0xFF)
        assertEquals(0x00, argb and 0xFF)
    }

    @Test
    fun seededColors_areDeterministicAndDistinct() {
        assertEquals(AccentColor.seededColors(12), AccentColor.seededColors(12))
        assertTrue(AccentColor.seededColors(12) != AccentColor.seededColors(200))
        assertEquals(AccentColor.seededColors(-30), AccentColor.seededColors(330))
    }
}

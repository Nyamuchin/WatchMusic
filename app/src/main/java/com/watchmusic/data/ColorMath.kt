package com.watchmusic.data

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pure color math (no Android dependencies) so the accent extraction can be unit
 * tested on the JVM.
 */
object ColorMath {

    /**
     * Normalizes an arbitrary ARGB color into an accent that stays readable on the
     * near-black watch background: saturated enough to pop, light enough to be
     * seen, but never washed out.
     */
    fun clampAccent(argb: Int): Int {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val hsl = rgbToHsl(r, g, b)
        val hue = hsl[0]
        val saturation = hsl[1].coerceIn(MIN_SATURATION, MAX_SATURATION)
        val lightness = hsl[2].coerceIn(MIN_LIGHTNESS, MAX_LIGHTNESS)
        return hslToArgb(hue, saturation, lightness)
    }

    fun rgbToHsl(r: Float, g: Float, b: Float): FloatArray {
        val max = max(r, max(g, b))
        val min = min(r, min(g, b))
        val lightness = (max + min) / 2f
        if (max == min) return floatArrayOf(0f, 0f, lightness)
        val delta = max - min
        val saturation = if (lightness > 0.5f) {
            delta / (2f - max - min)
        } else {
            delta / (max + min)
        }
        val hue = when (max) {
            r -> ((g - b) / delta + if (g < b) 6f else 0f)
            g -> ((b - r) / delta + 2f)
            else -> ((r - g) / delta + 4f)
        } / 6f
        return floatArrayOf(hue, saturation, lightness)
    }

    fun hslToArgb(hue: Float, saturation: Float, lightness: Float): Int {
        if (saturation <= 0f) {
            val gray = channel(lightness)
            return argb(gray, gray, gray)
        }
        val q = if (lightness < 0.5f) {
            lightness * (1f + saturation)
        } else {
            lightness + saturation - lightness * saturation
        }
        val p = 2f * lightness - q
        val r = hueToChannel(p, q, hue + 1f / 3f)
        val g = hueToChannel(p, q, hue)
        val b = hueToChannel(p, q, hue - 1f / 3f)
        return argb(channel(r), channel(g), channel(b))
    }

    fun argb(red: Int, green: Int, blue: Int): Int =
        (0xFF shl 24) or (red.coerceIn(0, 255) shl 16) or (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)

    private fun hueToChannel(p: Float, q: Float, rawT: Float): Float {
        var t = rawT
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        return when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 1f / 2f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else -> p
        }
    }

    private fun channel(value: Float): Int = (value * 255f).roundToInt().coerceIn(0, 255)

    private const val MIN_SATURATION = 0.45f
    private const val MAX_SATURATION = 0.95f
    private const val MIN_LIGHTNESS = 0.55f
    private const val MAX_LIGHTNESS = 0.78f
}

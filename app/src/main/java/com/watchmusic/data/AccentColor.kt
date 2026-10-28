package com.watchmusic.data

import android.graphics.Bitmap
import kotlin.math.roundToInt

/** Extracts a usable accent color from cover art, with a deterministic fallback. */
object AccentColor {

    /** Weighted saturation average over a downscaled copy of the artwork. */
    fun fromBitmap(bitmap: Bitmap): Int? {
        val side = SAMPLE_SIDE
        val sample = if (bitmap.width == side && bitmap.height == side) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, side, side, false)
        }
        val pixels = IntArray(side * side)
        sample.getPixels(pixels, 0, side, 0, 0, side, side)
        if (sample !== bitmap) sample.recycle()

        var sumRed = 0.0
        var sumGreen = 0.0
        var sumBlue = 0.0
        var totalWeight = 0.0
        for (pixel in pixels) {
            if (((pixel ushr 24) and 0xFF) < 128) continue
            val r = ((pixel shr 16) and 0xFF) / 255.0
            val g = ((pixel shr 8) and 0xFF) / 255.0
            val b = (pixel and 0xFF) / 255.0
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val lightness = (max + min) / 2.0
            if (lightness < 0.1 || lightness > 0.95) continue
            val saturation = if (max == min) 0.0 else {
                if (lightness > 0.5) (max - min) / (2.0 - max - min) else (max - min) / (max + min)
            }
            if (saturation < 0.08) continue
            val weight = 0.2 + saturation * saturation
            sumRed += r * weight
            sumGreen += g * weight
            sumBlue += b * weight
            totalWeight += weight
        }
        if (totalWeight <= 0.0) return null
        val red = (sumRed / totalWeight * 255.0).roundToInt().coerceIn(0, 255)
        val green = (sumGreen / totalWeight * 255.0).roundToInt().coerceIn(0, 255)
        val blue = (sumBlue / totalWeight * 255.0).roundToInt().coerceIn(0, 255)
        return ColorMath.clampAccent(ColorMath.argb(red, green, blue))
    }

    /** Two harmonious colors used to paint the placeholder cover of a track. */
    fun seededColors(seed: Int): Pair<Int, Int> {
        val hue = (((seed % 360) + 360) % 360) / 360f
        val start = ColorMath.hslToArgb(hue, 0.72f, 0.66f)
        val end = ColorMath.hslToArgb((hue + 0.09f) % 1f, 0.62f, 0.38f)
        return start to end
    }

    private const val SAMPLE_SIDE = 24
}

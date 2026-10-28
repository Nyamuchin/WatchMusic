package com.watchmusic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddedMetadataTest {

    @Test
    fun cleanTrimsAndDropsPlaceholders() {
        assertEquals("夜航", TagText.clean("  夜航 "))
        assertNull(TagText.clean(null))
        assertNull(TagText.clean("   "))
        assertNull(TagText.clean("<unknown>"))
        assertNull(TagText.clean("unknown"))
        assertNull(TagText.clean("null"))
    }

    @Test
    fun withoutExtensionUsesFileNameStem() {
        assertEquals("Tagged", TagText.withoutExtension("Tagged.mp3"))
        assertEquals("Aurora", TagText.withoutExtension("Aurora.wav"))
        assertEquals("no-dot-name", TagText.withoutExtension("no-dot-name"))
        assertEquals("a.b", TagText.withoutExtension("a.b.flac"))
        assertNull(TagText.withoutExtension(null))
    }

    @Test
    fun looksUntaggedDetectsMissingAndFileNames() {
        assertTrue(TagText.looksUntagged(null, "Tagged.mp3"))
        assertTrue(TagText.looksUntagged("Tagged", "Tagged.mp3"))
        assertTrue(TagText.looksUntagged("tagged", "Tagged.mp3"))
        assertTrue(TagText.looksUntagged("<unknown>", "Tagged.mp3"))
        assertFalse(TagText.looksUntagged("内嵌标题", "Tagged.mp3"))
        assertFalse(TagText.looksUntagged("Track 01", "Tagged.mp3"))
    }

    @Test
    fun looksUntaggedWithoutFileNameOnlyChecksEmptiness() {
        assertTrue(TagText.looksUntagged(null, null))
        assertFalse(TagText.looksUntagged("Some Title", null))
    }
}

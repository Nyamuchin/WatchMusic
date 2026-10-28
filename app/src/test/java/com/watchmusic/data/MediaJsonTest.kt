package com.watchmusic.data

import com.watchmusic.data.model.Playlist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaJsonTest {

    @Test
    fun ids_roundTrip() {
        val ids = listOf("ms:1", "ms:42", "ms:7")
        assertEquals(ids, MediaJson.decodeIds(MediaJson.encodeIds(ids)))
    }

    @Test
    fun ids_tolerateMissingOrInvalidInput() {
        assertTrue(MediaJson.decodeIds(null).isEmpty())
        assertTrue(MediaJson.decodeIds("").isEmpty())
        assertTrue(MediaJson.decodeIds("not-json").isEmpty())
        assertTrue(MediaJson.decodeIds("{}").isEmpty())
        assertEquals(listOf("a", "b"), MediaJson.decodeIds("""["a","b"]"""))
    }

    @Test
    fun playlists_roundTrip() {
        val playlists = listOf(
            Playlist(id = "p1", name = "夜晚", createdAt = 100L, trackIds = listOf("ms:1", "ms:2")),
            Playlist(id = "p2", name = "通勤", createdAt = 200L, trackIds = emptyList()),
        )
        val decoded = MediaJson.decodePlaylists(MediaJson.encodePlaylists(playlists))
        assertEquals(playlists, decoded)
    }

    @Test
    fun playlists_fillMissingNameAndSkipEntriesWithoutId() {
        val raw = """[{"name":""},{"id":"p1","createdAt":5,"trackIds":["ms:9"]}]"""
        val decoded = MediaJson.decodePlaylists(raw)
        assertEquals(1, decoded.size)
        assertEquals("p1", decoded[0].id)
        assertEquals("未命名歌单", decoded[0].name)
        assertEquals(listOf("ms:9"), decoded[0].trackIds)
    }

    @Test
    fun playlists_tolerateInvalidInput() {
        assertTrue(MediaJson.decodePlaylists(null).isEmpty())
        assertTrue(MediaJson.decodePlaylists("nope").isEmpty())
        assertTrue(MediaJson.decodePlaylists("""[1,2,3]""").isEmpty())
    }
}

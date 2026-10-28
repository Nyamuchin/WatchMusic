package com.watchmusic.data

import com.watchmusic.data.model.Playlist
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tiny JSON codecs for the values persisted in SharedPreferences.
 * Kept free of Android APIs so it can be unit tested directly.
 */
object MediaJson {

    fun decodeIds(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val id = array.optString(i)
                    if (id.isNotBlank()) add(id)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeIds(ids: List<String>): String {
        val array = JSONArray()
        ids.forEach { array.put(it) }
        return array.toString()
    }

    fun decodePlaylists(raw: String?): List<Playlist> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id")
                    if (id.isBlank()) continue
                    add(
                        Playlist(
                            id = id,
                            name = obj.optString("name").ifBlank { "未命名歌单" },
                            createdAt = obj.optLong("createdAt"),
                            trackIds = decodeArray(obj.optJSONArray("trackIds")),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodePlaylists(playlists: List<Playlist>): String {
        val array = JSONArray()
        playlists.forEach { playlist ->
            val obj = JSONObject()
            obj.put("id", playlist.id)
            obj.put("name", playlist.name)
            obj.put("createdAt", playlist.createdAt)
            val ids = JSONArray()
            playlist.trackIds.forEach { ids.put(it) }
            obj.put("trackIds", ids)
            array.put(obj)
        }
        return array.toString()
    }

    private fun decodeArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val id = array.optString(i)
                if (id.isNotBlank()) add(id)
            }
        }
    }
}

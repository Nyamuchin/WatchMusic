package com.watchmusic.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.watchmusic.data.model.Album
import com.watchmusic.data.model.Artist
import com.watchmusic.data.model.Library
import com.watchmusic.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Reads the device audio library through MediaStore, or only a SAF folder tree when
 * the user picked one. No bundled audio: an empty library is a normal state the UI
 * has to handle.
 */
class MediaRepository(
    private val context: Context,
    private val prefs: PrefsStore,
) {

    private val _library = MutableStateFlow<Library?>(null)
    val library: StateFlow<Library?> = _library.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val requiredPermission: String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun hasPermission(): Boolean =
        prefs.libraryFolder.value != null ||
            ContextCompat.checkSelfPermission(context, requiredPermission) ==
            PackageManager.PERMISSION_GRANTED

    fun contentUri(mediaId: Long): Uri =
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId)

    suspend fun refresh(): Result<Library> = withContext(Dispatchers.IO) {
        _loading.value = true
        val folder = prefs.libraryFolder.value
        val result = runCatching {
            if (folder != null) {
                try {
                    queryFolder(Uri.parse(folder))
                } catch (_: SecurityException) {
                    // The SAF grant can be revoked (app data restored, storage remounted).
                    // Drop the dead folder and fall back to the whole media library instead
                    // of leaving the user stuck on an unreadable folder.
                    prefs.setLibraryFolder(null)
                    queryMediaStore()
                }
            } else {
                queryMediaStore()
            }
        }
        result.onSuccess {
            _library.value = it
            _error.value = null
        }.onFailure {
            _error.value = when (it) {
                is SecurityException -> "无法读取所选文件夹"
                is IllegalArgumentException -> "所选文件夹不可用"
                else -> it.message ?: "扫描媒体库失败"
            }
        }
        _loading.value = false
        return@withContext result
    }

    /**
     * Accepts the freshly loaded library when the platform hands it over through
     * a content observer or after a permission grant.
     */
    fun publish(library: Library) {
        _library.value = library
        _error.value = null
    }

    private fun queryMediaStore(): Library {
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
        // The duration filter moved into code: MediaStore reports 0 for files it could not
        // parse (common for freshly copied mp3s), and those must not disappear from the app.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val tracks = mutableListOf<Track>()
        var reads = 0
        resolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val nameColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val mediaId = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(collection, mediaId)
                val fileName = if (nameColumn >= 0) cursor.getString(nameColumn) else null
                val tags = if (reads < MAX_METADATA_READS) {
                    reads++
                    EmbeddedMetadataReader.read(context, uri)
                } else {
                    null
                }
                val durationMs = tags?.durationMs?.takeIf { it > 0L }
                    ?: cursor.getLong(durationColumn).coerceAtLeast(0L)
                if (durationMs in 1 until MIN_DURATION_MS) continue

                val artist = tags?.artist
                    ?: TagText.clean(cursor.getString(artistColumn))
                val album = tags?.album ?: TagText.clean(cursor.getString(albumColumn))
                tracks += Track(
                    id = "ms:$mediaId",
                    title = tags?.title
                        ?: TagText.clean(cursor.getString(titleColumn))
                        ?: TagText.withoutExtension(fileName)
                        ?: "未知曲目",
                    artist = artist ?: "未知艺术家",
                    album = album ?: "未知专辑",
                    albumId = cursor.getLong(albumIdColumn),
                    durationMs = durationMs,
                    trackNumber = tags?.trackNumber?.takeIf { it > 0 }
                        ?: cursor.getInt(trackColumn),
                    year = tags?.year?.takeIf { it > 0 } ?: cursor.getInt(yearColumn),
                    uri = uri,
                )
            }
        }

        return buildLibrary(tracks)
    }

    /** Groups raw tracks into albums / artists for the browsing screens. */
    private fun buildLibrary(tracks: List<Track>): Library {
        val albums = tracks.groupBy { it.albumId }
            .map { (albumId, albumTracks) ->
                Album(
                    id = albumId,
                    title = albumTracks.first().album,
                    artist = albumTracks.groupingBy { it.artist }.eachCount()
                        .maxByOrNull { it.value }?.key ?: "未知艺术家",
                    trackCount = albumTracks.size,
                    coverTrack = albumTracks.firstOrNull(),
                )
            }
            .sortedBy { it.title.lowercase() }

        val artists = tracks.groupBy { it.artist }
            .map { (name, artistTracks) ->
                Artist(
                    name = name,
                    trackCount = artistTracks.size,
                    albumCount = artistTracks.map { it.albumId }.distinct().size,
                    coverTrack = artistTracks.firstOrNull(),
                )
            }
            .sortedBy { it.name.lowercase() }

        return Library(tracks = tracks, albums = albums, artists = artists)
    }

    /**
     * Walks a SAF folder tree and keeps only audio documents. No MediaStore query happens
     * in this mode, so music outside the chosen folder is never touched.
     */
    private fun queryFolder(treeUri: Uri): Library {
        val resolver = context.contentResolver
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val tracks = mutableListOf<Track>()
        val pending = ArrayDeque<String>()
        pending += rootId
        var visited = 0
        var reads = 0

        while (pending.isNotEmpty() && visited < MAX_DOCUMENTS) {
            val parentId = pending.removeFirst()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            val cursor = try {
                resolver.query(children, FOLDER_PROJECTION, null, null, null)
            } catch (_: IllegalArgumentException) {
                // Some providers reject the audio columns; retry with the document columns only.
                resolver.query(children, FOLDER_FALLBACK_PROJECTION, null, null, null)
            }
            cursor?.use { c ->
                val idIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val mimeIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val nameIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val titleIndex = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistIndex = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumIndex = c.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val durationIndex = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val trackIndex = c.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val yearIndex = c.getColumnIndex(MediaStore.Audio.Media.YEAR)

                while (c.moveToNext() && visited < MAX_DOCUMENTS) {
                    visited++
                    val documentId = c.getString(idIndex) ?: continue
                    val mimeType = c.getString(mimeIndex) ?: continue
                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        pending += documentId
                        continue
                    }
                    if (!mimeType.startsWith("audio/")) continue

                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                    val tags = if (reads < MAX_METADATA_READS) {
                        reads++
                        EmbeddedMetadataReader.read(context, documentUri)
                    } else {
                        null
                    }
                    val listedDuration = c.stringAt(durationIndex)?.toLongOrNull() ?: 0L
                    val durationMs = tags?.durationMs?.takeIf { it > 0L } ?: listedDuration
                    if (durationMs in 1 until MIN_DURATION_MS) continue

                    val fallbackTitle = TagText.withoutExtension(c.getString(nameIndex))
                    val album = tags?.album
                        ?: TagText.clean(c.stringAt(albumIndex))
                        ?: "未知专辑"
                    tracks += Track(
                        id = "doc:$documentId",
                        title = tags?.title
                            ?: TagText.clean(c.stringAt(titleIndex))
                            ?: fallbackTitle
                            ?: "未知曲目",
                        artist = tags?.artist
                            ?: TagText.clean(c.stringAt(artistIndex))
                            ?: "未知艺术家",
                        album = album,
                        albumId = -(album.hashCode().toLong() and 0x7fffffffL) - 1L,
                        durationMs = durationMs,
                        trackNumber = tags?.trackNumber?.takeIf { it > 0 }
                            ?: (c.stringAt(trackIndex)?.toIntOrNull() ?: 0),
                        year = tags?.year?.takeIf { it > 0 }
                            ?: (c.stringAt(yearIndex)?.toIntOrNull() ?: 0),
                        uri = documentUri,
                    )
                }
            }
        }

        return buildLibrary(tracks.sortedBy { it.title.lowercase() })
    }

    private companion object {
        const val MIN_DURATION_MS = 15_000L
        const val MAX_DOCUMENTS = 5_000
        const val MAX_METADATA_READS = 2_000

        val FOLDER_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
        )

        val FOLDER_FALLBACK_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        )
    }
}

/** Column value that is simply absent (or null) for a given provider/document. */
private fun Cursor.stringAt(index: Int): String? =
    if (index >= 0 && !isNull(index)) getString(index) else null

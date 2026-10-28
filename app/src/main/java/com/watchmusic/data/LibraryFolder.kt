package com.watchmusic.data

import android.net.Uri
import android.provider.DocumentsContract

/** Helpers for the optional "music only from this folder" restriction (SAF tree uris). */
object LibraryFolder {

    /**
     * Best-effort human readable name for a tree uri, e.g.
     * "content://…/tree/primary%3AMusic%2FRock" becomes "Rock".
     */
    fun displayName(treeUri: Uri): String {
        val treeId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }
            .getOrNull()
            .orEmpty()
        return displayNameFromTreeId(treeId)
    }

    fun displayNameFromTreeId(treeId: String): String {
        val path = treeId.substringAfter(':', missingDelimiterValue = treeId)
        val name = path.trimEnd('/').substringAfterLast('/')
        return name.ifBlank { DEFAULT_NAME }
    }

    const val DEFAULT_NAME = "所选文件夹"
}

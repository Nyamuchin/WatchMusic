package com.watchmusic.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryFolderTest {

    @Test
    fun leafFolderNameIsUsed() {
        assertEquals("Rock", LibraryFolder.displayNameFromTreeId("primary:Music/Library/Rock"))
    }

    @Test
    fun topLevelFolderKeepsItsOwnName() {
        assertEquals("Music", LibraryFolder.displayNameFromTreeId("primary:Music"))
    }

    @Test
    fun trailingSlashIsIgnored() {
        assertEquals("Jazz", LibraryFolder.displayNameFromTreeId("primary:Music/Jazz/"))
    }

    @Test
    fun storageRootFallsBackToDefaultName() {
        assertEquals(LibraryFolder.DEFAULT_NAME, LibraryFolder.displayNameFromTreeId("primary:"))
    }

    @Test
    fun emptyIdFallsBackToDefaultName() {
        assertEquals(LibraryFolder.DEFAULT_NAME, LibraryFolder.displayNameFromTreeId(""))
    }
}

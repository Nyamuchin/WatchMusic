package com.watchmusic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.watchmusic.R
import com.watchmusic.data.model.Album
import com.watchmusic.ui.components.AlbumTile
import com.watchmusic.ui.components.EmptyState
import com.watchmusic.ui.components.WatchScreen

@Composable
fun AlbumListScreen(
    albums: List<Album>,
    onBack: () -> Unit,
    onAlbumClick: (Album) -> Unit,
) {
    WatchScreen(
        title = stringResource(R.string.albums),
        subtitle = stringResource(R.string.albums_count, albums.size),
        onBack = onBack,
    ) {
        if (albums.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = stringResource(R.string.empty_library),
                    subtitle = stringResource(R.string.empty_library_subtitle),
                    icon = Icons.Rounded.Album,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(albums, key = { it.id }) { album ->
                    AlbumTile(album = album, onClick = { onAlbumClick(album) })
                }
            }
        }
    }
}

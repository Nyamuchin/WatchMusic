package com.watchmusic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.watchmusic.R
import com.watchmusic.data.model.Artist
import com.watchmusic.ui.components.ArtistTile
import com.watchmusic.ui.components.EmptyState
import com.watchmusic.ui.components.WatchScreen

@Composable
fun ArtistListScreen(
    artists: List<Artist>,
    onBack: () -> Unit,
    onArtistClick: (Artist) -> Unit,
) {
    WatchScreen(
        title = stringResource(R.string.artists),
        subtitle = stringResource(R.string.artists_count, artists.size),
        onBack = onBack,
    ) {
        if (artists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = stringResource(R.string.empty_library),
                    subtitle = stringResource(R.string.empty_library_subtitle),
                    icon = Icons.Rounded.Person,
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
                items(artists, key = { it.name }) { artist ->
                    ArtistTile(artist = artist, onClick = { onArtistClick(artist) })
                }
            }
        }
    }
}

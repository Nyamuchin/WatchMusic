package com.watchmusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.watchmusic.R
import com.watchmusic.data.SearchFilter
import com.watchmusic.data.model.Album
import com.watchmusic.data.model.Artist
import com.watchmusic.data.model.Library
import com.watchmusic.data.model.Track
import com.watchmusic.ui.components.AlbumTile
import com.watchmusic.ui.components.ArtistTile
import com.watchmusic.ui.components.EmptyState
import com.watchmusic.ui.components.SectionLabel
import com.watchmusic.ui.components.TrackRow
import com.watchmusic.ui.components.WatchScreen

/** Local search across songs, albums and artists of the scanned library. */
@Composable
fun SearchScreen(
    library: Library,
    currentTrackId: String?,
    isPlaying: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onBack: () -> Unit,
    onTrackClick: (List<Track>, Int) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val needle = query.trim()

    val tracks = remember(library, needle) { SearchFilter.tracks(library, needle) }
    val albums = remember(library, needle) { SearchFilter.albums(library, needle) }
    val artists = remember(library, needle) { SearchFilter.artists(library, needle) }

    WatchScreen(title = stringResource(R.string.search), onBack = onBack) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 3.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .height(36.dp)
                    .padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { query = "" },
                    )
                }
            }

            if (needle.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.search_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            } else if (tracks.isEmpty() && albums.isEmpty() && artists.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.search_empty),
                    icon = Icons.Rounded.Search,
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (tracks.isNotEmpty()) {
                        item {
                            SectionLabel(
                                text = stringResource(R.string.section_songs),
                                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 2.dp),
                            )
                        }
                        items(tracks, key = { it.id }) { track ->
                            TrackRow(
                                track = track,
                                isCurrent = track.id == currentTrackId,
                                isPlaying = isPlaying,
                                accent = accent,
                                onClick = { onTrackClick(tracks, tracks.indexOf(track)) },
                                onLongClick = {},
                            )
                        }
                    }
                    if (albums.isNotEmpty()) {
                        item {
                            SectionLabel(
                                text = stringResource(R.string.section_albums),
                                modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 2.dp),
                            )
                        }
                        items(albums, key = { it.id }) { album ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 3.dp),
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    AlbumTile(album = album, onClick = { onAlbumClick(album) })
                                }
                                Spacer(Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {}
                            }
                        }
                    }
                    if (artists.isNotEmpty()) {
                        item {
                            SectionLabel(
                                text = stringResource(R.string.section_artists),
                                modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 2.dp),
                            )
                        }
                        items(artists, key = { it.name }) { artist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 3.dp),
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ArtistTile(artist = artist, onClick = { onArtistClick(artist) })
                                }
                                Spacer(Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {}
                            }
                        }
                    }
                }
            }
        }
    }
}

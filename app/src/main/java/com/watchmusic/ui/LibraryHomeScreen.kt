package com.watchmusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.watchmusic.R
import com.watchmusic.ui.components.CircleIconButton
import com.watchmusic.ui.components.EmptyState
import com.watchmusic.ui.components.LibraryRow

/** Second pager page: a compact hub of every library section. */
@Composable
fun LibraryHomeScreen(
    tracksCount: Int,
    albumsCount: Int,
    artistsCount: Int,
    playlistsCount: Int,
    favoritesCount: Int,
    recentCount: Int,
    loading: Boolean,
    error: String?,
    hasTracks: Boolean,
    onOpenSongs: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onRescan: () -> Unit,
    uiScale: Float = 1f,
) {
    // Keep the search/settings entry at a tappable physical size when the UI is zoomed out,
    // so the scale can always be changed back from the settings page.
    val chrome = if (uiScale in 0.1f..0.999f) 1f / uiScale else 1f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                // The centred pair only fits on a wide (i.e. zoomed-out) screen; on a narrow
                // screen the title and the buttons share a row, and at extreme zoom they stack.
                when {
                    maxWidth < 120.dp -> Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.library),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        HeaderActions(
                            onOpenSearch = onOpenSearch,
                            onOpenSettings = onOpenSettings,
                            size = 26.dp * chrome,
                            iconSize = 15.dp * chrome,
                            gap = 10.dp * chrome,
                        )
                    }

                    maxWidth < 172.dp -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.library),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        HeaderActions(
                            onOpenSearch = onOpenSearch,
                            onOpenSettings = onOpenSettings,
                            size = 24.dp * chrome,
                            iconSize = 14.dp * chrome,
                            gap = 6.dp * chrome,
                        )
                    }

                    else -> {
                        Text(
                            text = stringResource(R.string.library),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(y = 3.dp)
                                .padding(end = 104.dp * chrome),
                        )
                        Box(modifier = Modifier.align(Alignment.Center)) {
                            HeaderActions(
                                onOpenSearch = onOpenSearch,
                                onOpenSettings = onOpenSettings,
                                size = 28.dp * chrome,
                                iconSize = 16.dp * chrome,
                                gap = 20.dp * chrome,
                            )
                        }
                    }
                }
            }

            if (!loading && (!hasTracks || error != null)) {
                Spacer(Modifier.height(10.dp))
                EmptyState(
                    title = error ?: stringResource(R.string.empty_library),
                    subtitle = stringResource(R.string.empty_library_subtitle),
                    icon = Icons.Rounded.LibraryMusic,
                    action = {
                        Button(
                            onClick = onRescan,
                            modifier = Modifier.height(32.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.rescan),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                )
                Spacer(Modifier.height(12.dp))
            }

            LibraryRow(
                icon = Icons.Rounded.LibraryMusic,
                title = stringResource(R.string.all_songs),
                subtitle = stringResource(R.string.songs_count, tracksCount),
                onClick = onOpenSongs,
            )
            LibraryRow(
                icon = Icons.Rounded.Album,
                title = stringResource(R.string.albums),
                subtitle = stringResource(R.string.albums_count, albumsCount),
                onClick = onOpenAlbums,
            )
            LibraryRow(
                icon = Icons.Rounded.Person,
                title = stringResource(R.string.artists),
                subtitle = stringResource(R.string.artists_count, artistsCount),
                onClick = onOpenArtists,
            )
            LibraryRow(
                icon = Icons.Rounded.PlaylistPlay,
                title = stringResource(R.string.playlists),
                subtitle = stringResource(R.string.playlists_count, playlistsCount),
                onClick = onOpenPlaylists,
            )
            LibraryRow(
                icon = Icons.Rounded.Favorite,
                title = stringResource(R.string.favorites),
                subtitle = stringResource(R.string.songs_count, favoritesCount),
                onClick = onOpenFavorites,
            )
            LibraryRow(
                icon = Icons.Rounded.History,
                title = stringResource(R.string.recent),
                subtitle = stringResource(R.string.songs_count, recentCount),
                onClick = onOpenRecent,
            )

            if (loading) {
                Text(
                    text = "正在扫描曲库…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun HeaderActions(
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    size: Dp,
    iconSize: Dp,
    gap: Dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(
            icon = Icons.Rounded.Search,
            contentDescription = stringResource(R.string.search),
            onClick = onOpenSearch,
            size = size,
            iconSize = iconSize,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
        CircleIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = stringResource(R.string.settings),
            onClick = onOpenSettings,
            size = size,
            iconSize = iconSize,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    }
}

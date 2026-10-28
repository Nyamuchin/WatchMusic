package com.watchmusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.watchmusic.R
import com.watchmusic.player.RepeatMode
import com.watchmusic.ui.components.CircleIconButton
import com.watchmusic.ui.components.DensityScale
import com.watchmusic.ui.components.HideSystemBarsInDialog
import com.watchmusic.ui.components.SheetRow
import com.watchmusic.util.TimeFormat
import kotlin.math.roundToInt

/** Shuffle, repeat, favourite, sleep timer and volume in one sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackOptionsSheet(
    shuffle: Boolean,
    repeat: RepeatMode,
    favorite: Boolean,
    sleepRemainingMs: Long,
    volumePercent: Int,
    onDismiss: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSleepMinutes: (Int) -> Unit,
    onCancelSleep: () -> Unit,
    onSetVolume: (Int) -> Unit,
    uiScale: Float = 1f,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        HideSystemBarsInDialog()
        DensityScale(uiScale) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 18.dp),
        ) {
            Text(
                text = stringResource(R.string.playback_options),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
            )

            SheetRow(
                title = stringResource(R.string.shuffle),
                subtitle = null,
                onClick = onToggleShuffle,
                leading = { SheetIcon(Icons.Rounded.Shuffle) },
                trailing = { Switch(checked = shuffle, onCheckedChange = null) },
            )
            SheetRow(
                title = stringResource(R.string.repeat),
                subtitle = stringResource(repeat.label()),
                onClick = onCycleRepeat,
                leading = {
                    SheetIcon(if (repeat == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat)
                },
            )
            SheetRow(
                title = stringResource(if (favorite) R.string.unfavorite else R.string.favorite),
                subtitle = null,
                onClick = onToggleFavorite,
                leading = {
                    SheetIcon(if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder)
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.sleep_timer),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (sleepRemainingMs > 0L) {
                    Text(
                        text = TimeFormat.countdown(sleepRemainingMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            val choices = listOf(0, 15, 30, 45, 60)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                choices.forEach { minutes ->
                    SleepChip(
                        label = if (minutes == 0) {
                            stringResource(R.string.sleep_off)
                        } else {
                            "$minutes"
                        },
                        selected = if (minutes == 0) sleepRemainingMs == 0L else false,
                        onClick = {
                            if (minutes == 0) onCancelSleep() else onSleepMinutes(minutes)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(
                    icon = Icons.Rounded.Remove,
                    contentDescription = null,
                    onClick = { onSetVolume((volumePercent - 10).coerceIn(0, 100)) },
                    size = 24.dp,
                    iconSize = 14.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((volumePercent / 100f).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Spacer(Modifier.width(8.dp))
                CircleIconButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = null,
                    onClick = { onSetVolume((volumePercent + 10).coerceIn(0, 100)) },
                    size = 24.dp,
                    iconSize = 14.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "$volumePercent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        }
    }
}

@Composable
private fun SheetIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(17.dp),
    )
}

@Composable
private fun SleepChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .height(28.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = fg)
    }
}

private fun RepeatMode.label(): Int = when (this) {
    RepeatMode.OFF -> R.string.repeat_off
    RepeatMode.ALL -> R.string.repeat_all
    RepeatMode.ONE -> R.string.repeat_one
}

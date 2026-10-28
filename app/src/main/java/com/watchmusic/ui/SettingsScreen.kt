package com.watchmusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.watchmusic.R
import com.watchmusic.ui.components.SectionLabel
import com.watchmusic.ui.components.SheetRow
import com.watchmusic.ui.components.WatchScreen

@Composable
fun SettingsScreen(
    useArtworkColor: Boolean,
    haptic: Boolean,
    crownVolume: Boolean,
    uiScale: Float,
    notificationGranted: Boolean,
    version: String,
    onBack: () -> Unit,
    onUseArtworkColor: (Boolean) -> Unit,
    onHaptic: (Boolean) -> Unit,
    onCrownVolume: (Boolean) -> Unit,
    onUiScale: (Float) -> Unit,
    libraryFolder: String?,
    onPickFolder: () -> Unit,
    onClearFolder: () -> Unit,
    onRequestNotification: () -> Unit,
    onRescan: () -> Unit,
) {
    WatchScreen(title = stringResource(R.string.settings), onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
        ) {
            SectionLabel(
                text = stringResource(R.string.colors_section),
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 2.dp),
            )
            ToggleRow(
                title = stringResource(R.string.artwork_color),
                subtitle = stringResource(R.string.artwork_color_desc),
                checked = useArtworkColor,
                onChange = onUseArtworkColor,
            )

            Text(
                text = stringResource(R.string.ui_scale),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp),
            )
            ScaleRow(
                values = listOf(0.25f, 0.5f, 0.75f, 1f),
                current = uiScale,
                onSelect = onUiScale,
            )
            Spacer(Modifier.height(6.dp))
            ScaleRow(
                values = listOf(1.25f, 1.5f, 1.75f, 2f),
                current = uiScale,
                onSelect = onUiScale,
            )

            SectionLabel(
                text = stringResource(R.string.playback_section),
                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 2.dp),
            )
            ToggleRow(
                title = stringResource(R.string.crown_volume),
                subtitle = stringResource(R.string.crown_volume_desc),
                checked = crownVolume,
                onChange = onCrownVolume,
            )
            ToggleRow(
                title = stringResource(R.string.haptics),
                subtitle = stringResource(R.string.haptics_desc),
                checked = haptic,
                onChange = onHaptic,
            )

            SectionLabel(
                text = stringResource(R.string.library_section),
                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 2.dp),
            )
            SheetRow(
                title = stringResource(R.string.library_folder),
                subtitle = if (libraryFolder == null) {
                    stringResource(R.string.library_folder_all)
                } else {
                    stringResource(R.string.library_folder_picked, libraryFolder)
                },
                onClick = onPickFolder,
                subtitleMaxLines = 2,
            )
            if (libraryFolder != null) {
                SheetRow(
                    title = stringResource(R.string.library_folder_clear),
                    subtitle = null,
                    onClick = onClearFolder,
                )
            }
            SheetRow(
                title = stringResource(R.string.rescan),
                subtitle = null,
                onClick = onRescan,
            )

            SectionLabel(
                text = stringResource(R.string.permission_section),
                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 2.dp),
            )
            SheetRow(
                title = stringResource(R.string.notification_permission),
                subtitle = stringResource(
                    if (notificationGranted) R.string.notification_granted else R.string.notification_denied,
                ),
                onClick = onRequestNotification,
            )

            SectionLabel(
                text = stringResource(R.string.about),
                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 2.dp),
            )
            SheetRow(
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.version, version),
                onClick = {},
            )
            Text(
                text = stringResource(R.string.about_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp),
            )
        }
    }
}

@Composable
private fun ScaleRow(
    values: List<Float>,
    current: Float,
    onSelect: (Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        values.forEach { value ->
            ScaleChip(
                label = "${(value * 100).roundToInt()}%",
                value = value,
                current = current,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun RowScope.ScaleChip(
    label: String,
    value: Float,
    current: Float,
    onSelect: (Float) -> Unit,
) {
    val selected = kotlin.math.abs(current - value) < 0.01f
    Box(
        modifier = Modifier
            .weight(1f)
            .height(30.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            .clickable { onSelect(value) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SheetRow(
                title = title,
                subtitle = subtitle,
                onClick = { onChange(!checked) },
                subtitleMaxLines = 2,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.padding(end = 12.dp),
        )
    }
}

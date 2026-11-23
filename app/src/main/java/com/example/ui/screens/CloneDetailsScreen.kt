package com.example.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.NameValidationResult
import com.example.domain.model.CloneItem
import com.example.ui.components.BadgedCloneIcon
import com.example.ui.components.CloneConfigDialog
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CloneDetailsScreen(
    clone: CloneItem?,
    isBengali: Boolean,
    showBackButton: Boolean,
    getAppIcon: (String) -> Drawable?,
    loadGrantedPermissions: suspend (String) -> List<String>,
    validateName: (String, String?) -> NameValidationResult,
    onRequestNotificationPermission: ((Boolean) -> Unit) -> Unit,
    onBack: () -> Unit,
    onOpenClone: (CloneItem) -> Unit,
    onStopClone: (String) -> Unit,
    onUpdateClone: (CloneItem) -> Unit,
    onPinShortcut: (CloneItem) -> Unit,
    onClearCache: (CloneItem) -> Unit,
    onClearData: (CloneItem) -> Unit,
    onExportConfig: (CloneItem) -> Unit,
    onDeleteClone: (CloneItem) -> Unit
) {
    var permissions by remember(clone?.originalPackageName) { mutableStateOf<List<String>>(emptyList()) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCustomizeDialog by remember { mutableStateOf(false) }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(clone?.originalPackageName) {
        if (clone != null) {
            permissions = loadGrantedPermissions(clone.originalPackageName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.details_title)) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("details_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (clone == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.details_not_found),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        val totalBytes = (clone.storageBytes + clone.cacheBytes).coerceAtLeast(1L)
        val dataRatio = clone.storageBytes.toFloat() / totalBytes.toFloat()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Card
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BadgedCloneIcon(
                            drawable = getAppIcon(clone.originalPackageName),
                            badgeLabel = clone.badgeLabel,
                            badgeColorHex = clone.badgeColorHex,
                            badgeStyle = clone.badgeStyle,
                            contentDescription = clone.cloneName,
                            size = 64.dp,
                            isLocked = clone.isLocked
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = clone.cloneName,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = clone.originalAppName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            StatusPill(isRunning = clone.isRunning)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onOpenClone(clone) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("details_open_btn")
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_open))
                        }

                        if (clone.isRunning) {
                            FilledTonalButton(
                                onClick = { onStopClone(clone.id) },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Rounded.StopCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.action_stop))
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onPinShortcut(clone) },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.action_pin_shortcut))
                            }
                        }
                    }
                }
            }

            // Metadata Card
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetadataRow(
                        label = stringResource(R.string.details_package),
                        value = clone.originalPackageName
                    )
                    MetadataRow(
                        label = stringResource(R.string.details_clone_id),
                        value = clone.id
                    )
                    MetadataRow(
                        label = stringResource(R.string.details_data_dir),
                        value = clone.dataDirPath
                    )
                    MetadataRow(
                        label = stringResource(R.string.details_created_at),
                        value = Formatters.formatDate(clone.createdAt, isBengali)
                    )
                    MetadataRow(
                        label = stringResource(R.string.details_last_used),
                        value = clone.lastUsedAt?.let { Formatters.formatDate(it, isBengali) }
                            ?: stringResource(R.string.last_used_never)
                    )
                }
            }

            // Storage Breakdown Card
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.details_storage_total),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = Formatters.formatBytes(clone.storageBytes + clone.cacheBytes),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { dataRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${stringResource(R.string.details_storage_data)}: ${Formatters.formatBytes(clone.storageBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${stringResource(R.string.details_storage_cache)}: ${Formatters.formatBytes(clone.cacheBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Toggles Card
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.details_controls_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    DetailSwitchRow(
                        icon = Icons.Rounded.Notifications,
                        title = stringResource(R.string.clone_option_notifications),
                        subtitle = stringResource(R.string.clone_option_notifications_desc),
                        checked = clone.notificationsEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                onRequestNotificationPermission { granted ->
                                    onUpdateClone(clone.copy(notificationsEnabled = granted))
                                }
                            } else {
                                onUpdateClone(clone.copy(notificationsEnabled = false))
                            }
                        }
                    )
                    HorizontalDivider()
                    DetailSwitchRow(
                        icon = Icons.Rounded.Lock,
                        title = stringResource(R.string.clone_option_lock),
                        subtitle = stringResource(R.string.clone_option_lock_desc),
                        checked = clone.isLocked,
                        onCheckedChange = { locked ->
                            onUpdateClone(clone.copy(isLocked = locked))
                        }
                    )
                    HorizontalDivider()
                    DetailSwitchRow(
                        icon = Icons.Rounded.VisibilityOff,
                        title = stringResource(R.string.clone_option_hide_launcher),
                        subtitle = stringResource(R.string.clone_option_hide_launcher_desc),
                        checked = clone.isHiddenFromLauncher,
                        onCheckedChange = { hidden ->
                            onUpdateClone(clone.copy(isHiddenFromLauncher = hidden))
                        }
                    )
                }
            }

            // Permissions Card
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.details_permissions_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (permissions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.details_permissions_none),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            permissions.forEach { perm ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(perm, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }

            // Maintenance Actions Card
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.details_danger_zone),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showRenameDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.DriveFileRenameOutline, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_rename))
                        }
                        OutlinedButton(
                            onClick = { showCustomizeDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Palette, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_customize_badge))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onClearCache(clone) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.CleaningServices, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_clear_cache))
                        }
                        OutlinedButton(
                            onClick = { onExportConfig(clone) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.FileUpload, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_export_config))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showClearDataConfirm = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_clear_data))
                        }
                        Button(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Delete, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_delete))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showRenameDialog && clone != null) {
        RenameCloneDialog(
            clone = clone,
            validateName = { validateName(it, clone.id) },
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                onUpdateClone(clone.copy(cloneName = newName))
                showRenameDialog = false
            }
        )
    }

    if (showCustomizeDialog && clone != null) {
        CloneConfigDialog(
            title = stringResource(R.string.clone_edit_appearance_title),
            confirmLabel = stringResource(R.string.clone_save_button),
            appDrawable = getAppIcon(clone.originalPackageName),
            initialName = clone.cloneName,
            initialBadgeLabel = clone.badgeLabel,
            initialBadgeColorHex = clone.badgeColorHex,
            badgeStyle = clone.badgeStyle,
            initialNotifications = clone.notificationsEnabled,
            initialHidden = clone.isHiddenFromLauncher,
            initialLocked = clone.isLocked,
            showToggles = false,
            validateName = { validateName(it, clone.id) },
            onRequestNotificationPermission = onRequestNotificationPermission,
            onDismiss = { showCustomizeDialog = false },
            onConfirm = { name, badgeLabel, badgeColorHex, _, _, _ ->
                onUpdateClone(
                    clone.copy(
                        cloneName = name,
                        badgeLabel = badgeLabel,
                        badgeColorHex = badgeColorHex
                    )
                )
                showCustomizeDialog = false
            }
        )
    }

    if (showClearDataConfirm && clone != null) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text(stringResource(R.string.clear_data_confirm_title)) },
            text = { Text(stringResource(R.string.clear_data_confirm_body, clone.cloneName)) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearData(clone)
                        showClearDataConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.confirm_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) {
                    Text(stringResource(R.string.clone_cancel_button))
                }
            }
        )
    }

    if (showDeleteConfirm && clone != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_body, clone.cloneName)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClone(clone)
                        showDeleteConfirm = false
                        if (showBackButton) onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.clone_cancel_button))
                }
            }
        )
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DetailSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

package com.example.ui.screens

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ControlPointDuplicate
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.NameValidationResult
import com.example.domain.model.CloneItem
import com.example.ui.components.BadgedCloneIcon
import com.example.ui.components.CloneConfigDialog
import com.example.ui.theme.StatusRunningColor
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyClonesScreen(
    clones: List<CloneItem>,
    isBengali: Boolean,
    selectedCloneId: String?,
    getAppIcon: (String) -> Drawable?,
    validateName: (String, String?) -> NameValidationResult,
    onRequestNotificationPermission: ((Boolean) -> Unit) -> Unit,
    onOpenClone: (CloneItem) -> Unit,
    onStopClone: (String) -> Unit,
    onStopClones: (List<String>) -> Unit,
    onUpdateClone: (CloneItem) -> Unit,
    onPinShortcut: (CloneItem) -> Unit,
    onClearCache: (CloneItem) -> Unit,
    onClearData: (CloneItem) -> Unit,
    onExportConfig: (CloneItem) -> Unit,
    onDeleteClone: (CloneItem) -> Unit,
    onDeleteClones: (List<CloneItem>) -> Unit,
    onSelectDetails: (CloneItem) -> Unit,
    onBrowseApps: () -> Unit
) {
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var renameTarget by remember { mutableStateOf<CloneItem?>(null) }
    var customizeTarget by remember { mutableStateOf<CloneItem?>(null) }
    var clearDataTarget by remember { mutableStateOf<CloneItem?>(null) }
    var deleteTarget by remember { mutableStateOf<CloneItem?>(null) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedIds = emptySet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectionMode) {
                        Text(stringResource(R.string.selected_count, selectedIds.size))
                    } else {
                        Text(stringResource(R.string.clones_title))
                    }
                },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = {
                            selectionMode = false
                            selectedIds = emptySet()
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.batch_clear_selection)
                            )
                        }
                    }
                },
                actions = {
                    if (clones.isNotEmpty()) {
                        if (selectionMode) {
                            IconButton(
                                onClick = {
                                    selectedIds = if (selectedIds.size == clones.size) {
                                        emptySet()
                                    } else {
                                        clones.map { it.id }.toSet()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SelectAll,
                                    contentDescription = stringResource(R.string.batch_select_all)
                                )
                            }
                            IconButton(
                                onClick = {
                                    onStopClones(selectedIds.toList())
                                    selectionMode = false
                                    selectedIds = emptySet()
                                },
                                enabled = selectedIds.isNotEmpty(),
                                modifier = Modifier.testTag("batch_stop_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.StopCircle,
                                    contentDescription = stringResource(R.string.batch_stop)
                                )
                            }
                            IconButton(
                                onClick = { showBatchDeleteConfirm = true },
                                enabled = selectedIds.isNotEmpty(),
                                modifier = Modifier.testTag("batch_delete_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.batch_delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { selectionMode = true },
                                modifier = Modifier.testTag("clones_multiselect_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Checklist,
                                    contentDescription = stringResource(R.string.batch_select_all)
                                )
                            }
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
        if (clones.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding)) {
                EmptyOrErrorState(
                    icon = Icons.Rounded.ControlPointDuplicate,
                    title = stringResource(R.string.clones_empty_title),
                    description = stringResource(R.string.clones_empty_desc),
                    actionLabel = stringResource(R.string.clones_go_to_apps),
                    onAction = onBrowseApps
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(clones, key = { it.id }) { clone ->
                    CloneCard(
                        clone = clone,
                        isBengali = isBengali,
                        isSelected = clone.id in selectedIds,
                        isDetailActive = clone.id == selectedCloneId,
                        selectionMode = selectionMode,
                        drawable = getAppIcon(clone.originalPackageName),
                        onClick = {
                            if (selectionMode) {
                                selectedIds = if (clone.id in selectedIds) {
                                    selectedIds - clone.id
                                } else {
                                    selectedIds + clone.id
                                }
                            } else {
                                onSelectDetails(clone)
                            }
                        },
                        onLongClick = {
                            if (!selectionMode) {
                                selectionMode = true
                                selectedIds = setOf(clone.id)
                            }
                        },
                        onOpen = { onOpenClone(clone) },
                        onStop = { onStopClone(clone.id) },
                        onDetails = { onSelectDetails(clone) },
                        onRename = { renameTarget = clone },
                        onCustomize = { customizeTarget = clone },
                        onPinShortcut = { onPinShortcut(clone) },
                        onClearCache = { onClearCache(clone) },
                        onClearData = { clearDataTarget = clone },
                        onExportConfig = { onExportConfig(clone) },
                        onDelete = { deleteTarget = clone }
                    )
                }
            }
        }
    }

    // Rename Dialog
    renameTarget?.let { target ->
        RenameCloneDialog(
            clone = target,
            validateName = { validateName(it, target.id) },
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                onUpdateClone(target.copy(cloneName = newName))
                renameTarget = null
            }
        )
    }

    // Customize Badge / Appearance Dialog
    customizeTarget?.let { target ->
        CloneConfigDialog(
            title = stringResource(R.string.clone_edit_appearance_title),
            confirmLabel = stringResource(R.string.clone_save_button),
            appDrawable = getAppIcon(target.originalPackageName),
            initialName = target.cloneName,
            initialBadgeLabel = target.badgeLabel,
            initialBadgeColorHex = target.badgeColorHex,
            badgeStyle = target.badgeStyle,
            initialNotifications = target.notificationsEnabled,
            initialHidden = target.isHiddenFromLauncher,
            initialLocked = target.isLocked,
            showToggles = false,
            validateName = { validateName(it, target.id) },
            onRequestNotificationPermission = onRequestNotificationPermission,
            onDismiss = { customizeTarget = null },
            onConfirm = { name, badgeLabel, badgeColorHex, _, _, _ ->
                onUpdateClone(
                    target.copy(
                        cloneName = name,
                        badgeLabel = badgeLabel,
                        badgeColorHex = badgeColorHex
                    )
                )
                customizeTarget = null
            }
        )
    }

    // Clear Data Confirmation
    clearDataTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { clearDataTarget = null },
            title = { Text(stringResource(R.string.clear_data_confirm_title)) },
            text = { Text(stringResource(R.string.clear_data_confirm_body, target.cloneName)) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearData(target)
                        clearDataTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.confirm_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { clearDataTarget = null }) {
                    Text(stringResource(R.string.clone_cancel_button))
                }
            }
        )
    }

    // Single Delete Confirmation
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_body, target.cloneName)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClone(target)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_clone_btn")
                ) {
                    Text(stringResource(R.string.confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.clone_cancel_button))
                }
            }
        )
    }

    // Batch Delete Confirmation
    if (showBatchDeleteConfirm) {
        val toDelete = clones.filter { it.id in selectedIds }
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text(stringResource(R.string.batch_delete_confirm_title, toDelete.size)) },
            text = { Text(stringResource(R.string.batch_delete_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClones(toDelete)
                        showBatchDeleteConfirm = false
                        selectionMode = false
                        selectedIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text(stringResource(R.string.clone_cancel_button))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CloneCard(
    clone: CloneItem,
    isBengali: Boolean,
    isSelected: Boolean,
    isDetailActive: Boolean,
    selectionMode: Boolean,
    drawable: Drawable?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOpen: () -> Unit,
    onStop: () -> Unit,
    onDetails: () -> Unit,
    onRename: () -> Unit,
    onCustomize: () -> Unit,
    onPinShortcut: () -> Unit,
    onClearCache: () -> Unit,
    onClearData: () -> Unit,
    onExportConfig: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val totalBytes = clone.storageBytes + clone.cacheBytes
    val lastUsedText = clone.lastUsedAt?.let {
        stringResource(R.string.last_used_format, Formatters.formatRelativeTime(it, isBengali))
    } ?: stringResource(R.string.last_used_never)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (isDetailActive || isSelected) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium
                    )
                } else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("clone_card_${clone.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(visible = selectionMode) {
                    Row {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onClick() }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }

                BadgedCloneIcon(
                    drawable = drawable,
                    badgeLabel = clone.badgeLabel,
                    badgeColorHex = clone.badgeColorHex,
                    badgeStyle = clone.badgeStyle,
                    contentDescription = clone.cloneName,
                    size = 52.dp,
                    isLocked = clone.isLocked
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = clone.cloneName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusPill(isRunning = clone.isRunning)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = clone.originalAppName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$lastUsedText  •  ${stringResource(R.string.storage_size_format, Formatters.formatBytes(totalBytes))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!selectionMode) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onOpen,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("open_clone_btn_${clone.id}"),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_open))
                    }

                    if (clone.isRunning) {
                        FilledTonalButton(
                            onClick = onStop,
                            modifier = Modifier.height(42.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.StopCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.status_stopped))
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDetails,
                            modifier = Modifier.height(42.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(stringResource(R.string.action_details))
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("clone_more_btn_${clone.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.action_more_options)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.Info, null) },
                                text = { Text(stringResource(R.string.action_details)) },
                                onClick = { menuExpanded = false; onDetails() }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, null) },
                                text = { Text(stringResource(R.string.action_rename)) },
                                onClick = { menuExpanded = false; onRename() }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.Palette, null) },
                                text = { Text(stringResource(R.string.action_customize_badge)) },
                                onClick = { menuExpanded = false; onCustomize() }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.AddToHomeScreen, null) },
                                text = { Text(stringResource(R.string.action_pin_shortcut)) },
                                onClick = { menuExpanded = false; onPinShortcut() }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.CleaningServices, null) },
                                text = { Text(stringResource(R.string.action_clear_cache)) },
                                onClick = { menuExpanded = false; onClearCache() }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.DeleteSweep, null) },
                                text = { Text(stringResource(R.string.action_clear_data)) },
                                onClick = { menuExpanded = false; onClearData() }
                            )
                            if (clone.isRunning) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Rounded.StopCircle, null) },
                                    text = { Text(stringResource(R.string.action_stop)) },
                                    onClick = { menuExpanded = false; onStop() }
                                )
                            }
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.FileUpload, null) },
                                text = { Text(stringResource(R.string.action_export_config)) },
                                onClick = { menuExpanded = false; onExportConfig() }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                text = {
                                    Text(
                                        stringResource(R.string.action_delete),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = { menuExpanded = false; onDelete() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(isRunning: Boolean) {
    val color = if (isRunning) StatusRunningColor else MaterialTheme.colorScheme.outline
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (isRunning) stringResource(R.string.status_running)
                else stringResource(R.string.status_stopped),
                style = MaterialTheme.typography.labelSmall,
                color = if (isRunning) StatusRunningColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun RenameCloneDialog(
    clone: CloneItem,
    validateName: (String) -> NameValidationResult,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(clone.cloneName) }
    val validation = validateName(name)
    val errorText = when (validation) {
        NameValidationResult.Empty -> stringResource(R.string.clone_name_error_empty)
        NameValidationResult.TooLong -> stringResource(R.string.clone_name_error_length)
        NameValidationResult.Duplicate -> stringResource(R.string.clone_name_error_duplicate)
        NameValidationResult.Valid -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_dialog_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 34) name = it },
                label = { Text(stringResource(R.string.clone_name_label)) },
                isError = errorText != null,
                supportingText = { Text(errorText ?: "${name.trim().length}/30") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (validation == NameValidationResult.Valid) onConfirm(name.trim()) },
                enabled = validation == NameValidationResult.Valid
            ) {
                Text(stringResource(R.string.clone_save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.clone_cancel_button))
            }
        }
    )
}

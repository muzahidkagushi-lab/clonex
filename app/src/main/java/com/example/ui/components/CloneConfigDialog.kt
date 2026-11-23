package com.example.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.NameValidationResult
import com.example.domain.model.BadgeStyle
import com.example.util.Formatters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CloneConfigDialog(
    title: String,
    confirmLabel: String,
    appDrawable: Drawable?,
    initialName: String,
    initialBadgeLabel: String,
    initialBadgeColorHex: Long,
    badgeStyle: BadgeStyle,
    initialNotifications: Boolean,
    initialHidden: Boolean,
    initialLocked: Boolean,
    showToggles: Boolean = true,
    validateName: (String) -> NameValidationResult,
    onRequestNotificationPermission: (onResult: (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        badgeLabel: String,
        badgeColorHex: Long,
        notifications: Boolean,
        hidden: Boolean,
        locked: Boolean
    ) -> Unit
) {
    var cloneName by remember { mutableStateOf(initialName) }
    var badgeLabel by remember { mutableStateOf(initialBadgeLabel) }
    var badgeColorHex by remember { mutableLongStateOf(initialBadgeColorHex) }
    var notificationsEnabled by remember { mutableStateOf(initialNotifications) }
    var hiddenFromLauncher by remember { mutableStateOf(initialHidden) }
    var isLocked by remember { mutableStateOf(initialLocked) }

    val validation = validateName(cloneName)
    val nameErrorText = when (validation) {
        NameValidationResult.Empty -> stringResource(R.string.clone_name_error_empty)
        NameValidationResult.TooLong -> stringResource(R.string.clone_name_error_length)
        NameValidationResult.Duplicate -> stringResource(R.string.clone_name_error_duplicate)
        NameValidationResult.Valid -> null
    }

    val presetLabels = listOf("2", "3", "A", "B", "PRO", "WORK")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BadgedCloneIcon(
                    drawable = appDrawable,
                    badgeLabel = badgeLabel.ifBlank { "2" },
                    badgeColorHex = badgeColorHex,
                    badgeStyle = badgeStyle,
                    contentDescription = cloneName,
                    size = 48.dp,
                    isLocked = isLocked
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = cloneName,
                    onValueChange = { if (it.length <= 34) cloneName = it },
                    label = { Text(stringResource(R.string.clone_name_label)) },
                    placeholder = { Text(stringResource(R.string.clone_name_hint)) },
                    isError = nameErrorText != null,
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(nameErrorText ?: "")
                            Text("${cloneName.trim().length}/30")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clone_name_input")
                )

                Text(
                    text = stringResource(R.string.clone_badge_label_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetLabels.forEach { preset ->
                        FilterChip(
                            selected = badgeLabel.equals(preset, ignoreCase = true),
                            onClick = { badgeLabel = preset },
                            label = { Text(preset) }
                        )
                    }
                }

                OutlinedTextField(
                    value = badgeLabel,
                    onValueChange = { if (it.length <= 4) badgeLabel = it.uppercase() },
                    label = { Text(stringResource(R.string.clone_badge_label_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clone_badge_label_input")
                )

                Text(
                    text = stringResource(R.string.clone_badge_color_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Formatters.badgePaletteHex.forEach { colorHex ->
                        val selected = badgeColorHex == colorHex
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { badgeColorHex = colorHex }
                                .testTag("badge_color_$colorHex"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (showToggles) {
                    HorizontalDivider()

                    ToggleRow(
                        icon = Icons.Rounded.Notifications,
                        title = stringResource(R.string.clone_option_notifications),
                        subtitle = stringResource(R.string.clone_option_notifications_desc),
                        checked = notificationsEnabled,
                        testTag = "clone_notif_switch",
                        onCheckedChange = { enable ->
                            if (enable) {
                                onRequestNotificationPermission { granted ->
                                    notificationsEnabled = granted
                                }
                            } else {
                                notificationsEnabled = false
                            }
                        }
                    )

                    ToggleRow(
                        icon = Icons.Rounded.VisibilityOff,
                        title = stringResource(R.string.clone_option_hide_launcher),
                        subtitle = stringResource(R.string.clone_option_hide_launcher_desc),
                        checked = hiddenFromLauncher,
                        testTag = "clone_hide_switch",
                        onCheckedChange = { hiddenFromLauncher = it }
                    )

                    ToggleRow(
                        icon = Icons.Rounded.Lock,
                        title = stringResource(R.string.clone_option_lock),
                        subtitle = stringResource(R.string.clone_option_lock_desc),
                        checked = isLocked,
                        testTag = "clone_lock_switch",
                        onCheckedChange = { isLocked = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validation == NameValidationResult.Valid) {
                        onConfirm(
                            cloneName.trim(),
                            badgeLabel.trim().ifEmpty { "2" },
                            badgeColorHex,
                            notificationsEnabled,
                            hiddenFromLauncher,
                            isLocked
                        )
                    }
                },
                enabled = validation == NameValidationResult.Valid,
                modifier = Modifier.testTag("clone_confirm_btn")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("clone_cancel_btn")
            ) {
                Text(stringResource(R.string.clone_cancel_button))
            }
        }
    )
}

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

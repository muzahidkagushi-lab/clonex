package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.domain.model.AppSettings
import com.example.domain.model.AutoLockTimeout
import com.example.domain.model.BadgeStyle
import com.example.domain.model.LanguageOption
import com.example.domain.model.LockMethod
import com.example.domain.model.ThemeMode
import com.example.ui.components.CredentialSetupDialog
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    totalStorageBytes: Long,
    onLanguageChange: (LanguageOption) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onAppLockEnabledChange: (Boolean) -> Unit,
    onLockMethodChange: (LockMethod) -> Unit,
    onSavePin: (String) -> Unit,
    onSavePattern: (String) -> Unit,
    onAutoLockChange: (AutoLockTimeout) -> Unit,
    onDefaultCloneLockChange: (Boolean) -> Unit,
    onMasterNotificationsChange: (Boolean) -> Unit,
    onRequestNotificationPermission: ((Boolean) -> Unit) -> Unit,
    onNameFormatChange: (String) -> Unit,
    onCloneLimitChange: (Int) -> Unit,
    onDefaultBadgeStyleChange: (BadgeStyle) -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
    onAnalyticsChange: (Boolean) -> Unit,
    onClearAllCache: () -> Unit,
    onClearAllData: () -> Unit,
    onExportBackupUri: (Uri) -> Unit,
    onImportBackupUri: (Uri) -> Unit,
    onResetApp: () -> Unit,
    onOpenAboutHelp: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let(onExportBackupUri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(onImportBackupUri)
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLockMethodDialog by remember { mutableStateOf(false) }
    var showCredentialSetup by remember { mutableStateOf(false) }
    var showAutoLockDialog by remember { mutableStateOf(false) }
    var showFormatDialog by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }
    var showBadgeStyleDialog by remember { mutableStateOf(false) }
    var showBatteryGuide by remember { mutableStateOf(false) }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }

    val shareText = stringResource(R.string.share_app_text)
    val rateThanksText = stringResource(R.string.rate_dialog_thanks)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Appearance & Language
                SettingsSection(title = stringResource(R.string.settings_section_general)) {
                    SettingsRow(
                        icon = Icons.Rounded.Language,
                        title = stringResource(R.string.settings_language),
                        subtitle = when (settings.language) {
                            LanguageOption.SYSTEM -> stringResource(R.string.lang_system)
                            LanguageOption.BENGALI -> stringResource(R.string.lang_bengali)
                            LanguageOption.ENGLISH -> stringResource(R.string.lang_english)
                        },
                        testTag = "setting_language_row",
                        onClick = { showLanguageDialog = true }
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Rounded.DarkMode,
                        title = stringResource(R.string.settings_theme),
                        subtitle = when (settings.themeMode) {
                            ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                            ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                            ThemeMode.DARK -> stringResource(R.string.theme_dark)
                        },
                        testTag = "setting_theme_row",
                        onClick = { showThemeDialog = true }
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        HorizontalDivider()
                        SettingsSwitchRow(
                            icon = Icons.Rounded.ColorLens,
                            title = stringResource(R.string.settings_dynamic_color),
                            subtitle = stringResource(R.string.settings_dynamic_color_desc),
                            checked = settings.dynamicColor,
                            onCheckedChange = onDynamicColorChange
                        )
                    }
                }

                // Security & Lock
                SettingsSection(title = stringResource(R.string.settings_section_security)) {
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Lock,
                        title = stringResource(R.string.settings_app_lock),
                        subtitle = stringResource(R.string.settings_app_lock_desc),
                        checked = settings.appLockEnabled,
                        onCheckedChange = { enable ->
                            if (enable && settings.pinHash.isEmpty() && settings.patternHash.isEmpty()) {
                                showCredentialSetup = true
                            } else {
                                onAppLockEnabledChange(enable)
                            }
                        }
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Style,
                        title = stringResource(R.string.settings_lock_method),
                        subtitle = when (settings.lockMethod) {
                            LockMethod.PIN -> stringResource(R.string.lock_method_pin)
                            LockMethod.PATTERN -> stringResource(R.string.lock_method_pattern)
                            LockMethod.BIOMETRIC -> stringResource(R.string.lock_method_biometric)
                        },
                        onClick = { showLockMethodDialog = true }
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Key,
                        title = stringResource(R.string.settings_configure_credential),
                        subtitle = stringResource(R.string.lock_enter_pin),
                        onClick = { showCredentialSetup = true }
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Timer,
                        title = stringResource(R.string.settings_auto_lock),
                        subtitle = when (settings.autoLockTimeout) {
                            AutoLockTimeout.IMMEDIATE -> stringResource(R.string.auto_lock_immediate)
                            AutoLockTimeout.SECONDS_30 -> stringResource(R.string.auto_lock_30s)
                            AutoLockTimeout.MINUTE_1 -> stringResource(R.string.auto_lock_1m)
                            AutoLockTimeout.MINUTES_5 -> stringResource(R.string.auto_lock_5m)
                        },
                        onClick = { showAutoLockDialog = true }
                    )
                    HorizontalDivider()
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Lock,
                        title = stringResource(R.string.settings_default_clone_lock),
                        subtitle = stringResource(R.string.settings_default_clone_lock_desc),
                        checked = settings.defaultCloneLock,
                        onCheckedChange = onDefaultCloneLockChange
                    )
                }

                // Cloning & Notifications
                SettingsSection(title = stringResource(R.string.settings_section_cloning)) {
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Notifications,
                        title = stringResource(R.string.settings_master_notifications),
                        subtitle = stringResource(R.string.settings_master_notifications_desc),
                        checked = settings.masterNotificationsEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                onRequestNotificationPermission { granted ->
                                    onMasterNotificationsChange(granted)
                                }
                            } else {
                                onMasterNotificationsChange(false)
                            }
                        }
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Rounded.TextFields,
                        title = stringResource(R.string.settings_name_format),
                        subtitle = stringResource(
                            R.string.settings_name_format_desc,
                            settings.cloneNameFormat
                        ),
                        onClick = { showFormatDialog = true }
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Rounded.FormatListNumbered,
                        title = stringResource(R.string.settings_clone_limit),
                        subtitle = if (settings.cloneLimit <= 0) {
                            stringResource(R.string.clone_limit_unlimited)
                        } else {
                            stringResource(R.string.clone_limit_custom, settings.cloneLimit)
                        },
                        onClick = { showLimitDialog = true }
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Palette,
                        title = stringResource(R.string.settings_default_badge_style),
                        subtitle = when (settings.defaultBadgeStyle) {
                            BadgeStyle.CORNER_PILL -> stringResource(R.string.badge_style_corner)
                            BadgeStyle.CIRCLE_DOT -> stringResource(R.string.badge_style_circle)
                            BadgeStyle.TOP_RIBBON -> stringResource(R.string.badge_style_ribbon)
                        },
                        onClick = { showBadgeStyleDialog = true }
                    )
                    HorizontalDivider()
                    SettingsSwitchRow(
                        icon = Icons.Rounded.PowerSettingsNew,
                        title = stringResource(R.string.settings_auto_start),
                        subtitle = stringResource(R.string.settings_auto_start_desc),
                        checked = settings.autoStartOnBoot,
                        onCheckedChange = onAutoStartChange
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Rounded.BatteryChargingFull,
                        title = stringResource(R.string.settings_battery_guide),
                        subtitle = stringResource(R.string.settings_battery_guide_desc),
                        onClick = { showBatteryGuide = true }
                    )
                }

                // Storage & Backup
                SettingsSection(title = stringResource(R.string.settings_section_storage)) {
                    Text(
                        text = stringResource(
                            R.string.settings_total_storage,
                            Formatters.formatBytes(totalStorageBytes)
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Rounded.CleaningServices,
                        title = stringResource(R.string.settings_clear_all_cache),
                        subtitle = null,
                        onClick = onClearAllCache
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Rounded.DeleteSweep,
                        title = stringResource(R.string.settings_clear_all_data),
                        subtitle = null,
                        isDestructive = true,
                        onClick = { showClearDataConfirm = true }
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Upload,
                        title = stringResource(R.string.settings_export_backup),
                        subtitle = "clonex-backup.json",
                        onClick = { exportLauncher.launch("clonex-backup.json") }
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Download,
                        title = stringResource(R.string.settings_import_backup),
                        subtitle = "JSON",
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }
                    )
                }

                // Privacy & Reset
                SettingsSection(title = stringResource(R.string.settings_section_privacy_reset)) {
                    SettingsSwitchRow(
                        icon = Icons.Rounded.BugReport,
                        title = stringResource(R.string.settings_analytics),
                        subtitle = stringResource(R.string.settings_analytics_desc),
                        checked = settings.localAnalyticsEnabled,
                        onCheckedChange = onAnalyticsChange
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Rounded.RestartAlt,
                        title = stringResource(R.string.settings_reset_app),
                        subtitle = stringResource(R.string.settings_reset_app_desc),
                        isDestructive = true,
                        onClick = { showResetConfirm = true }
                    )
                }

                // App Info & Support
                SettingsSection(title = stringResource(R.string.settings_section_info)) {
                    SettingsRow(
                        icon = Icons.AutoMirrored.Rounded.HelpOutline,
                        title = stringResource(R.string.settings_about_faq),
                        subtitle = stringResource(R.string.app_full_title),
                        testTag = "setting_about_faq_row",
                        onClick = onOpenAboutHelp
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Info,
                        title = stringResource(R.string.settings_version),
                        subtitle = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        onClick = {}
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Description,
                        title = stringResource(R.string.settings_licenses),
                        subtitle = "Jetpack Compose, Material 3, Room, Moshi",
                        onClick = { showLicensesDialog = true }
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Rounded.StarRate,
                        title = stringResource(R.string.settings_rate_app),
                        subtitle = null,
                        onClick = { showRateDialog = true }
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Share,
                        title = stringResource(R.string.settings_share_app),
                        subtitle = null,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                    )
                    HorizontalDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Email,
                        title = stringResource(R.string.settings_contact_support),
                        subtitle = "support@clonex.local",
                        onClick = {
                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:support@clonex.local")
                                putExtra(Intent.EXTRA_SUBJECT, "CloneX Support")
                            }
                            runCatching { context.startActivity(emailIntent) }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Language Picker Dialog
    if (showLanguageDialog) {
        RadioPickerDialog(
            title = stringResource(R.string.settings_language),
            options = listOf(
                LanguageOption.SYSTEM to stringResource(R.string.lang_system),
                LanguageOption.BENGALI to stringResource(R.string.lang_bengali),
                LanguageOption.ENGLISH to stringResource(R.string.lang_english)
            ),
            selected = settings.language,
            onSelect = {
                onLanguageChange(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    // Theme Picker Dialog
    if (showThemeDialog) {
        RadioPickerDialog(
            title = stringResource(R.string.settings_theme),
            options = listOf(
                ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                ThemeMode.LIGHT to stringResource(R.string.theme_light),
                ThemeMode.DARK to stringResource(R.string.theme_dark)
            ),
            selected = settings.themeMode,
            onSelect = {
                onThemeModeChange(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    // Lock Method Picker Dialog
    if (showLockMethodDialog) {
        RadioPickerDialog(
            title = stringResource(R.string.settings_lock_method),
            options = listOf(
                LockMethod.PIN to stringResource(R.string.lock_method_pin),
                LockMethod.PATTERN to stringResource(R.string.lock_method_pattern),
                LockMethod.BIOMETRIC to stringResource(R.string.lock_method_biometric)
            ),
            selected = settings.lockMethod,
            onSelect = { method ->
                onLockMethodChange(method)
                showLockMethodDialog = false
                if ((method == LockMethod.PIN && settings.pinHash.isEmpty()) ||
                    (method == LockMethod.PATTERN && settings.patternHash.isEmpty())
                ) {
                    showCredentialSetup = true
                }
            },
            onDismiss = { showLockMethodDialog = false }
        )
    }

    // PIN / Pattern Setup Dialog
    if (showCredentialSetup) {
        CredentialSetupDialog(
            initialMethod = settings.lockMethod,
            onSavePin = onSavePin,
            onSavePattern = onSavePattern,
            onDismiss = { showCredentialSetup = false }
        )
    }

    // Auto-Lock Timeout Dialog
    if (showAutoLockDialog) {
        RadioPickerDialog(
            title = stringResource(R.string.settings_auto_lock),
            options = listOf(
                AutoLockTimeout.IMMEDIATE to stringResource(R.string.auto_lock_immediate),
                AutoLockTimeout.SECONDS_30 to stringResource(R.string.auto_lock_30s),
                AutoLockTimeout.MINUTE_1 to stringResource(R.string.auto_lock_1m),
                AutoLockTimeout.MINUTES_5 to stringResource(R.string.auto_lock_5m)
            ),
            selected = settings.autoLockTimeout,
            onSelect = {
                onAutoLockChange(it)
                showAutoLockDialog = false
            },
            onDismiss = { showAutoLockDialog = false }
        )
    }

    // Badge Style Dialog
    if (showBadgeStyleDialog) {
        RadioPickerDialog(
            title = stringResource(R.string.settings_default_badge_style),
            options = listOf(
                BadgeStyle.CORNER_PILL to stringResource(R.string.badge_style_corner),
                BadgeStyle.CIRCLE_DOT to stringResource(R.string.badge_style_circle),
                BadgeStyle.TOP_RIBBON to stringResource(R.string.badge_style_ribbon)
            ),
            selected = settings.defaultBadgeStyle,
            onSelect = {
                onDefaultBadgeStyleChange(it)
                showBadgeStyleDialog = false
            },
            onDismiss = { showBadgeStyleDialog = false }
        )
    }

    // Naming Format Dialog
    if (showFormatDialog) {
        var format by remember { mutableStateOf(settings.cloneNameFormat) }
        val presets = listOf("{name} {n}", "{name} ({n})", "Clone {n} · {name}")
        AlertDialog(
            onDismissRequest = { showFormatDialog = false },
            title = { Text(stringResource(R.string.settings_name_format)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        presets.forEach { preset ->
                            FilterChip(
                                selected = format == preset,
                                onClick = { format = preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = format,
                        onValueChange = { format = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onNameFormatChange(format)
                    showFormatDialog = false
                }) {
                    Text(stringResource(R.string.clone_save_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFormatDialog = false }) {
                    Text(stringResource(R.string.clone_cancel_button))
                }
            }
        )
    }

    // Clone Limit Dialog
    if (showLimitDialog) {
        var unlimited by remember { mutableStateOf(settings.cloneLimit <= 0) }
        var sliderVal by remember { mutableFloatStateOf((if (settings.cloneLimit > 0) settings.cloneLimit else 10).toFloat()) }
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text(stringResource(R.string.settings_clone_limit)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { unlimited = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = unlimited, onClick = { unlimited = true })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.clone_limit_unlimited))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { unlimited = false },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = !unlimited, onClick = { unlimited = false })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.clone_limit_custom, sliderVal.toInt()))
                    }
                    if (!unlimited) {
                        Slider(
                            value = sliderVal,
                            onValueChange = { sliderVal = it },
                            valueRange = 1f..50f,
                            steps = 48
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCloneLimitChange(if (unlimited) 0 else sliderVal.toInt())
                    showLimitDialog = false
                }) {
                    Text(stringResource(R.string.clone_save_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text(stringResource(R.string.clone_cancel_button))
                }
            }
        )
    }

    // Battery Optimization Guide Dialog
    if (showBatteryGuide) {
        AlertDialog(
            onDismissRequest = { showBatteryGuide = false },
            icon = { Icon(Icons.Rounded.BatteryChargingFull, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.battery_guide_title)) },
            text = { Text(stringResource(R.string.battery_guide_body)) },
            confirmButton = {
                Button(onClick = {
                    showBatteryGuide = false
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    runCatching { context.startActivity(intent) }
                }) {
                    Text(stringResource(R.string.battery_open_system))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryGuide = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // Clear All Data Confirmation
    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text(stringResource(R.string.settings_clear_all_data)) },
            text = { Text(stringResource(R.string.settings_clear_all_data_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
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

    // Reset App Confirmation
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            icon = { Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.reset_confirm_title)) },
            text = { Text(stringResource(R.string.reset_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetApp()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.clone_cancel_button))
                }
            }
        )
    }

    // Open Source Licenses Dialog
    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text(stringResource(R.string.settings_licenses)) },
            text = {
                Text(
                    text = "• Jetpack Compose & Material 3 (Apache 2.0)\n" +
                        "• AndroidX Room & DataStore (Apache 2.0)\n" +
                        "• Kotlin Coroutines (Apache 2.0)\n" +
                        "• Moshi JSON (Apache 2.0)\n" +
                        "• Noto Sans Bengali & Plus Jakarta Sans (SIL OFL 1.1)",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = { showLicensesDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // Rate App Dialog
    if (showRateDialog) {
        AlertDialog(
            onDismissRequest = { showRateDialog = false },
            icon = { Icon(Icons.Rounded.StarRate, null, tint = MaterialTheme.colorScheme.secondary) },
            title = { Text(stringResource(R.string.rate_dialog_title)) },
            text = { Text(stringResource(R.string.rate_dialog_body)) },
            confirmButton = {
                Button(onClick = {
                    showRateDialog = false
                    onShowMessage(rateThanksText)
                }) {
                    Text("★★★★★")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRateDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    isDestructive: Boolean = false,
    testTag: String? = null,
    onClick: () -> Unit
) {
    val tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(14.dp))
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

@Composable
private fun <T> RadioPickerDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = value == selected, onClick = { onSelect(value) })
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

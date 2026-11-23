package com.example.ui.navigation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ControlPointDuplicate
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ControlPointDuplicate
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.ui.components.NotificationRationaleDialog
import com.example.ui.components.SandboxSessionDialog
import com.example.ui.components.SecurityLockOverlay
import com.example.ui.screens.AboutHelpScreen
import com.example.ui.screens.CloneDetailsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyClonesScreen
import com.example.ui.screens.ResponsibleUseDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashAndOnboardingScreen
import com.example.ui.viewmodel.LockChallenge
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiEvent

object Routes {
    const val HOME = "home"
    const val CLONES = "clones"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val CLONE_DETAILS = "clone_details/{cloneId}"

    fun cloneDetails(cloneId: String) = "clone_details/$cloneId"
}

private data class TopLevelNavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun CloneXAppRoot(
    viewModel: MainViewModel,
    isBengali: Boolean,
    onRecreateForLocale: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val clones by viewModel.clones.collectAsStateWithLifecycle()
    val appsUiState by viewModel.appsUiState.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeFilter by viewModel.appFilter.collectAsStateWithLifecycle()
    val activeSort by viewModel.appSort.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val totalStorageBytes by viewModel.totalCloneStorageBytes.collectAsStateWithLifecycle()
    val selectedDetailCloneId by viewModel.selectedDetailCloneId.collectAsStateWithLifecycle()
    val activeSandboxClone by viewModel.activeSandboxClone.collectAsStateWithLifecycle()
    val activeLockChallenge by viewModel.activeLockChallenge.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Notification permission rationale + runtime request coordination
    var pendingNotifCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    var showNotifRationale by remember { mutableStateOf(false) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingNotifCallback?.invoke(granted)
        pendingNotifCallback = null
    }

    fun requestNotificationPermissionWithRationale(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onResult(true)
            return
        }
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            onResult(true)
            return
        }
        pendingNotifCallback = onResult
        showNotifRationale = true
    }

    val undoLabel = stringResource(R.string.snackbar_undo)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> {
                    if (event.isSuccessHaptic) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    snackbarHostState.showSnackbar(event.text)
                }
                is UiEvent.ShowDeletedUndo -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDeleteClone(event.clone)
                    }
                }
                is UiEvent.ShareText -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_TEXT, event.text)
                        putExtra(Intent.EXTRA_SUBJECT, event.subject)
                    }
                    context.startActivity(Intent.createChooser(intent, event.subject))
                }
                UiEvent.RecreateForLocale -> {
                    onRecreateForLocale()
                }
            }
        }
    }

    // First launch Splash & Onboarding
    if (!settings.onboardingCompleted) {
        SplashAndOnboardingScreen(
            currentLanguage = settings.language,
            onSelectLanguage = viewModel::setLanguage,
            onComplete = viewModel::completeOnboardingAndTerms
        )
        return
    }

    // Safety net if terms weren't accepted yet (e.g., reset)
    var showPrivacyFromTerms by remember { mutableStateOf(false) }
    if (!settings.termsAccepted) {
        ResponsibleUseDialog(
            onReadPrivacy = { showPrivacyFromTerms = true },
            onAccept = viewModel::acceptTermsOnly
        )
        if (showPrivacyFromTerms) {
            AlertDialog(
                onDismissRequest = { showPrivacyFromTerms = false },
                title = { Text(stringResource(R.string.privacy_policy_title)) },
                text = { Text(stringResource(R.string.privacy_policy_body)) },
                confirmButton = {
                    Button(onClick = { showPrivacyFromTerms = false }) {
                        Text(stringResource(R.string.close))
                    }
                }
            )
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.HOME

    var showExitConfirm by remember { mutableStateOf(false) }

    // Exit confirmation on Home root
    BackHandler(enabled = currentRoute == Routes.HOME && activeLockChallenge == null && activeSandboxClone == null) {
        showExitConfirm = true
    }

    val navItems = remember {
        listOf(
            TopLevelNavItem(
                route = Routes.HOME,
                labelRes = R.string.nav_home,
                selectedIcon = Icons.Rounded.Apps,
                unselectedIcon = Icons.Outlined.Apps,
                testTag = "nav_home_tab"
            ),
            TopLevelNavItem(
                route = Routes.CLONES,
                labelRes = R.string.nav_clones,
                selectedIcon = Icons.Rounded.ControlPointDuplicate,
                unselectedIcon = Icons.Outlined.ControlPointDuplicate,
                testTag = "nav_clones_tab"
            ),
            TopLevelNavItem(
                route = Routes.SETTINGS,
                labelRes = R.string.nav_settings,
                selectedIcon = Icons.Rounded.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                testTag = "nav_settings_tab"
            ),
            TopLevelNavItem(
                route = Routes.ABOUT,
                labelRes = R.string.nav_about,
                selectedIcon = Icons.Rounded.Help,
                unselectedIcon = Icons.Outlined.HelpOutline,
                testTag = "nav_about_tab"
            )
        )
    }

    val isTopLevel = currentRoute in navItems.map { it.route }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isMediumOrExpanded = maxWidth >= 600.dp
        val isExpanded = maxWidth >= 840.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.navigationBars,
            bottomBar = {
                if (!isMediumOrExpanded && isTopLevel) {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = {
                                    if (item.route == Routes.CLONES && clones.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge { Text(clones.size.toString()) }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = stringResource(item.labelRes)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelRes)
                                        )
                                    }
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isMediumOrExpanded && isTopLevel) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = {
                                    if (item.route == Routes.CLONES && clones.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge { Text(clones.size.toString()) }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = stringResource(item.labelRes)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelRes)
                                        )
                                    }
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                    VerticalDivider()
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            appsUiState = appsUiState,
                            filteredApps = filteredApps,
                            clones = clones,
                            settings = settings,
                            searchQuery = searchQuery,
                            activeFilter = activeFilter,
                            activeSort = activeSort,
                            isRefreshing = isRefreshing,
                            isLimitReached = viewModel.isCloneLimitReached(),
                            onSearchChange = viewModel::updateSearchQuery,
                            onFilterChange = viewModel::updateFilter,
                            onSortChange = viewModel::updateSort,
                            onRefresh = { viewModel.refreshInstalledApps(isInitial = false) },
                            onViewClonesForApp = { navigateTopLevel(Routes.CLONES) },
                            validateName = { name -> viewModel.validateCloneName(name) },
                            onRequestNotificationPermission = ::requestNotificationPermissionWithRationale,
                            onCreateClone = viewModel::createClone
                        )
                    }

                    composable(Routes.CLONES) {
                        if (isExpanded) {
                            // Tablet List-Detail canonical layout
                            val detailTarget = clones.firstOrNull { it.id == selectedDetailCloneId }
                                ?: clones.firstOrNull()
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    MyClonesScreen(
                                        clones = clones,
                                        isBengali = isBengali,
                                        selectedCloneId = detailTarget?.id,
                                        getAppIcon = viewModel::getAppIcon,
                                        validateName = viewModel::validateCloneName,
                                        onRequestNotificationPermission = ::requestNotificationPermissionWithRationale,
                                        onOpenClone = viewModel::requestLaunchClone,
                                        onStopClone = viewModel::stopClone,
                                        onStopClones = viewModel::stopClones,
                                        onUpdateClone = viewModel::updateClone,
                                        onPinShortcut = viewModel::pinShortcut,
                                        onClearCache = viewModel::clearCloneCache,
                                        onClearData = viewModel::clearCloneData,
                                        onExportConfig = viewModel::exportSingleCloneConfig,
                                        onDeleteClone = viewModel::deleteClone,
                                        onDeleteClones = viewModel::deleteClones,
                                        onSelectDetails = { clone -> viewModel.selectDetailClone(clone.id) },
                                        onBrowseApps = { navigateTopLevel(Routes.HOME) }
                                    )
                                }
                                if (clones.isNotEmpty()) {
                                    VerticalDivider(modifier = Modifier.width(1.dp))
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        CloneDetailsScreen(
                                            clone = detailTarget,
                                            isBengali = isBengali,
                                            showBackButton = false,
                                            getAppIcon = viewModel::getAppIcon,
                                            loadGrantedPermissions = viewModel::getGrantedPermissions,
                                            validateName = viewModel::validateCloneName,
                                            onRequestNotificationPermission = ::requestNotificationPermissionWithRationale,
                                            onBack = {},
                                            onOpenClone = viewModel::requestLaunchClone,
                                            onStopClone = viewModel::stopClone,
                                            onUpdateClone = viewModel::updateClone,
                                            onPinShortcut = viewModel::pinShortcut,
                                            onClearCache = viewModel::clearCloneCache,
                                            onClearData = viewModel::clearCloneData,
                                            onExportConfig = viewModel::exportSingleCloneConfig,
                                            onDeleteClone = viewModel::deleteClone
                                        )
                                    }
                                }
                            }
                        } else {
                            MyClonesScreen(
                                clones = clones,
                                isBengali = isBengali,
                                selectedCloneId = null,
                                getAppIcon = viewModel::getAppIcon,
                                validateName = viewModel::validateCloneName,
                                onRequestNotificationPermission = ::requestNotificationPermissionWithRationale,
                                onOpenClone = viewModel::requestLaunchClone,
                                onStopClone = viewModel::stopClone,
                                onStopClones = viewModel::stopClones,
                                onUpdateClone = viewModel::updateClone,
                                onPinShortcut = viewModel::pinShortcut,
                                onClearCache = viewModel::clearCloneCache,
                                onClearData = viewModel::clearCloneData,
                                onExportConfig = viewModel::exportSingleCloneConfig,
                                onDeleteClone = viewModel::deleteClone,
                                onDeleteClones = viewModel::deleteClones,
                                onSelectDetails = { clone ->
                                    viewModel.selectDetailClone(clone.id)
                                    navController.navigate(Routes.cloneDetails(clone.id))
                                },
                                onBrowseApps = { navigateTopLevel(Routes.HOME) }
                            )
                        }
                    }

                    composable(
                        route = Routes.CLONE_DETAILS,
                        arguments = listOf(navArgument("cloneId") { type = NavType.StringType })
                    ) { entry ->
                        val cloneId = entry.arguments?.getString("cloneId") ?: ""
                        val observedClone by viewModel.observeClone(cloneId)
                            .collectAsStateWithLifecycle(initialValue = clones.firstOrNull { it.id == cloneId })
                        CloneDetailsScreen(
                            clone = observedClone,
                            isBengali = isBengali,
                            showBackButton = true,
                            getAppIcon = viewModel::getAppIcon,
                            loadGrantedPermissions = viewModel::getGrantedPermissions,
                            validateName = viewModel::validateCloneName,
                            onRequestNotificationPermission = ::requestNotificationPermissionWithRationale,
                            onBack = { navController.popBackStack() },
                            onOpenClone = viewModel::requestLaunchClone,
                            onStopClone = viewModel::stopClone,
                            onUpdateClone = viewModel::updateClone,
                            onPinShortcut = viewModel::pinShortcut,
                            onClearCache = viewModel::clearCloneCache,
                            onClearData = viewModel::clearCloneData,
                            onExportConfig = viewModel::exportSingleCloneConfig,
                            onDeleteClone = viewModel::deleteClone
                        )
                    }

                    composable(Routes.SETTINGS) {
                        val scopeMsg = remember { mutableStateOf<String?>(null) }
                        scopeMsg.value?.let { text ->
                            LaunchedEffect(text) {
                                snackbarHostState.showSnackbar(text)
                                scopeMsg.value = null
                            }
                        }
                        SettingsScreen(
                            settings = settings,
                            totalStorageBytes = totalStorageBytes,
                            onLanguageChange = viewModel::setLanguage,
                            onThemeModeChange = viewModel::setThemeMode,
                            onDynamicColorChange = viewModel::setDynamicColor,
                            onAppLockEnabledChange = viewModel::setAppLockEnabled,
                            onLockMethodChange = viewModel::setLockMethod,
                            onSavePin = viewModel::savePin,
                            onSavePattern = viewModel::savePattern,
                            onAutoLockChange = viewModel::setAutoLockTimeout,
                            onDefaultCloneLockChange = viewModel::setDefaultCloneLock,
                            onMasterNotificationsChange = viewModel::setMasterNotifications,
                            onRequestNotificationPermission = ::requestNotificationPermissionWithRationale,
                            onNameFormatChange = viewModel::setCloneNameFormat,
                            onCloneLimitChange = viewModel::setCloneLimit,
                            onDefaultBadgeStyleChange = viewModel::setDefaultBadgeStyle,
                            onAutoStartChange = viewModel::setAutoStartOnBoot,
                            onAnalyticsChange = viewModel::setLocalAnalytics,
                            onClearAllCache = viewModel::clearAllCaches,
                            onClearAllData = viewModel::clearAllCloneData,
                            onExportBackupUri = viewModel::exportBackupToUri,
                            onImportBackupUri = viewModel::importBackupFromUri,
                            onResetApp = viewModel::resetEntireApp,
                            onOpenAboutHelp = { navigateTopLevel(Routes.ABOUT) },
                            onShowMessage = { msg -> scopeMsg.value = msg }
                        )
                    }

                    composable(Routes.ABOUT) {
                        AboutHelpScreen(
                            showBackButton = false,
                            onBack = { navigateTopLevel(Routes.HOME) }
                        )
                    }
                }
            }
        }
    }

    // Android 13+ Notification Permission Rationale Dialog
    if (showNotifRationale) {
        NotificationRationaleDialog(
            onAllow = {
                showNotifRationale = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    pendingNotifCallback?.invoke(true)
                    pendingNotifCallback = null
                }
            },
            onDismiss = {
                showNotifRationale = false
                pendingNotifCallback?.invoke(false)
                pendingNotifCallback = null
            }
        )
    }

    // Active Isolated Sandbox Preview Dialog on Clone Launch
    activeSandboxClone?.let { runningClone ->
        SandboxSessionDialog(
            clone = runningClone,
            appDrawable = viewModel.getAppIcon(runningClone.originalPackageName),
            onOpenHostApp = viewModel::openHostPackage,
            onStopClone = viewModel::stopClone,
            onDismiss = viewModel::dismissActiveSandbox
        )
    }

    // App Lock / Per-Clone Lock Overlay
    activeLockChallenge?.let { challenge ->
        val subtitle = when (challenge) {
            LockChallenge.AppUnlock -> stringResource(R.string.lock_subtitle_app)
            is LockChallenge.CloneUnlock -> stringResource(R.string.lock_subtitle_clone, challenge.clone.cloneName)
        }
        SecurityLockOverlay(
            subtitle = subtitle,
            settings = settings,
            canDismiss = challenge is LockChallenge.CloneUnlock,
            onUnlockSuccess = viewModel::onLockSucceeded,
            onDismiss = viewModel::dismissCloneLockChallenge
        )
    }

    // Exit Confirmation Dialog on Home
    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text(stringResource(R.string.exit_confirm_title)) },
            text = { Text(stringResource(R.string.exit_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirm = false
                        (context as? Activity)?.finish()
                    },
                    modifier = Modifier.testTag("exit_confirm_yes_btn")
                ) {
                    Text(stringResource(R.string.exit_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text(stringResource(R.string.exit_confirm_no))
                }
            }
        )
    }
}

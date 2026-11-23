package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ControlPointDuplicate
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.NameValidationResult
import com.example.domain.model.AppFilter
import com.example.domain.model.AppSettings
import com.example.domain.model.AppSort
import com.example.domain.model.CloneItem
import com.example.domain.model.InstalledApp
import com.example.ui.components.AppIconImage
import com.example.ui.components.AppsSkeletonList
import com.example.ui.components.CloneConfigDialog
import com.example.ui.viewmodel.AppWithCloneCount
import com.example.ui.viewmodel.AppsUiState
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    appsUiState: AppsUiState,
    filteredApps: List<AppWithCloneCount>,
    clones: List<CloneItem>,
    settings: AppSettings,
    searchQuery: String,
    activeFilter: AppFilter,
    activeSort: AppSort,
    isRefreshing: Boolean,
    isLimitReached: Boolean,
    onSearchChange: (String) -> Unit,
    onFilterChange: (AppFilter) -> Unit,
    onSortChange: (AppSort) -> Unit,
    onRefresh: () -> Unit,
    onViewClonesForApp: () -> Unit,
    validateName: (String) -> NameValidationResult,
    onRequestNotificationPermission: (onResult: (Boolean) -> Unit) -> Unit,
    onCreateClone: (
        app: InstalledApp,
        name: String,
        badgeLabel: String,
        badgeColorHex: Long,
        notifications: Boolean,
        hidden: Boolean,
        locked: Boolean
    ) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedAppForSheet by remember { mutableStateOf<AppWithCloneCount?>(null) }
    var appToCloneDialog by remember { mutableStateOf<InstalledApp?>(null) }
    var showQuickClonePicker by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Back button closes sheets first before navigating out
    BackHandler(enabled = selectedAppForSheet != null || showQuickClonePicker || appToCloneDialog != null) {
        when {
            appToCloneDialog != null -> appToCloneDialog = null
            selectedAppForSheet != null -> selectedAppForSheet = null
            showQuickClonePicker -> showQuickClonePicker = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_full_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.home_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Box {
                        TextButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("home_sort_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Sort,
                                contentDescription = stringResource(R.string.sort_label)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (activeSort) {
                                    AppSort.A_Z -> stringResource(R.string.sort_az)
                                    AppSort.RECENTLY_USED -> stringResource(R.string.sort_recent)
                                    AppSort.MOST_CLONED -> stringResource(R.string.sort_most_cloned)
                                }
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_az)) },
                                onClick = {
                                    onSortChange(AppSort.A_Z)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_recent)) },
                                onClick = {
                                    onSortChange(AppSort.RECENTLY_USED)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_most_cloned)) },
                                onClick = {
                                    onSortChange(AppSort.MOST_CLONED)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickClonePicker = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("home_quick_clone_fab")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.fab_quick_clone)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text(stringResource(R.string.home_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.clear_search)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeFilter == AppFilter.ALL,
                    onClick = { onFilterChange(AppFilter.ALL) },
                    label = { Text(stringResource(R.string.filter_all)) },
                    modifier = Modifier.testTag("filter_all_chip")
                )
                FilterChip(
                    selected = activeFilter == AppFilter.USER,
                    onClick = { onFilterChange(AppFilter.USER) },
                    label = { Text(stringResource(R.string.filter_user)) },
                    modifier = Modifier.testTag("filter_user_chip")
                )
                FilterChip(
                    selected = activeFilter == AppFilter.SYSTEM,
                    onClick = { onFilterChange(AppFilter.SYSTEM) },
                    label = { Text(stringResource(R.string.filter_system)) },
                    modifier = Modifier.testTag("filter_system_chip")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when (appsUiState) {
                    AppsUiState.Loading -> {
                        AppsSkeletonList(
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    is AppsUiState.Error -> {
                        EmptyOrErrorState(
                            icon = Icons.Rounded.ErrorOutline,
                            title = stringResource(R.string.apps_error_title),
                            description = appsUiState.message,
                            actionLabel = stringResource(R.string.apps_error_retry),
                            onAction = onRefresh
                        )
                    }
                    is AppsUiState.Success -> {
                        if (filteredApps.isEmpty()) {
                            EmptyOrErrorState(
                                icon = Icons.Rounded.SearchOff,
                                title = stringResource(R.string.apps_empty_title),
                                description = stringResource(R.string.apps_empty_desc),
                                actionLabel = if (searchQuery.isNotEmpty() || activeFilter != AppFilter.ALL) {
                                    stringResource(R.string.clear_search)
                                } else null,
                                onAction = {
                                    onSearchChange("")
                                    onFilterChange(AppFilter.ALL)
                                }
                            )
                        } else {
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val columns = when {
                                    maxWidth >= 840.dp -> 3
                                    maxWidth >= 600.dp -> 2
                                    else -> 1
                                }
                                if (columns == 1) {
                                    LazyColumn(
                                        contentPadding = PaddingValues(top = 4.dp, bottom = 92.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(
                                            items = filteredApps,
                                            key = { it.app.packageName }
                                        ) { item ->
                                            InstalledAppCard(
                                                item = item,
                                                onClick = { selectedAppForSheet = item }
                                            )
                                        }
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(columns),
                                        contentPadding = PaddingValues(top = 4.dp, bottom = 92.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(
                                            items = filteredApps,
                                            key = { it.app.packageName }
                                        ) { item ->
                                            InstalledAppCard(
                                                item = item,
                                                onClick = { selectedAppForSheet = item }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheet on tapping an app
    selectedAppForSheet?.let { selected ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedAppForSheet = null },
            sheetState = sheetState
        ) {
            AppActionBottomSheetContent(
                item = selected,
                isLimitReached = isLimitReached,
                cloneLimit = settings.cloneLimit,
                onCloneClick = {
                    val app = selected.app
                    selectedAppForSheet = null
                    appToCloneDialog = app
                },
                onViewClonesClick = {
                    selectedAppForSheet = null
                    onViewClonesForApp()
                }
            )
        }
    }

    // Quick-Pick Sheet from '+' FAB
    if (showQuickClonePicker && appsUiState is AppsUiState.Success) {
        val quickSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val cloneableApps = remember(appsUiState.apps) {
            appsUiState.apps.filter { !it.isSelfApp && !it.isSystemApp }
                .ifEmpty { appsUiState.apps.filter { !it.isSelfApp } }
        }
        ModalBottomSheet(
            onDismissRequest = { showQuickClonePicker = false },
            sheetState = quickSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.quick_clone_sheet_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cloneableApps, key = { it.packageName }) { app ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showQuickClonePicker = false
                                    appToCloneDialog = app
                                },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIconImage(
                                    drawable = app.iconDrawable,
                                    contentDescription = app.appName,
                                    size = 42.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ControlPointDuplicate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Clone Configuration Dialog
    appToCloneDialog?.let { targetApp ->
        val (suggestedName, suggestedBadge) = remember(targetApp.packageName, clones, settings.cloneNameFormat) {
            Formatters.generateUniqueCloneName(
                formatTemplate = settings.cloneNameFormat,
                appName = targetApp.appName,
                existingClones = clones
            )
        }
        CloneConfigDialog(
            title = stringResource(R.string.clone_dialog_title),
            confirmLabel = stringResource(R.string.clone_create_button),
            appDrawable = targetApp.iconDrawable,
            initialName = suggestedName,
            initialBadgeLabel = suggestedBadge,
            initialBadgeColorHex = Formatters.badgePaletteHex.first(),
            badgeStyle = settings.defaultBadgeStyle,
            initialNotifications = settings.masterNotificationsEnabled,
            initialHidden = false,
            initialLocked = settings.defaultCloneLock,
            validateName = validateName,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onDismiss = { appToCloneDialog = null },
            onConfirm = { name, badgeLabel, badgeColorHex, notifications, hidden, locked ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCreateClone(
                    targetApp,
                    name,
                    badgeLabel,
                    badgeColorHex,
                    notifications,
                    hidden,
                    locked
                )
                appToCloneDialog = null
            }
        )
    }
}

@Composable
private fun InstalledAppCard(
    item: AppWithCloneCount,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("app_card_${item.app.packageName}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconImage(
                drawable = item.app.iconDrawable,
                contentDescription = item.app.appName,
                size = 48.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.app.isSystemApp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = stringResource(R.string.sheet_type_system),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (item.cloneCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (item.cloneCount == 1) stringResource(R.string.clone_count_single)
                        else stringResource(R.string.clone_count_badge, item.cloneCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppActionBottomSheetContent(
    item: AppWithCloneCount,
    isLimitReached: Boolean,
    cloneLimit: Int,
    onCloneClick: () -> Unit,
    onViewClonesClick: () -> Unit
) {
    val app = item.app
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIconImage(
                drawable = app.iconDrawable,
                contentDescription = app.appName,
                size = 56.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${stringResource(R.string.sheet_package_label)}: ${app.packageName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${stringResource(R.string.sheet_version_label)}: ${app.versionName} • ${
                        if (app.isSystemApp) stringResource(R.string.sheet_type_system)
                        else stringResource(R.string.sheet_type_user)
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (app.isSelfApp) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.warning_self_clone),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        if (app.isSensitiveBankingOrDrm && !app.isSelfApp) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.warning_banking_drm_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.warning_banking_drm_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        if (isLimitReached && !app.isSelfApp) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = stringResource(R.string.warning_limit_reached, cloneLimit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (item.cloneCount > 0) {
            OutlinedButton(
                onClick = onViewClonesClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.sheet_action_view_clones) + " (${item.cloneCount})")
            }
        }

        Button(
            onClick = onCloneClick,
            enabled = !app.isSelfApp && !isLimitReached,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("sheet_clone_button")
        ) {
            Icon(Icons.Rounded.ControlPointDuplicate, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.sheet_action_clone))
        }
    }
}

@Composable
fun EmptyOrErrorState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

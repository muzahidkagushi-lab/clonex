package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.CloneDatabase
import com.example.data.preferences.SettingsDataStore
import com.example.data.repository.CloneRepository
import com.example.data.repository.InstalledAppsRepository
import com.example.data.repository.NameValidationResult
import com.example.domain.model.AppFilter
import com.example.domain.model.AppSettings
import com.example.domain.model.AppSort
import com.example.domain.model.AutoLockTimeout
import com.example.domain.model.BadgeStyle
import com.example.domain.model.CloneItem
import com.example.domain.model.InstalledApp
import com.example.domain.model.LanguageOption
import com.example.domain.model.LockMethod
import com.example.domain.model.ThemeMode
import com.example.engine.LaunchCloneResult
import com.example.engine.MockCloneEngine
import com.example.util.Formatters
import com.example.util.LocaleHelper
import com.example.util.SecurityHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AppsUiState {
    data object Loading : AppsUiState()
    data class Success(val apps: List<InstalledApp>) : AppsUiState()
    data class Error(val message: String) : AppsUiState()
}

data class AppWithCloneCount(
    val app: InstalledApp,
    val cloneCount: Int,
    val lastCloneUsedAt: Long?
)

sealed class LockChallenge {
    data object AppUnlock : LockChallenge()
    data class CloneUnlock(val clone: CloneItem) : LockChallenge()
}

sealed class UiEvent {
    data class ShowMessage(val text: String, val isSuccessHaptic: Boolean = false) : UiEvent()
    data class ShowDeletedUndo(val clone: CloneItem, val message: String) : UiEvent()
    data class ShareText(val text: String, val subject: String) : UiEvent()
    data object RecreateForLocale : UiEvent()
}

class MainViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val settingsStore = SettingsDataStore(application)
    private val appsRepository = InstalledAppsRepository(application)
    private val cloneEngine = MockCloneEngine(application)
    private val cloneDao = CloneDatabase.getInstance(application).cloneDao()
    private val cloneRepository = CloneRepository(application, cloneDao, cloneEngine, appsRepository)

    val settingsOrNull: StateFlow<AppSettings?> = settingsStore.settingsFlow
        .map<AppSettings, AppSettings?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val settings: StateFlow<AppSettings> = settingsStore.settingsFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AppSettings(language = LanguageOption.fromCode(LocaleHelper.getSavedLanguageCodeSync(application)))
        )

    val clones: StateFlow<List<CloneItem>> = cloneRepository.clonesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSandboxClone: StateFlow<CloneItem?> = cloneRepository.activeSandboxClone

    private val _appsUiState = MutableStateFlow<AppsUiState>(AppsUiState.Loading)
    val appsUiState: StateFlow<AppsUiState> = _appsUiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow(savedStateHandle.get<String>(KEY_SEARCH) ?: "")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _appFilter = MutableStateFlow(
        runCatching { AppFilter.valueOf(savedStateHandle.get<String>(KEY_FILTER) ?: "") }
            .getOrDefault(AppFilter.ALL)
    )
    val appFilter: StateFlow<AppFilter> = _appFilter.asStateFlow()

    private val _appSort = MutableStateFlow(
        runCatching { AppSort.valueOf(savedStateHandle.get<String>(KEY_SORT) ?: "") }
            .getOrDefault(AppSort.A_Z)
    )
    val appSort: StateFlow<AppSort> = _appSort.asStateFlow()

    private val _selectedDetailCloneId = MutableStateFlow(savedStateHandle.get<String>(KEY_DETAIL_CLONE_ID))
    val selectedDetailCloneId: StateFlow<String?> = _selectedDetailCloneId.asStateFlow()

    private val _activeLockChallenge = MutableStateFlow<LockChallenge?>(null)
    val activeLockChallenge: StateFlow<LockChallenge?> = _activeLockChallenge.asStateFlow()

    private var lastBackgroundTimestamp: Long = 0L
    private var hasUnlockedAppSession: Boolean = false

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val filteredApps: StateFlow<List<AppWithCloneCount>> = combine(
        _appsUiState,
        clones,
        _searchQuery,
        _appFilter,
        _appSort
    ) { state, cloneList, query, filter, sort ->
        if (state !is AppsUiState.Success) return@combine emptyList()

        val clonesByPkg = cloneList.groupBy { it.originalPackageName }
        val q = query.trim().lowercase()

        val filtered = state.apps.filter { app ->
            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.USER -> !app.isSystemApp
                AppFilter.SYSTEM -> app.isSystemApp
            }
            val matchesQuery = q.isEmpty() ||
                app.appName.lowercase().contains(q) ||
                app.packageName.lowercase().contains(q)
            matchesFilter && matchesQuery
        }.map { app ->
            val pkgClones = clonesByPkg[app.packageName].orEmpty()
            AppWithCloneCount(
                app = app,
                cloneCount = pkgClones.size,
                lastCloneUsedAt = pkgClones.mapNotNull { it.lastUsedAt }.maxOrNull()
            )
        }

        when (sort) {
            AppSort.A_Z -> filtered.sortedBy { it.app.appName.lowercase() }
            AppSort.RECENTLY_USED -> filtered.sortedWith(
                compareByDescending<AppWithCloneCount> { it.lastCloneUsedAt ?: it.app.lastUpdateTime }
                    .thenBy { it.app.appName.lowercase() }
            )
            AppSort.MOST_CLONED -> filtered.sortedWith(
                compareByDescending<AppWithCloneCount> { it.cloneCount }
                    .thenBy { it.app.appName.lowercase() }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCloneStorageBytes: StateFlow<Long> = clones
        .map { list -> list.sumOf { it.storageBytes + it.cacheBytes } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    init {
        refreshInstalledApps(isInitial = true)
        viewModelScope.launch {
            val currentSettings = settingsStore.settingsFlow.first()
            if (currentSettings.appLockEnabled && currentSettings.onboardingCompleted) {
                _activeLockChallenge.value = LockChallenge.AppUnlock
            }
        }
    }

    private fun localizedContext(): Context {
        val lang = settings.value.language
        return LocaleHelper.wrapContext(getApplication(), lang)
    }

    fun onAppBackgrounded() {
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        val current = settings.value
        if (!current.appLockEnabled || !current.onboardingCompleted) return
        val elapsed = System.currentTimeMillis() - lastBackgroundTimestamp
        if (!hasUnlockedAppSession || (lastBackgroundTimestamp > 0L && elapsed >= current.autoLockTimeout.millis)) {
            _activeLockChallenge.value = LockChallenge.AppUnlock
        }
    }

    fun onLockSucceeded() {
        val challenge = _activeLockChallenge.value
        _activeLockChallenge.value = null
        when (challenge) {
            LockChallenge.AppUnlock -> hasUnlockedAppSession = true
            is LockChallenge.CloneUnlock -> performLaunchClone(challenge.clone)
            null -> Unit
        }
    }

    fun dismissCloneLockChallenge() {
        if (_activeLockChallenge.value is LockChallenge.CloneUnlock) {
            _activeLockChallenge.value = null
        }
    }

    fun refreshInstalledApps(isInitial: Boolean = false) {
        viewModelScope.launch {
            if (isInitial) {
                _appsUiState.value = AppsUiState.Loading
            } else {
                _isRefreshing.value = true
            }
            runCatching {
                appsRepository.loadInstalledApps(forceRefresh = !isInitial)
            }.onSuccess { list ->
                _appsUiState.value = AppsUiState.Success(list)
            }.onFailure { err ->
                _appsUiState.value = AppsUiState.Error(err.localizedMessage ?: "Failed to load apps")
            }
            _isRefreshing.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        savedStateHandle[KEY_SEARCH] = query
    }

    fun updateFilter(filter: AppFilter) {
        _appFilter.value = filter
        savedStateHandle[KEY_FILTER] = filter.name
    }

    fun updateSort(sort: AppSort) {
        _appSort.value = sort
        savedStateHandle[KEY_SORT] = sort.name
    }

    fun selectDetailClone(cloneId: String?) {
        _selectedDetailCloneId.value = cloneId
        savedStateHandle[KEY_DETAIL_CLONE_ID] = cloneId
    }

    fun observeClone(cloneId: String) = cloneRepository.observeCloneById(cloneId)

    fun getAppIcon(packageName: String): Drawable? = appsRepository.getAppIcon(packageName)

    suspend fun getGrantedPermissions(packageName: String): List<String> =
        appsRepository.getGrantedPermissionsForPackage(packageName)

    fun validateCloneName(candidate: String, excludeCloneId: String? = null): NameValidationResult =
        cloneRepository.validateCloneName(candidate, clones.value, excludeCloneId)

    fun isCloneLimitReached(): Boolean {
        val limit = settings.value.cloneLimit
        return limit > 0 && clones.value.size >= limit
    }

    fun completeOnboardingAndTerms() {
        viewModelScope.launch {
            settingsStore.setOnboardingCompleted(true)
            settingsStore.setTermsAccepted(true)
        }
    }

    fun acceptTermsOnly() {
        viewModelScope.launch {
            settingsStore.setTermsAccepted(true)
        }
    }

    fun createClone(
        app: InstalledApp,
        cloneName: String,
        badgeLabel: String,
        badgeColorHex: Long,
        notificationsEnabled: Boolean,
        isHiddenFromLauncher: Boolean,
        isLocked: Boolean
    ) {
        viewModelScope.launch {
            val created = cloneRepository.createClone(
                app = app,
                cloneName = cloneName,
                badgeLabel = badgeLabel,
                badgeColorHex = badgeColorHex,
                badgeStyle = settings.value.defaultBadgeStyle,
                notificationsEnabled = notificationsEnabled,
                isHiddenFromLauncher = isHiddenFromLauncher,
                isLocked = isLocked
            )
            val ctx = localizedContext()
            _events.emit(
                UiEvent.ShowMessage(
                    text = ctx.getString(R.string.clone_created_msg, created.cloneName),
                    isSuccessHaptic = true
                )
            )
        }
    }

    fun requestLaunchClone(clone: CloneItem) {
        if (clone.isLocked) {
            _activeLockChallenge.value = LockChallenge.CloneUnlock(clone)
        } else {
            performLaunchClone(clone)
        }
    }

    fun launchCloneById(cloneId: String) {
        viewModelScope.launch {
            val target = clones.value.firstOrNull { it.id == cloneId } ?: return@launch
            requestLaunchClone(target)
        }
    }

    private fun performLaunchClone(clone: CloneItem) {
        viewModelScope.launch {
            val ctx = localizedContext()
            when (val res = cloneRepository.launchClone(clone)) {
                is LaunchCloneResult.Success -> {
                    _events.emit(
                        UiEvent.ShowMessage(
                            ctx.getString(R.string.snackbar_launched, res.clone.cloneName),
                            isSuccessHaptic = true
                        )
                    )
                }
                is LaunchCloneResult.PackageMissing -> {
                    _events.emit(
                        UiEvent.ShowMessage(
                            ctx.getString(R.string.snackbar_launch_error, res.packageName)
                        )
                    )
                }
                is LaunchCloneResult.Error -> {
                    _events.emit(UiEvent.ShowMessage(res.message))
                }
            }
        }
    }

    fun openHostPackage(packageName: String) {
        val ctx = getApplication<Application>()
        val launchIntent = ctx.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (launchIntent != null) {
            ctx.startActivity(launchIntent)
        }
    }

    fun dismissActiveSandbox() {
        cloneRepository.dismissActiveSandbox()
    }

    fun stopClone(cloneId: String) {
        viewModelScope.launch {
            cloneRepository.stopClone(cloneId)
            _events.emit(UiEvent.ShowMessage(localizedContext().getString(R.string.snackbar_stopped)))
        }
    }

    fun stopClones(cloneIds: List<String>) {
        viewModelScope.launch {
            cloneRepository.stopClones(cloneIds)
            _events.emit(UiEvent.ShowMessage(localizedContext().getString(R.string.snackbar_stopped)))
        }
    }

    fun updateClone(clone: CloneItem, showToast: Boolean = true) {
        viewModelScope.launch {
            cloneRepository.updateClone(clone)
            if (showToast) {
                _events.emit(UiEvent.ShowMessage(localizedContext().getString(R.string.clone_updated_msg)))
            }
        }
    }

    fun deleteClone(clone: CloneItem) {
        viewModelScope.launch {
            cloneRepository.deleteClone(clone)
            if (_selectedDetailCloneId.value == clone.id) {
                selectDetailClone(null)
            }
            val msg = localizedContext().getString(R.string.snackbar_deleted, clone.cloneName)
            _events.emit(UiEvent.ShowDeletedUndo(clone = clone, message = msg))
        }
    }

    fun undoDeleteClone(clone: CloneItem) {
        viewModelScope.launch {
            cloneRepository.restoreClone(clone)
            _events.emit(
                UiEvent.ShowMessage(
                    localizedContext().getString(R.string.snackbar_restored, clone.cloneName)
                )
            )
        }
    }

    fun deleteClones(clonesToDelete: List<CloneItem>) {
        viewModelScope.launch {
            cloneRepository.deleteClones(clonesToDelete)
            if (_selectedDetailCloneId.value in clonesToDelete.map { it.id }) {
                selectDetailClone(null)
            }
            _events.emit(
                UiEvent.ShowMessage(
                    localizedContext().getString(R.string.snackbar_deleted, "${clonesToDelete.size}")
                )
            )
        }
    }

    fun clearCloneCache(clone: CloneItem) {
        viewModelScope.launch {
            val freed = cloneRepository.clearCloneCache(clone)
            _events.emit(
                UiEvent.ShowMessage(
                    localizedContext().getString(
                        R.string.snackbar_cache_cleared,
                        Formatters.formatBytes(freed)
                    )
                )
            )
        }
    }

    fun clearCloneData(clone: CloneItem) {
        viewModelScope.launch {
            cloneRepository.clearCloneData(clone)
            _events.emit(UiEvent.ShowMessage(localizedContext().getString(R.string.snackbar_data_cleared)))
        }
    }

    fun pinShortcut(clone: CloneItem) {
        viewModelScope.launch {
            val ok = cloneRepository.pinHomeShortcut(clone)
            val ctx = localizedContext()
            val msg = if (ok) {
                ctx.getString(R.string.snackbar_shortcut_requested, clone.cloneName)
            } else {
                ctx.getString(R.string.snackbar_shortcut_unsupported)
            }
            _events.emit(UiEvent.ShowMessage(msg, isSuccessHaptic = ok))
        }
    }

    fun exportSingleCloneConfig(clone: CloneItem) {
        val json = cloneRepository.exportSingleCloneToJson(clone)
        viewModelScope.launch {
            _events.emit(UiEvent.ShareText(text = json, subject = "${clone.cloneName}.json"))
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            val ctx = localizedContext()
            runCatching {
                val json = cloneRepository.exportClonesToJson()
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("Cannot open stream")
                clones.value.size
            }.onSuccess { count ->
                _events.emit(
                    UiEvent.ShowMessage(
                        ctx.getString(R.string.backup_export_success, count),
                        isSuccessHaptic = true
                    )
                )
            }.onFailure {
                _events.emit(UiEvent.ShowMessage(ctx.getString(R.string.backup_error)))
            }
        }
    }

    fun importBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            val ctx = localizedContext()
            runCatching {
                val json = getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().readText()
                } ?: error("Cannot open stream")
                cloneRepository.importClonesFromJson(json)
            }.onSuccess { count ->
                _events.emit(
                    UiEvent.ShowMessage(
                        ctx.getString(R.string.backup_import_success, count),
                        isSuccessHaptic = true
                    )
                )
            }.onFailure {
                _events.emit(UiEvent.ShowMessage(ctx.getString(R.string.backup_error)))
            }
        }
    }

    // Settings mutations
    fun setLanguage(language: LanguageOption) {
        viewModelScope.launch {
            val previous = settings.value.language
            settingsStore.setLanguage(language)
            if (previous != language) {
                _events.emit(UiEvent.RecreateForLocale)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setDynamicColor(enabled) }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAppLockEnabled(enabled) }
    }

    fun setLockMethod(method: LockMethod) {
        viewModelScope.launch { settingsStore.setLockMethod(method) }
    }

    fun savePin(pin: String) {
        viewModelScope.launch {
            settingsStore.setPinHash(SecurityHelper.hashSecret(pin))
            settingsStore.setLockMethod(LockMethod.PIN)
            settingsStore.setAppLockEnabled(true)
            hasUnlockedAppSession = true
            _events.emit(UiEvent.ShowMessage(localizedContext().getString(R.string.lock_saved), true))
        }
    }

    fun savePattern(pattern: String) {
        viewModelScope.launch {
            settingsStore.setPatternHash(SecurityHelper.hashSecret(pattern))
            settingsStore.setLockMethod(LockMethod.PATTERN)
            settingsStore.setAppLockEnabled(true)
            hasUnlockedAppSession = true
            _events.emit(UiEvent.ShowMessage(localizedContext().getString(R.string.lock_saved), true))
        }
    }

    fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        viewModelScope.launch { settingsStore.setAutoLockTimeout(timeout) }
    }

    fun setDefaultCloneLock(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setDefaultCloneLock(enabled) }
    }

    fun setMasterNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setMasterNotifications(enabled)
            // Apply across existing clones too for immediate unified control
            clones.value.forEach { clone ->
                if (clone.notificationsEnabled != enabled) {
                    cloneRepository.updateClone(clone.copy(notificationsEnabled = enabled))
                }
            }
        }
    }

    fun setCloneNameFormat(format: String) {
        val sanitized = format.trim().ifEmpty { "{name} {n}" }
        viewModelScope.launch { settingsStore.setCloneNameFormat(sanitized) }
    }

    fun setCloneLimit(limit: Int) {
        viewModelScope.launch { settingsStore.setCloneLimit(limit) }
    }

    fun setAutoStartOnBoot(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAutoStartOnBoot(enabled) }
    }

    fun setDefaultBadgeStyle(style: BadgeStyle) {
        viewModelScope.launch { settingsStore.setDefaultBadgeStyle(style) }
    }

    fun setLocalAnalytics(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setLocalAnalytics(enabled) }
    }

    fun clearAllCaches() {
        viewModelScope.launch {
            val freed = cloneRepository.clearAllCaches()
            _events.emit(
                UiEvent.ShowMessage(
                    localizedContext().getString(
                        R.string.snackbar_cache_cleared,
                        Formatters.formatBytes(freed)
                    )
                )
            )
        }
    }

    fun clearAllCloneData() {
        viewModelScope.launch {
            cloneRepository.clearAllData()
            _events.emit(UiEvent.ShowMessage(localizedContext().getString(R.string.snackbar_data_cleared)))
        }
    }

    fun resetEntireApp() {
        viewModelScope.launch {
            cloneRepository.deleteAllClonesAndReset()
            settingsStore.resetAllSettings()
            selectDetailClone(null)
            _events.emit(UiEvent.ShowMessage(localizedContext().getString(R.string.reset_done)))
        }
    }

    companion object {
        private const val KEY_SEARCH = "search_query"
        private const val KEY_FILTER = "app_filter"
        private const val KEY_SORT = "app_sort"
        private const val KEY_DETAIL_CLONE_ID = "detail_clone_id"
    }
}

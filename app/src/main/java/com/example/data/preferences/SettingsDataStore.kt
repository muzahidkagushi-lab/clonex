package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppSettings
import com.example.domain.model.AutoLockTimeout
import com.example.domain.model.BadgeStyle
import com.example.domain.model.LanguageOption
import com.example.domain.model.LockMethod
import com.example.domain.model.ThemeMode
import com.example.util.LocaleHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "clonex_settings")

/**
 * Manages user preferences in Jetpack DataStore.
 * Language changes are mirrored synchronously to [LocaleHelper] so cold starts apply the
 * selected locale cleanly inside MainActivity.attachBaseContext.
 */
class SettingsDataStore(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val TERMS_ACCEPTED = booleanPreferencesKey("terms_accepted")
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val LOCK_METHOD = stringPreferencesKey("lock_method")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PATTERN_HASH = stringPreferencesKey("pattern_hash")
        val AUTO_LOCK_TIMEOUT = stringPreferencesKey("auto_lock_timeout")
        val DEFAULT_CLONE_LOCK = booleanPreferencesKey("default_clone_lock")
        val MASTER_NOTIFICATIONS = booleanPreferencesKey("master_notifications")
        val CLONE_NAME_FORMAT = stringPreferencesKey("clone_name_format")
        val CLONE_LIMIT = intPreferencesKey("clone_limit")
        val AUTO_START_ON_BOOT = booleanPreferencesKey("auto_start_on_boot")
        val DEFAULT_BADGE_STYLE = stringPreferencesKey("default_badge_style")
        val LOCAL_ANALYTICS = booleanPreferencesKey("local_analytics")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            termsAccepted = prefs[Keys.TERMS_ACCEPTED] ?: false,
            language = LanguageOption.fromCode(prefs[Keys.LANGUAGE]),
            themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            lockMethod = LockMethod.fromKey(prefs[Keys.LOCK_METHOD]),
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            patternHash = prefs[Keys.PATTERN_HASH] ?: "",
            autoLockTimeout = AutoLockTimeout.fromKey(prefs[Keys.AUTO_LOCK_TIMEOUT]),
            defaultCloneLock = prefs[Keys.DEFAULT_CLONE_LOCK] ?: false,
            masterNotificationsEnabled = prefs[Keys.MASTER_NOTIFICATIONS] ?: true,
            cloneNameFormat = prefs[Keys.CLONE_NAME_FORMAT] ?: "{name} {n}",
            cloneLimit = prefs[Keys.CLONE_LIMIT] ?: 0,
            autoStartOnBoot = prefs[Keys.AUTO_START_ON_BOOT] ?: false,
            defaultBadgeStyle = BadgeStyle.fromKey(prefs[Keys.DEFAULT_BADGE_STYLE]),
            localAnalyticsEnabled = prefs[Keys.LOCAL_ANALYTICS] ?: false
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setTermsAccepted(accepted: Boolean) {
        context.dataStore.edit { it[Keys.TERMS_ACCEPTED] = accepted }
    }

    suspend fun setLanguage(language: LanguageOption) {
        LocaleHelper.saveLanguageCodeSync(context, language.code)
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.key }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setLockMethod(method: LockMethod) {
        context.dataStore.edit { it[Keys.LOCK_METHOD] = method.key }
    }

    suspend fun setPinHash(hash: String) {
        context.dataStore.edit { it[Keys.PIN_HASH] = hash }
    }

    suspend fun setPatternHash(hash: String) {
        context.dataStore.edit { it[Keys.PATTERN_HASH] = hash }
    }

    suspend fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        context.dataStore.edit { it[Keys.AUTO_LOCK_TIMEOUT] = timeout.key }
    }

    suspend fun setDefaultCloneLock(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEFAULT_CLONE_LOCK] = enabled }
    }

    suspend fun setMasterNotifications(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MASTER_NOTIFICATIONS] = enabled }
    }

    suspend fun setCloneNameFormat(format: String) {
        context.dataStore.edit { it[Keys.CLONE_NAME_FORMAT] = format }
    }

    suspend fun setCloneLimit(limit: Int) {
        context.dataStore.edit { it[Keys.CLONE_LIMIT] = limit.coerceAtLeast(0) }
    }

    suspend fun setAutoStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_START_ON_BOOT] = enabled }
    }

    suspend fun setDefaultBadgeStyle(style: BadgeStyle) {
        context.dataStore.edit { it[Keys.DEFAULT_BADGE_STYLE] = style.key }
    }

    suspend fun setLocalAnalytics(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOCAL_ANALYTICS] = enabled }
    }

    suspend fun resetAllSettings() {
        LocaleHelper.saveLanguageCodeSync(context, LanguageOption.SYSTEM.code)
        context.dataStore.edit { prefs ->
            prefs.clear()
            prefs[Keys.ONBOARDING_COMPLETED] = true
            prefs[Keys.TERMS_ACCEPTED] = true
        }
    }
}

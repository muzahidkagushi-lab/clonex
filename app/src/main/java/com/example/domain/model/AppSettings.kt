package com.example.domain.model

enum class LanguageOption(val code: String) {
    SYSTEM("system"),
    BENGALI("bn"),
    ENGLISH("en");

    companion object {
        fun fromCode(code: String?): LanguageOption =
            entries.firstOrNull { it.code == code } ?: SYSTEM
    }
}

enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class LockMethod(val key: String) {
    PIN("pin"),
    PATTERN("pattern"),
    BIOMETRIC("biometric");

    companion object {
        fun fromKey(key: String?): LockMethod =
            entries.firstOrNull { it.key == key } ?: PIN
    }
}

enum class AutoLockTimeout(val key: String, val millis: Long) {
    IMMEDIATE("immediate", 0L),
    SECONDS_30("30s", 30_000L),
    MINUTE_1("1m", 60_000L),
    MINUTES_5("5m", 300_000L);

    companion object {
        fun fromKey(key: String?): AutoLockTimeout =
            entries.firstOrNull { it.key == key } ?: IMMEDIATE
    }
}

enum class BadgeStyle(val key: String) {
    CORNER_PILL("corner"),
    CIRCLE_DOT("circle"),
    TOP_RIBBON("ribbon");

    companion object {
        fun fromKey(key: String?): BadgeStyle =
            entries.firstOrNull { it.key == key } ?: CORNER_PILL
    }
}

enum class AppFilter {
    ALL, USER, SYSTEM
}

enum class AppSort {
    A_Z, RECENTLY_USED, MOST_CLONED
}

/**
 * Holds all user-configurable CloneX settings persisted in Jetpack DataStore.
 */
data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val termsAccepted: Boolean = false,
    val language: LanguageOption = LanguageOption.SYSTEM,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val appLockEnabled: Boolean = false,
    val lockMethod: LockMethod = LockMethod.PIN,
    val pinHash: String = "",
    val patternHash: String = "",
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.IMMEDIATE,
    val defaultCloneLock: Boolean = false,
    val masterNotificationsEnabled: Boolean = true,
    val cloneNameFormat: String = "{name} {n}",
    val cloneLimit: Int = 0, // 0 means unlimited
    val autoStartOnBoot: Boolean = false,
    val defaultBadgeStyle: BadgeStyle = BadgeStyle.CORNER_PILL,
    val localAnalyticsEnabled: Boolean = false
)

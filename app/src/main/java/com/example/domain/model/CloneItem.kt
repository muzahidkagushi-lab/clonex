package com.example.domain.model

/**
 * Represents a configured app clone stored locally in Room and managed by CloneEngine.
 */
data class CloneItem(
    val id: String,
    val originalPackageName: String,
    val originalAppName: String,
    val cloneName: String,
    val badgeLabel: String,
    val badgeColorHex: Long,
    val badgeStyle: BadgeStyle = BadgeStyle.CORNER_PILL,
    val dataDirPath: String,
    val createdAt: Long,
    val lastUsedAt: Long?,
    val isRunning: Boolean = false,
    val storageBytes: Long = 0L,
    val cacheBytes: Long = 0L,
    val notificationsEnabled: Boolean = true,
    val isHiddenFromLauncher: Boolean = false,
    val isLocked: Boolean = false
)

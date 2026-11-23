package com.example.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupPayload(
    val schemaVersion: Int = 1,
    val exportedAt: Long,
    val appVersion: String = "1.0",
    val clones: List<CloneBackupEntry>
)

@JsonClass(generateAdapter = true)
data class CloneBackupEntry(
    val id: String,
    val originalPackageName: String,
    val originalAppName: String,
    val cloneName: String,
    val badgeLabel: String,
    val badgeColorHex: Long,
    val badgeStyle: String,
    val createdAt: Long,
    val lastUsedAt: Long?,
    val notificationsEnabled: Boolean,
    val isHiddenFromLauncher: Boolean,
    val isLocked: Boolean
)

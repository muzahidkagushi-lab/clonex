package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.BadgeStyle
import com.example.domain.model.CloneItem

/**
 * Room entity persisting metadata for an isolated app clone.
 */
@Entity(tableName = "clones")
data class CloneEntity(
    @PrimaryKey val id: String,
    val originalPackageName: String,
    val originalAppName: String,
    val cloneName: String,
    val badgeLabel: String,
    val badgeColorHex: Long,
    val badgeStyle: String,
    val dataDirPath: String,
    val createdAt: Long,
    val lastUsedAt: Long?,
    val storageBytes: Long,
    val cacheBytes: Long,
    val notificationsEnabled: Boolean,
    val isHiddenFromLauncher: Boolean,
    val isLocked: Boolean
) {
    fun toDomain(isRunning: Boolean = false): CloneItem = CloneItem(
        id = id,
        originalPackageName = originalPackageName,
        originalAppName = originalAppName,
        cloneName = cloneName,
        badgeLabel = badgeLabel,
        badgeColorHex = badgeColorHex,
        badgeStyle = BadgeStyle.fromKey(badgeStyle),
        dataDirPath = dataDirPath,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
        isRunning = isRunning,
        storageBytes = storageBytes,
        cacheBytes = cacheBytes,
        notificationsEnabled = notificationsEnabled,
        isHiddenFromLauncher = isHiddenFromLauncher,
        isLocked = isLocked
    )

    companion object {
        fun fromDomain(item: CloneItem): CloneEntity = CloneEntity(
            id = item.id,
            originalPackageName = item.originalPackageName,
            originalAppName = item.originalAppName,
            cloneName = item.cloneName,
            badgeLabel = item.badgeLabel,
            badgeColorHex = item.badgeColorHex,
            badgeStyle = item.badgeStyle.key,
            dataDirPath = item.dataDirPath,
            createdAt = item.createdAt,
            lastUsedAt = item.lastUsedAt,
            storageBytes = item.storageBytes,
            cacheBytes = item.cacheBytes,
            notificationsEnabled = item.notificationsEnabled,
            isHiddenFromLauncher = item.isHiddenFromLauncher,
            isLocked = item.isLocked
        )
    }
}

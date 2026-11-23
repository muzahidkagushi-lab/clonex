package com.example.data.repository

import android.content.Context
import android.graphics.drawable.Drawable
import com.example.data.local.CloneDao
import com.example.data.local.CloneEntity
import com.example.domain.model.BackupPayload
import com.example.domain.model.BadgeStyle
import com.example.domain.model.CloneBackupEntry
import com.example.domain.model.CloneItem
import com.example.domain.model.InstalledApp
import com.example.engine.CloneEngine
import com.example.engine.LaunchCloneResult
import com.example.util.ShortcutHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.util.UUID

sealed class NameValidationResult {
    data object Valid : NameValidationResult()
    data object Empty : NameValidationResult()
    data object TooLong : NameValidationResult()
    data object Duplicate : NameValidationResult()
}

/**
 * Coordinates Room persistence, CloneEngine lifecycle, ShortcutHelper, and JSON Backup/Restore.
 */
class CloneRepository(
    private val context: Context,
    private val cloneDao: CloneDao,
    private val cloneEngine: CloneEngine,
    private val appsRepository: InstalledAppsRepository
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val backupAdapter = moshi.adapter(BackupPayload::class.java).indent("  ")
    private val singleCloneAdapter = moshi.adapter(CloneBackupEntry::class.java).indent("  ")

    val clonesFlow: Flow<List<CloneItem>> = combine(
        cloneDao.observeAllClones(),
        cloneEngine.runningCloneIds
    ) { entities, runningIds ->
        entities.map { entity ->
            entity.toDomain(isRunning = entity.id in runningIds)
        }
    }

    val activeSandboxClone = cloneEngine.activeSandboxClone

    fun observeCloneById(cloneId: String): Flow<CloneItem?> = combine(
        cloneDao.observeCloneById(cloneId),
        cloneEngine.runningCloneIds
    ) { entity, runningIds ->
        entity?.toDomain(isRunning = entity.id in runningIds)
    }

    fun validateCloneName(
        candidate: String,
        existingClones: List<CloneItem>,
        excludeCloneId: String? = null
    ): NameValidationResult {
        val trimmed = candidate.trim()
        if (trimmed.isEmpty()) return NameValidationResult.Empty
        if (trimmed.length > 30) return NameValidationResult.TooLong
        val isDup = existingClones.any {
            it.id != excludeCloneId && it.cloneName.equals(trimmed, ignoreCase = true)
        }
        return if (isDup) NameValidationResult.Duplicate else NameValidationResult.Valid
    }

    suspend fun createClone(
        app: InstalledApp,
        cloneName: String,
        badgeLabel: String,
        badgeColorHex: Long,
        badgeStyle: BadgeStyle,
        notificationsEnabled: Boolean,
        isHiddenFromLauncher: Boolean,
        isLocked: Boolean
    ): CloneItem {
        val cloneId = UUID.randomUUID().toString()
        val created = cloneEngine.createClone(
            cloneId = cloneId,
            originalPackageName = app.packageName,
            originalAppName = app.appName,
            cloneName = cloneName.trim(),
            badgeLabel = badgeLabel.trim().ifEmpty { "2" },
            badgeColorHex = badgeColorHex,
            badgeStyle = badgeStyle,
            notificationsEnabled = notificationsEnabled,
            isHiddenFromLauncher = isHiddenFromLauncher,
            isLocked = isLocked
        )
        cloneDao.insertClone(CloneEntity.fromDomain(created))
        return created
    }

    suspend fun launchClone(clone: CloneItem): LaunchCloneResult {
        val result = cloneEngine.launchClone(clone)
        if (result is LaunchCloneResult.Success) {
            cloneDao.updateClone(CloneEntity.fromDomain(result.clone))
        }
        return result
    }

    suspend fun stopClone(cloneId: String) {
        cloneEngine.stopClone(cloneId)
    }

    suspend fun stopClones(cloneIds: List<String>) {
        cloneIds.forEach { cloneEngine.stopClone(it) }
    }

    suspend fun updateClone(clone: CloneItem) {
        cloneDao.updateClone(CloneEntity.fromDomain(clone))
    }

    suspend fun deleteClone(clone: CloneItem) {
        ShortcutHelper.disableCloneShortcut(context, clone.id)
        cloneEngine.deleteClone(clone.id)
        cloneDao.deleteCloneById(clone.id)
    }

    suspend fun deleteClones(clones: List<CloneItem>) {
        clones.forEach { clone ->
            ShortcutHelper.disableCloneShortcut(context, clone.id)
            cloneEngine.deleteClone(clone.id)
        }
        cloneDao.deleteClonesByIds(clones.map { it.id })
    }

    suspend fun restoreClone(clone: CloneItem) {
        cloneEngine.ensureSandboxInitialized(clone)
        val usage = cloneEngine.getStorageUsage(clone.id)
        val restored = clone.copy(
            isRunning = false,
            storageBytes = usage.dataBytes,
            cacheBytes = usage.cacheBytes
        )
        cloneDao.insertClone(CloneEntity.fromDomain(restored))
    }

    suspend fun clearCloneCache(clone: CloneItem): Long {
        val freed = cloneEngine.clearCache(clone.id)
        val usage = cloneEngine.getStorageUsage(clone.id)
        cloneDao.updateClone(
            CloneEntity.fromDomain(
                clone.copy(storageBytes = usage.dataBytes, cacheBytes = usage.cacheBytes)
            )
        )
        return freed
    }

    suspend fun clearCloneData(clone: CloneItem) {
        cloneEngine.clearData(clone.id)
        val usage = cloneEngine.getStorageUsage(clone.id)
        cloneDao.updateClone(
            CloneEntity.fromDomain(
                clone.copy(
                    isRunning = false,
                    storageBytes = usage.dataBytes,
                    cacheBytes = usage.cacheBytes
                )
            )
        )
    }

    suspend fun clearAllCaches(): Long {
        val clones = cloneDao.getAllClonesOnce().map { it.toDomain() }
        var totalFreed = 0L
        clones.forEach { clone ->
            totalFreed += clearCloneCache(clone)
        }
        return totalFreed
    }

    suspend fun clearAllData() {
        val clones = cloneDao.getAllClonesOnce().map { it.toDomain() }
        clones.forEach { clearCloneData(it) }
    }

    suspend fun deleteAllClonesAndReset() {
        val clones = cloneDao.getAllClonesOnce().map { it.toDomain() }
        clones.forEach { clone ->
            ShortcutHelper.disableCloneShortcut(context, clone.id)
            cloneEngine.deleteClone(clone.id)
        }
        cloneDao.deleteAllClones()
    }

    fun pinHomeShortcut(clone: CloneItem): Boolean {
        val iconDrawable: Drawable? = appsRepository.getAppIcon(clone.originalPackageName)
        return ShortcutHelper.pinCloneShortcut(context, clone, iconDrawable)
    }

    fun dismissActiveSandbox() {
        cloneEngine.dismissActiveSandbox()
    }

    suspend fun exportClonesToJson(): String = withContext(Dispatchers.IO) {
        val entries = cloneDao.getAllClonesOnce().map { entity ->
            CloneBackupEntry(
                id = entity.id,
                originalPackageName = entity.originalPackageName,
                originalAppName = entity.originalAppName,
                cloneName = entity.cloneName,
                badgeLabel = entity.badgeLabel,
                badgeColorHex = entity.badgeColorHex,
                badgeStyle = entity.badgeStyle,
                createdAt = entity.createdAt,
                lastUsedAt = entity.lastUsedAt,
                notificationsEnabled = entity.notificationsEnabled,
                isHiddenFromLauncher = entity.isHiddenFromLauncher,
                isLocked = entity.isLocked
            )
        }
        backupAdapter.toJson(
            BackupPayload(
                exportedAt = System.currentTimeMillis(),
                clones = entries
            )
        )
    }

    fun exportSingleCloneToJson(clone: CloneItem): String {
        val entry = CloneBackupEntry(
            id = clone.id,
            originalPackageName = clone.originalPackageName,
            originalAppName = clone.originalAppName,
            cloneName = clone.cloneName,
            badgeLabel = clone.badgeLabel,
            badgeColorHex = clone.badgeColorHex,
            badgeStyle = clone.badgeStyle.key,
            createdAt = clone.createdAt,
            lastUsedAt = clone.lastUsedAt,
            notificationsEnabled = clone.notificationsEnabled,
            isHiddenFromLauncher = clone.isHiddenFromLauncher,
            isLocked = clone.isLocked
        )
        return singleCloneAdapter.toJson(entry)
    }

    suspend fun importClonesFromJson(json: String): Int = withContext(Dispatchers.IO) {
        val payload = backupAdapter.fromJson(json) ?: return@withContext 0
        val existing = cloneDao.getAllClonesOnce()
        val existingNames = existing.map { it.cloneName.lowercase() }.toMutableSet()
        var importedCount = 0

        for (entry in payload.clones) {
            var candidateName = entry.cloneName.trim().take(30).ifEmpty { entry.originalAppName.take(26) + " 2" }
            var suffix = 2
            while (candidateName.lowercase() in existingNames) {
                val base = entry.cloneName.trim().take(25)
                candidateName = "$base $suffix".take(30)
                suffix++
            }
            existingNames.add(candidateName.lowercase())

            val newId = if (existing.any { it.id == entry.id }) UUID.randomUUID().toString() else entry.id
            val tempItem = CloneItem(
                id = newId,
                originalPackageName = entry.originalPackageName,
                originalAppName = entry.originalAppName,
                cloneName = candidateName,
                badgeLabel = entry.badgeLabel.take(4).ifEmpty { "2" },
                badgeColorHex = entry.badgeColorHex,
                badgeStyle = BadgeStyle.fromKey(entry.badgeStyle),
                dataDirPath = "",
                createdAt = entry.createdAt,
                lastUsedAt = entry.lastUsedAt,
                notificationsEnabled = entry.notificationsEnabled,
                isHiddenFromLauncher = entry.isHiddenFromLauncher,
                isLocked = entry.isLocked
            )
            cloneEngine.ensureSandboxInitialized(tempItem)
            val usage = cloneEngine.getStorageUsage(newId)
            val rootPath = context.filesDir.resolve("clones/$newId").absolutePath
            cloneDao.insertClone(
                CloneEntity.fromDomain(
                    tempItem.copy(
                        dataDirPath = rootPath,
                        storageBytes = usage.dataBytes,
                        cacheBytes = usage.cacheBytes
                    )
                )
            )
            importedCount++
        }
        importedCount
    }
}

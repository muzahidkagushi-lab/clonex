package com.example.engine

import android.content.Context
import android.content.pm.PackageManager
import com.example.domain.model.BadgeStyle
import com.example.domain.model.CloneItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Default on-device sandbox & simulation implementation of [CloneEngine].
 *
 * It allocates real per-UUID isolated directories under `filesDir/clones/<cloneId>/`
 * (`data/` and `cache/`), persists sandbox metadata files, calculates real byte usage,
 * and tracks running session states reactively.
 *
 * TODO (Virtualization Hook):
 *  1. In [createClone]: Install/register the virtualized APK inside your virtualization SDK
 *     (e.g., BlackBoxCore.get().installPackageAsUser(packageName, userId)).
 *  2. In [launchClone]: Call the virtualization SDK launcher
 *     (e.g., BlackBoxCore.get().launchApk(clone.originalPackageName, mappedUserId))
 *     and remove the in-app sandbox preview sheet if the virtualized Activity takes foreground.
 *  3. In [stopClone]: Call BlackBoxCore.get().stopPackage(packageName, mappedUserId).
 *  4. In [deleteClone] / [clearData]: Delegate user-space deletion to the virtualization SDK
 *     alongside cleaning up [sandboxRootDir].
 */
class MockCloneEngine(
    private val context: Context
) : CloneEngine {

    private val clonesBaseDir: File
        get() = File(context.filesDir, "clones").apply { if (!exists()) mkdirs() }

    private val _runningCloneIds = MutableStateFlow<Set<String>>(emptySet())
    override val runningCloneIds: StateFlow<Set<String>> = _runningCloneIds.asStateFlow()

    private val _activeSandboxClone = MutableStateFlow<CloneItem?>(null)
    override val activeSandboxClone: StateFlow<CloneItem?> = _activeSandboxClone.asStateFlow()

    private fun cloneRoot(cloneId: String): File = File(clonesBaseDir, cloneId)
    private fun cloneDataDir(cloneId: String): File = File(cloneRoot(cloneId), "data")
    private fun cloneCacheDir(cloneId: String): File = File(cloneRoot(cloneId), "cache")

    override suspend fun createClone(
        cloneId: String,
        originalPackageName: String,
        originalAppName: String,
        cloneName: String,
        badgeLabel: String,
        badgeColorHex: Long,
        badgeStyle: BadgeStyle,
        notificationsEnabled: Boolean,
        isHiddenFromLauncher: Boolean,
        isLocked: Boolean
    ): CloneItem = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val root = cloneRoot(cloneId)
        val dataDir = cloneDataDir(cloneId)
        val cacheDir = cloneCacheDir(cloneId)
        dataDir.mkdirs()
        cacheDir.mkdirs()

        // TODO: Register virtualized user container in real CloneEngine SDK here.
        // Seed initial isolated container manifest & session stub so disk usage is authentic.
        File(dataDir, "sandbox_manifest.json").writeText(
            """{"cloneId":"$cloneId","package":"$originalPackageName","createdAt":$now}"""
        )
        File(dataDir, "isolated_prefs.dat").writeBytes(ByteArray(2048) { 0x42 })
        File(cacheDir, "boot_cache.tmp").writeBytes(ByteArray(1024) { 0x1A })

        val usage = computeDirUsage(cloneId)
        CloneItem(
            id = cloneId,
            originalPackageName = originalPackageName,
            originalAppName = originalAppName,
            cloneName = cloneName,
            badgeLabel = badgeLabel,
            badgeColorHex = badgeColorHex,
            badgeStyle = badgeStyle,
            dataDirPath = root.absolutePath,
            createdAt = now,
            lastUsedAt = null,
            isRunning = false,
            storageBytes = usage.dataBytes,
            cacheBytes = usage.cacheBytes,
            notificationsEnabled = notificationsEnabled,
            isHiddenFromLauncher = isHiddenFromLauncher,
            isLocked = isLocked
        )
    }

    override suspend fun launchClone(clone: CloneItem): LaunchCloneResult = withContext(Dispatchers.IO) {
        // Verify original package is still installed on the device
        val isInstalled = try {
            context.packageManager.getPackageInfo(clone.originalPackageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

        if (!isInstalled) {
            return@withContext LaunchCloneResult.PackageMissing(clone.originalPackageName)
        }

        val dataDir = cloneDataDir(clone.id).apply { mkdirs() }
        val cacheDir = cloneCacheDir(clone.id).apply { mkdirs() }

        // TODO: Invoke real virtualization launcher here, e.g.:
        // BlackBoxCore.get().launchApk(clone.originalPackageName, userSpaceId)

        // Update isolated session & cache files to reflect an active isolated run
        val now = System.currentTimeMillis()
        File(dataDir, "session_state.json").writeText(
            """{"active":true,"cloneId":"${clone.id}","lastLaunch":$now}"""
        )
        File(cacheDir, "runtime_cache_${now % 10}.tmp").writeBytes(ByteArray(4096) { 0x2C })

        _runningCloneIds.update { it + clone.id }
        val usage = computeDirUsage(clone.id)
        val launchedClone = clone.copy(
            isRunning = true,
            lastUsedAt = now,
            storageBytes = usage.dataBytes,
            cacheBytes = usage.cacheBytes
        )
        _activeSandboxClone.value = launchedClone
        LaunchCloneResult.Success(launchedClone)
    }

    override suspend fun stopClone(cloneId: String) = withContext(Dispatchers.IO) {
        // TODO: Call real virtualization stopPackage(packageName, userSpaceId) here.
        _runningCloneIds.update { it - cloneId }
        if (_activeSandboxClone.value?.id == cloneId) {
            _activeSandboxClone.value = null
        }
        val sessionFile = File(cloneDataDir(cloneId), "session_state.json")
        if (sessionFile.exists()) {
            sessionFile.writeText("""{"active":false,"cloneId":"$cloneId"}""")
        }
    }

    override suspend fun deleteClone(cloneId: String) = withContext(Dispatchers.IO) {
        stopClone(cloneId)
        // TODO: Uninstall virtualized user space in real CloneEngine SDK here.
        cloneRoot(cloneId).deleteRecursively()
        Unit
    }

    override suspend fun clearData(cloneId: String) = withContext(Dispatchers.IO) {
        stopClone(cloneId)
        val dataDir = cloneDataDir(cloneId)
        dataDir.deleteRecursively()
        dataDir.mkdirs()
        File(dataDir, "sandbox_manifest.json").writeText(
            """{"cloneId":"$cloneId","resetAt":${System.currentTimeMillis()}}"""
        )
        Unit
    }

    override suspend fun clearCache(cloneId: String): Long = withContext(Dispatchers.IO) {
        val cacheDir = cloneCacheDir(cloneId)
        val freed = directorySize(cacheDir)
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
        freed
    }

    override suspend fun getStorageUsage(cloneId: String): CloneStorageUsage =
        withContext(Dispatchers.IO) {
            computeDirUsage(cloneId)
        }

    override suspend fun ensureSandboxInitialized(clone: CloneItem) = withContext(Dispatchers.IO) {
        val dataDir = cloneDataDir(clone.id)
        val cacheDir = cloneCacheDir(clone.id)
        if (!dataDir.exists()) dataDir.mkdirs()
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val manifest = File(dataDir, "sandbox_manifest.json")
        if (!manifest.exists()) {
            manifest.writeText(
                """{"cloneId":"${clone.id}","package":"${clone.originalPackageName}","restoredAt":${System.currentTimeMillis()}}"""
            )
        }
        Unit
    }

    override fun dismissActiveSandbox() {
        _activeSandboxClone.value = null
    }

    override suspend fun onBootCompleted(clonesToStart: List<CloneItem>) = withContext(Dispatchers.IO) {
        // TODO: Start persisted virtualized background services for clonesToStart.
        val validIds = clonesToStart.map { it.id }.toSet()
        _runningCloneIds.update { it + validIds }
    }

    private fun computeDirUsage(cloneId: String): CloneStorageUsage {
        val dataBytes = directorySize(cloneDataDir(cloneId))
        val cacheBytes = directorySize(cloneCacheDir(cloneId))
        return CloneStorageUsage(dataBytes = dataBytes, cacheBytes = cacheBytes)
    }

    private fun directorySize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { directorySize(it) } ?: 0L
    }
}

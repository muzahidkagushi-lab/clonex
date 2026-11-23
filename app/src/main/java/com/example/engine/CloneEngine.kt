package com.example.engine

import com.example.domain.model.BadgeStyle
import com.example.domain.model.CloneItem
import kotlinx.coroutines.flow.StateFlow

/**
 * Storage breakdown in bytes for a single isolated clone sandbox.
 */
data class CloneStorageUsage(
    val dataBytes: Long,
    val cacheBytes: Long
) {
    val totalBytes: Long get() = dataBytes + cacheBytes
}

/**
 * Result emitted when launching a clone inside CloneEngine.
 */
sealed class LaunchCloneResult {
    data class Success(val clone: CloneItem) : LaunchCloneResult()
    data class PackageMissing(val packageName: String) : LaunchCloneResult()
    data class Error(val message: String) : LaunchCloneResult()
}

/**
 * Clean abstraction for app virtualization and sandbox lifecycle management.
 *
 * Replace [MockCloneEngine] with a concrete implementation backed by an Android
 * app-virtualization engine (e.g., BlackBox, VirtualApp, SandHook, or Work Profile APIs)
 * without modifying the UI or Repository layers.
 */
interface CloneEngine {

    /** Observable set of clone UUIDs currently marked as running. */
    val runningCloneIds: StateFlow<Set<String>>

    /** Currently foregrounded sandbox session, if any. */
    val activeSandboxClone: StateFlow<CloneItem?>

    /**
     * Provisions an isolated data and cache directory for a new clone.
     */
    suspend fun createClone(
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
    ): CloneItem

    /**
     * Starts an isolated instance of the cloned application.
     */
    suspend fun launchClone(clone: CloneItem): LaunchCloneResult

    /**
     * Force-stops a running clone process/container.
     */
    suspend fun stopClone(cloneId: String)

    /**
     * Permanently removes the clone's isolated sandbox directory and stops it if running.
     */
    suspend fun deleteClone(cloneId: String)

    /**
     * Wipes isolated user data (logins, databases, shared preferences) inside the clone.
     */
    suspend fun clearData(cloneId: String)

    /**
     * Wipes only temporary cache files inside the clone, returning bytes freed.
     */
    suspend fun clearCache(cloneId: String): Long

    /**
     * Computes actual disk usage (data + cache) for the clone's isolated directory.
     */
    suspend fun getStorageUsage(cloneId: String): CloneStorageUsage

    /**
     * Restores isolated sandbox directories if needed (e.g., after Undo delete or backup import).
     */
    suspend fun ensureSandboxInitialized(clone: CloneItem)

    /**
     * Dismisses the active foreground sandbox preview overlay.
     */
    fun dismissActiveSandbox()

    /**
     * Called on device boot when auto-start is enabled.
     */
    suspend fun onBootCompleted(clonesToStart: List<CloneItem>)
}

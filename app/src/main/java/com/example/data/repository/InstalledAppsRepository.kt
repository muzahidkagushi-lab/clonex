package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.example.domain.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Discovers installed launchable apps on the device via Android's PackageManager
 * using the `<queries>` launcher intent filter declared in AndroidManifest.xml.
 */
class InstalledAppsRepository(private val context: Context) {

    private val iconCache = ConcurrentHashMap<String, Drawable>()

    private val sensitiveKeywords = listOf(
        "bank", "pay", "wallet", "upi", "bkash", "nagad", "rocket", "upay",
        "gpay", "paypal", "stripe", "venmo", "cash", "finance", "credit",
        "netflix", "primevideo", "disney", "hotstar", "drm", "widevine",
        "play.games", "authenticator", "authy"
    )

    suspend fun loadInstalledApps(forceRefresh: Boolean = false): List<InstalledApp> =
        withContext(Dispatchers.IO) {
            if (forceRefresh) {
                iconCache.clear()
            }
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    launcherIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            }

            val seenPackages = HashSet<String>()
            val apps = mutableListOf<InstalledApp>()

            for (resolveInfo in resolveInfos) {
                val activityInfo = resolveInfo.activityInfo ?: continue
                val pkgName = activityInfo.packageName ?: continue
                if (!seenPackages.add(pkgName)) continue

                val appInfo = activityInfo.applicationInfo
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

                val label = resolveInfo.loadLabel(pm)?.toString()?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: appInfo.loadLabel(pm)?.toString()?.trim()
                    ?: pkgName

                val pkgInfo = getPackageInfoSafe(pm, pkgName)
                val versionName = pkgInfo?.versionName ?: "1.0"
                val lastUpdate = pkgInfo?.lastUpdateTime ?: 0L
                val permissions = extractGrantedPermissions(pkgInfo)

                val icon = iconCache.getOrPut(pkgName) {
                    runCatching { resolveInfo.loadIcon(pm) }
                        .getOrNull() ?: pm.defaultActivityIcon
                }

                val isSensitive = sensitiveKeywords.any { kw ->
                    pkgName.contains(kw, ignoreCase = true) ||
                        label.contains(kw, ignoreCase = true)
                }

                apps.add(
                    InstalledApp(
                        packageName = pkgName,
                        appName = label,
                        versionName = versionName,
                        isSystemApp = isSystem,
                        lastUpdateTime = lastUpdate,
                        iconDrawable = icon,
                        isSensitiveBankingOrDrm = isSensitive,
                        isSelfApp = pkgName == context.packageName,
                        grantedPermissions = permissions
                    )
                )
            }

            apps.sortedBy { it.appName.lowercase() }
        }

    fun getAppIcon(packageName: String): Drawable? {
        iconCache[packageName]?.let { return it }
        return runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            iconCache[packageName] = drawable
            drawable
        }.getOrNull()
    }

    suspend fun getGrantedPermissionsForPackage(packageName: String): List<String> =
        withContext(Dispatchers.IO) {
            val pkgInfo = getPackageInfoSafe(context.packageManager, packageName)
            extractGrantedPermissions(pkgInfo)
        }

    private fun getPackageInfoSafe(pm: PackageManager, packageName: String): PackageInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }
        }.getOrNull()
    }

    private fun extractGrantedPermissions(pkgInfo: PackageInfo?): List<String> {
        val requested = pkgInfo?.requestedPermissions ?: return emptyList()
        val flags = pkgInfo.requestedPermissionsFlags ?: return emptyList()
        val granted = mutableListOf<String>()
        for (i in requested.indices) {
            if (i < flags.size && (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                val shortName = requested[i].substringAfterLast('.')
                granted.add(shortName)
            }
        }
        return granted.distinct().sorted()
    }
}

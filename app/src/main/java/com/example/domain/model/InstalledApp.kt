package com.example.domain.model

import android.graphics.drawable.Drawable

/**
 * Represents an installed application discovered on the device via PackageManager.
 */
data class InstalledApp(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val lastUpdateTime: Long,
    val iconDrawable: Drawable? = null,
    val isSensitiveBankingOrDrm: Boolean = false,
    val isSelfApp: Boolean = false,
    val grantedPermissions: List<String> = emptyList()
)

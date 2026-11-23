package com.example.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.CloneDatabase
import com.example.data.preferences.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Restores clones on device boot when the user has enabled "Auto-Start Clones on Boot" in Settings.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsDataStore(appContext).settingsFlow.first()
                if (settings.autoStartOnBoot) {
                    val dao = CloneDatabase.getInstance(appContext).cloneDao()
                    val clones = dao.getAllClonesOnce().map { it.toDomain() }
                    val engine = MockCloneEngine(appContext)
                    engine.onBootCompleted(clones)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

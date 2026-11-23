package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.LanguageOption
import com.example.ui.navigation.CloneXAppRoot
import com.example.ui.theme.CloneXTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.util.LocaleHelper
import com.example.util.ShortcutHelper

/**
 * Single-activity host for CloneX – App Cloner.
 * Applies explicit locale overrides cleanly via applyOverrideConfiguration (preserving the
 * genuine Activity context for Material 3 sheets, dialogs, and Dynamic Color), handles edge-to-edge
 * insets, auto-lock lifecycle hooks, and pinned home-screen shortcut launches.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        val savedOption = LanguageOption.fromCode(LocaleHelper.getSavedLanguageCodeSync(newBase))
        super.attachBaseContext(newBase)
        if (savedOption != LanguageOption.SYSTEM) {
            applyOverrideConfiguration(LocaleHelper.buildOverrideConfiguration(newBase, savedOption))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShortcutIntent(intent)

        setContent {
            val loadedSettings by viewModel.settingsOrNull.collectAsStateWithLifecycle()
            val settings = loadedSettings
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> viewModel.onAppBackgrounded()
                        Lifecycle.Event.ON_START -> viewModel.onAppForegrounded()
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val isBengali = remember(settings?.language) {
                LocaleHelper.isBengaliActive(settings?.language ?: LanguageOption.SYSTEM)
            }

            CloneXTheme(
                themeMode = settings?.themeMode ?: com.example.domain.model.ThemeMode.SYSTEM,
                dynamicColor = settings?.dynamicColor ?: true,
                isBengali = isBengali
            ) {
                if (settings == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                } else {
                    CloneXAppRoot(
                        viewModel = viewModel,
                        isBengali = isBengali,
                        onRecreateForLocale = { recreate() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShortcutIntent(intent)
    }

    private fun handleShortcutIntent(intent: Intent?) {
        val cloneId = intent?.getStringExtra(ShortcutHelper.EXTRA_CLONE_ID) ?: return
        intent.removeExtra(ShortcutHelper.EXTRA_CLONE_ID)
        viewModel.launchCloneById(cloneId)
    }
}

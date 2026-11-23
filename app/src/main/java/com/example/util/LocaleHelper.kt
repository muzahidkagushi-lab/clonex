package com.example.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import com.example.domain.model.LanguageOption
import java.util.Locale

/**
 * Handles runtime and cold-start locale resolution for English, Bengali, and System Default.
 */
object LocaleHelper {

    private const val PREF_NAME = "clonex_locale_sync"
    private const val KEY_LANG = "lang_code"

    fun saveLanguageCodeSync(context: Context, code: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, code)
            .apply()
    }

    fun getSavedLanguageCodeSync(context: Context): String {
        return runCatching {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANG, LanguageOption.SYSTEM.code)
        }.getOrNull() ?: LanguageOption.SYSTEM.code
    }

    fun resolveLocale(option: LanguageOption): Locale {
        return when (option) {
            LanguageOption.BENGALI -> Locale.forLanguageTag("bn")
            LanguageOption.ENGLISH -> Locale.ENGLISH
            LanguageOption.SYSTEM -> {
                val sysLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Resources.getSystem().configuration.locales.get(0)
                } else {
                    @Suppress("DEPRECATION")
                    Resources.getSystem().configuration.locale
                }
                sysLocale ?: Locale.getDefault()
            }
        }
    }

    fun buildOverrideConfiguration(base: Context, option: LanguageOption): Configuration {
        val locale = resolveLocale(option)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        config.setLayoutDirection(locale)
        return config
    }

    fun wrapContext(base: Context, option: LanguageOption): Context {
        if (option == LanguageOption.SYSTEM) return base
        val config = buildOverrideConfiguration(base, option)
        return base.createConfigurationContext(config)
    }

    fun isBengaliActive(option: LanguageOption): Boolean {
        return resolveLocale(option).language.equals("bn", ignoreCase = true)
    }
}

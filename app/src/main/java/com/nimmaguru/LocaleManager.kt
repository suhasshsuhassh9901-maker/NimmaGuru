package com.nimmaguru

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

object LocaleManager {

    private const val PREF_FILE = "nimmaguru_prefs"
    private const val KEY_LANG  = "language"
    const val LANG_EN = "en"
    const val LANG_KN = "kn"

    fun getLanguage(ctx: Context): String =
        prefs(ctx).getString(KEY_LANG, LANG_EN) ?: LANG_EN

    fun setLanguage(ctx: Context, lang: String) {
        prefs(ctx).edit().putString(KEY_LANG, lang).apply()
    }

    fun wrap(ctx: Context): Context {
        val lang   = getLanguage(ctx)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(ctx.resources.configuration)
        config.setLocale(locale)
        return ctx.createConfigurationContext(config)
    }

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
}

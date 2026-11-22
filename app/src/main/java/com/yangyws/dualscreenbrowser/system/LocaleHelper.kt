package com.yangyws.dualscreenbrowser.system

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import com.yangyws.dualscreenbrowser.R
import java.util.Locale

enum class AppLanguage(
    val code: String,
    @param:StringRes val labelRes: Int
) {
    SYSTEM("system", R.string.language_system_default),
    ZH_TW("zh-TW", R.string.language_zh_tw),
    EN("en", R.string.language_english),
    JA("ja", R.string.language_japanese)
}

object LocaleHelper {
    fun currentLanguage(context: Context): AppLanguage {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            val locales = localeManager?.applicationLocales
            if (locales == null || locales.isEmpty) return AppLanguage.SYSTEM
            val tag = locales[0]?.toLanguageTag().orEmpty()
            return when {
                tag.startsWith("zh", ignoreCase = true) -> AppLanguage.ZH_TW
                tag.startsWith("en", ignoreCase = true) -> AppLanguage.EN
                tag.startsWith("ja", ignoreCase = true) -> AppLanguage.JA
                else -> AppLanguage.SYSTEM
            }
        }
        return AppLanguage.SYSTEM
    }

    fun applyLanguage(context: Context, language: AppLanguage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            val desired = when (language) {
                AppLanguage.SYSTEM -> LocaleList.getEmptyLocaleList()
                AppLanguage.EN -> LocaleList(Locale.ENGLISH)
                AppLanguage.ZH_TW -> LocaleList(Locale.TRADITIONAL_CHINESE)
                AppLanguage.JA -> LocaleList(Locale.JAPANESE)
            }
            if (localeManager != null && localeManager.applicationLocales != desired) {
                localeManager.applicationLocales = desired
            }
        }
    }
}

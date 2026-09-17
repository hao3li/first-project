package com.example.activitytest.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * 应用语言切换工具。
 *
 * 语言偏好完全交由 AppCompatDelegate 管理（会自动持久化并在重启后恢复），
 * 这里只负责读取/设置，避免自维护 SharedPreferences 导致的状态脱节。
 */
object LocaleHelper {

    const val LANG_ZH = "zh"
    const val LANG_EN = "en"

    /**
     * 判断当前应用是否使用中文。
     */
    fun isChinese(): Boolean {
        return getCurrentLanguage() == LANG_ZH
    }

    /**
     * 读取当前应用语言代码（"zh" 或 "en"）。
     * 优先取应用级 locale，未设置时回退到系统语言判断。
     */
    fun getCurrentLanguage(): String {
        val appLocales =
            AppCompatDelegate.getApplicationLocales()

        // 应用已显式设置过语言
        if (!appLocales.isEmpty) {
            val first = appLocales[0]?.language ?: ""
            return if (first == LANG_EN) LANG_EN else LANG_ZH
        }

        // 未设置时跟随系统语言
        val systemLang =
            Locale.getDefault().language
        return if (systemLang == LANG_EN) LANG_EN else LANG_ZH
    }

    /**
     * 切换应用语言，返回是否发生了实际变化。
     */
    fun setLanguage(language: String): Boolean {
        val target = if (language == LANG_EN) LANG_EN else LANG_ZH

        if (getCurrentLanguage() == target) {
            return false
        }

        val locale = if (target == LANG_EN) {
            Locale.ENGLISH
        } else {
            Locale.CHINESE
        }

        // AppCompatDelegate 会自动持久化该语言，并在下次启动时恢复
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(locale.toLanguageTag())
        )

        return true
    }
}

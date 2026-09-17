package com.example.activitytest.util

import android.app.Activity
import android.content.Context
import com.example.activitytest.R

object ThemeHelper {

    const val THEME_NAVY = "navy"
    const val THEME_GREEN = "green"
    const val THEME_PURPLE = "purple"
    const val THEME_PINK = "pink"
    const val THEME_SKYBLUE = "skyblue"
    const val THEME_YELLOW = "yellow"

    private const val PREF_NAME = "theme_preferences"
    private const val KEY_THEME = "selected_theme"

    fun applyTheme(activity: Activity) {
        activity.setTheme(
            when (getTheme(activity)) {
                THEME_GREEN -> R.style.Theme_ActivityTest_Green
                THEME_PURPLE -> R.style.Theme_ActivityTest_Purple
                THEME_PINK -> R.style.Theme_ActivityTest_Pink
                THEME_SKYBLUE -> R.style.Theme_ActivityTest_SkyBlue
                THEME_YELLOW -> R.style.Theme_ActivityTest_Yellow
                else -> R.style.Theme_ActivityTest
            }
        )
    }

    fun getTheme(context: Context): String = context
        .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .getString(KEY_THEME, THEME_NAVY)
        ?: THEME_NAVY

    fun saveTheme(context: Context, theme: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, theme)
            .apply()
    }
}

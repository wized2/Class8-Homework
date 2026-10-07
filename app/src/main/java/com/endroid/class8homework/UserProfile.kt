package com.endroid.class8homework

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class UserProfile(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("user_profile", Context.MODE_PRIVATE)

    var fullName: String
        get() = prefs.getString(KEY_NAME, "").orEmpty()
        set(v) = prefs.edit().putString(KEY_NAME, v.trim()).apply()

    /** true = Muslim, false = Non-Muslim, null = not set */
    var isMuslim: Boolean?
        get() = if (!prefs.contains(KEY_MUSLIM)) null else prefs.getBoolean(KEY_MUSLIM, true)
        set(v) {
            if (v == null) prefs.edit().remove(KEY_MUSLIM).apply()
            else prefs.edit().putBoolean(KEY_MUSLIM, v).apply()
        }

    /** system | light | dark */
    var themeMode: String
        get() = prefs.getString(KEY_THEME, "system").orEmpty().ifBlank { "system" }
        set(v) = prefs.edit().putString(KEY_THEME, v).apply()

    val isComplete: Boolean
        get() = fullName.isNotBlank() && isMuslim != null

    fun firstName(): String {
        val n = fullName.trim()
        if (n.isEmpty()) return ""
        return n.split("\\s+".toRegex()).first()
    }

    fun visibleSubjects(): List<Subjects.Info> {
        val muslim = isMuslim
        return Subjects.ALL.filter { info ->
            when (info.key) {
                "ethics" -> muslim == false
                "islamiat", "tarjama-tul-quran" -> muslim != false
                else -> true
            }
        }
    }

    fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(
            when (themeMode) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    companion object {
        private const val KEY_NAME = "full_name"
        private const val KEY_MUSLIM = "is_muslim"
        private const val KEY_THEME = "theme_mode"
    }
}

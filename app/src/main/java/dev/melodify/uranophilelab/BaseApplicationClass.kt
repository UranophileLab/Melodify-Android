package dev.melodify.uranophilelab

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import dev.melodify.uranophilelab.activities.SettingsActivity.SettingsSharedPrefManager
import dev.melodify.uranophilelab.utils.MusicPlayerManager
import dev.melodify.uranophilelab.utils.SharedPreferenceManager

open class BaseApplicationClass : Application() {
    private val TAG = "ApplicationClass"

    override fun onCreate() {
        super.onCreate()

        sharedPreferenceManager = SharedPreferenceManager.getInstance(this)
        MusicPlayerManager.init(this)
        updateTheme(this)

        sharedPreferenceManager?.migrateFromOldPrefs(this) { 
            sharedPreferenceManager?.clearOldPrefsAsync(this, null) 
        }
    }

    companion object {
        var sharedPreferenceManager: SharedPreferenceManager? = null

        fun updateTheme(context: Context? = null) {
            val ctx = context ?: MusicPlayerManager.appContext ?: return
            val settingsSharedPrefManager = SettingsSharedPrefManager(ctx)
            val theme = settingsSharedPrefManager.theme
            AppCompatDelegate.setDefaultNightMode(
                when (theme) {
                    "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                    "light" -> AppCompatDelegate.MODE_NIGHT_NO
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
        }
    }
}

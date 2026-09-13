package dev.melodify.uranophilelab

import android.app.Activity
import android.app.Application
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

        sharedPreferenceManager!!.migrateFromOldPrefs(this) { sharedPreferenceManager!!.clearOldPrefsAsync(this, null) }
    }

    companion object {
        var sharedPreferenceManager: SharedPreferenceManager? = null
        var currentActivity: Activity? = null

        fun updateTheme() {
            if (currentActivity == null) return
            val settingsSharedPrefManager = SettingsSharedPrefManager(currentActivity!!)
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

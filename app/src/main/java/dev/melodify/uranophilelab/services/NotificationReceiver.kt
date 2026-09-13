package dev.melodify.uranophilelab.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import dev.melodify.uranophilelab.activities.MusicOverviewActivity
import dev.melodify.uranophilelab.utils.MusicPlayerManager

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action == null) {
            Log.e(TAG, "Received null intent or action")
            return
        }

        val action = intent.action
        Log.i(TAG, "Received action: $action")

        val serviceIntent = Intent(context, MusicService::class.java)

        when (action) {
            MusicPlayerManager.ACTION_NEXT,
            MusicPlayerManager.ACTION_PREV,
            MusicPlayerManager.ACTION_PLAY -> {
                Log.i(TAG, "Processing media action: $action")
                serviceIntent.putExtra("action", action)
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start MusicService for action: $action", e)
                }
            }

            "action_click" -> {
                Log.i(TAG, "Processing CLICK action")
                try {
                    val activityIntent: Intent = Intent(context, MusicOverviewActivity::class.java)
                        .putExtra("id", MusicPlayerManager.MUSIC_ID)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(activityIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error launching activity", e)
                }
            }

            else -> Log.i(TAG, "Unknown action received: $action")
        }
    }

    companion object {
        private const val TAG = "NotificationReceiver"
    }
}

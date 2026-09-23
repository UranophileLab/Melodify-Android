package dev.melodify.uranophilelab.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.melodify.uranophilelab.utils.MusicPlayerManager

class WidgetControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            "ACTION_TOGGLE_PLAY" -> {
                Log.d("MelodifyWidget", "Play/Pause pressed")
                MusicPlayerManager.togglePlayPause()
            }

            "ACTION_NEXT" -> {
                Log.d("MelodifyWidget", "Next pressed")
                MusicPlayerManager.nextTrack()
            }

            "ACTION_PREV" -> {
                Log.d("MelodifyWidget", "Previous pressed")
                MusicPlayerManager.prevTrack()
            }
        }
    }
}

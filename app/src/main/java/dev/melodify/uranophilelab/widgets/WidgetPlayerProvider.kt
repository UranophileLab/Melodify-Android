package dev.melodify.uranophilelab.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.RemoteViews
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.activities.MusicOverviewActivity
import dev.melodify.uranophilelab.utils.MusicPlayerManager
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target

class WidgetPlayerProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, WidgetPlayerProvider::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_player)

            // Song Info
            val title = if (MusicPlayerManager.MUSIC_TITLE.isNullOrEmpty()) "Not Playing" else MusicPlayerManager.MUSIC_TITLE
            val description = MusicPlayerManager.MUSIC_DESCRIPTION ?: "SaavnMp3"
            
            // Extract artist from description if it follows "plays | year | copyright" or similar
            val artist = if (description.contains("|")) {
                description.substringAfterLast("|").trim()
            } else {
                description
            }

            views.setTextViewText(R.id.widget_song_title, title)
            views.setTextViewText(R.id.widget_artist_name, artist)

            // Play/Pause Icon
            val isPlaying = MusicPlayerManager.player?.isPlaying == true
            views.setImageViewResource(
                R.id.button_play_pause,
                if (isPlaying) R.drawable.baseline_pause_24 else R.drawable.play_arrow_24px
            )

            // Open App Intent
            val intent = Intent(context, MusicOverviewActivity::class.java).apply {
                putExtra("id", MusicPlayerManager.MUSIC_ID)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_album_art, pendingIntent)
            views.setOnClickPendingIntent(R.id.text_container, pendingIntent)

            // Control Intents
            views.setOnClickPendingIntent(R.id.button_play_pause, getPendingSelfIntent(context, "ACTION_TOGGLE_PLAY"))
            views.setOnClickPendingIntent(R.id.button_next, getPendingSelfIntent(context, "ACTION_NEXT"))
            views.setOnClickPendingIntent(R.id.button_prev, getPendingSelfIntent(context, "ACTION_PREV"))

            val imageUrl = MusicPlayerManager.IMAGE_URL
            if (!imageUrl.isNullOrEmpty()) {
                val target = object : Target {
                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        widgetTargets.remove(appWidgetId)
                        if (bitmap != null) {
                            views.setImageViewBitmap(R.id.widget_album_art, bitmap)
                        } else {
                            views.setImageViewResource(R.id.widget_album_art, R.mipmap.ic_launcher)
                        }
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                        widgetTargets.remove(appWidgetId)
                        views.setImageViewResource(R.id.widget_album_art, R.mipmap.ic_launcher)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
                }
                widgetTargets[appWidgetId] = target
                Picasso.get().load(imageUrl).into(target)
            } else {
                views.setImageViewResource(R.id.widget_album_art, R.mipmap.ic_launcher)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private val widgetTargets = HashMap<Int, Target>()

        private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, WidgetControlReceiver::class.java)
            intent.action = action
            return PendingIntent.getBroadcast(context, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
        }
    }
}

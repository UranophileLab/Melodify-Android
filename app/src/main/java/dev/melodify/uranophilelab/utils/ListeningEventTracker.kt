package dev.melodify.uranophilelab.utils

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.melodify.uranophilelab.network.ApiManager
import dev.melodify.uranophilelab.network.utility.RequestNetwork
import java.util.Calendar
import java.util.UUID

/**
 * Tracks listening events and telemetry for the Melodify Music Recommendation Engine.
 * Implements the 15-second skip rule, implicit score computation, and offline telemetry queuing.
 */
object ListeningEventTracker {
    private const val TAG = "ListeningEventTracker"
    private const val PREF_OFFLINE_EVENTS = "offline_telemetry_events"
    private val gson = Gson()

    data class ListeningEvent(
        val eventId: String = UUID.randomUUID().toString(),
        val userId: String,
        val songId: String,
        val playStartTime: Long,
        var playEndTime: Long = 0L,
        var listenDuration: Long = 0L,
        var skipped: Boolean = false,
        var skipTime: Long = 0L,
        var completed: Boolean = false,
        var replayed: Boolean = false,
        var liked: Boolean = false,
        var addedToPlaylist: Boolean = false,
        var shared: Boolean = false,
        val hourOfDay: Int,
        val dayOfWeek: Int,
        val device: String = "${Build.MANUFACTURER} ${Build.MODEL}",
        val source: String = "recommendation",
        var score: Double = 0.0
    )

    private var currentEvent: ListeningEvent? = null
    private var songDurationSeconds: Long = 0L

    fun onTrackStarted(context: Context, songId: String, durationSeconds: Long, source: String = "recommendation") {
        // Finalize previous session if active
        currentEvent?.let { finalizeAndDispatchEvent(context, wasSkipped = true) }

        val calendar = Calendar.getInstance()
        val userId = getOrCreateUserId(context)
        songDurationSeconds = durationSeconds

        currentEvent = ListeningEvent(
            userId = userId,
            songId = songId,
            playStartTime = System.currentTimeMillis() / 1000,
            hourOfDay = calendar.get(Calendar.HOUR_OF_DAY),
            dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
            source = source
        )
        Log.d(TAG, "Started tracking playback session for songId: $songId")
    }

    fun updateDuration(durationSeconds: Long) {
        if (durationSeconds > 0) {
            songDurationSeconds = durationSeconds
        }
    }

    fun onTrackLiked(songId: String) {
        currentEvent?.takeIf { it.songId == songId }?.let {
            it.liked = true
            Log.d(TAG, "Track marked as liked: $songId")
        }
    }

    fun onTrackReplayed(songId: String) {
        currentEvent?.takeIf { it.songId == songId }?.let {
            it.replayed = true
            Log.d(TAG, "Track marked as replayed: $songId")
        }
    }

    fun onTrackAddedToPlaylist(songId: String) {
        currentEvent?.takeIf { it.songId == songId }?.let {
            it.addedToPlaylist = true
            Log.d(TAG, "Track added to playlist: $songId")
        }
    }

    fun onTrackShared(songId: String) {
        currentEvent?.takeIf { it.songId == songId }?.let {
            it.shared = true
            Log.d(TAG, "Track shared: $songId")
        }
    }

    fun onTrackEnded(context: Context, completed: Boolean, wasSkipped: Boolean, currentPositionMs: Long = 0L) {
        val event = currentEvent ?: return
        val now = System.currentTimeMillis() / 1000
        event.playEndTime = now
        event.listenDuration = (now - event.playStartTime).coerceAtLeast(0L)
        event.completed = completed
        event.skipped = wasSkipped

        if (wasSkipped) {
            event.skipTime = if (currentPositionMs > 0) currentPositionMs / 1000 else event.listenDuration
        } else {
            event.skipTime = 0L
        }

        finalizeAndDispatchEvent(context, wasSkipped)
    }

    private fun finalizeAndDispatchEvent(context: Context, wasSkipped: Boolean) {
        val event = currentEvent ?: return
        currentEvent = null

        // Evaluate score using the 15-second skip rule
        event.score = calculateImplicitScore(event, songDurationSeconds)
        Log.d(TAG, "Finalized session for ${event.songId}: listenSec=${event.listenDuration}, skipSec=${event.skipTime}, score=${event.score}")

        dispatchTelemetryEvent(context, event)
    }

    /**
     * Calculates score based on explicit/implicit feedback rules:
     * - Skipped < 15s = -1.0
     * - Skipped 15s-30s = 0.0
     * - Listened > 50% = +1.0
     * - Completed (>= 90%) = +2.0
     * - Replay, like, add to playlist, or share = +3.0
     */
    fun calculateImplicitScore(event: ListeningEvent, totalSongDuration: Long): Double {
        if (event.liked || event.replayed || event.addedToPlaylist || event.shared) {
            return 3.0
        }

        if (event.completed) {
            return 2.0
        }

        val listenRatio = if (totalSongDuration > 0) event.listenDuration.toDouble() / totalSongDuration else 0.0
        if (listenRatio >= 0.9) {
            return 2.0
        }

        if (event.skipped) {
            val skipSec = event.skipTime
            return when {
                skipSec < 15 -> -1.0
                skipSec in 15..30 -> 0.0
                listenRatio > 0.5 -> 1.0
                else -> -0.5
            }
        }

        if (listenRatio > 0.5) {
            return 1.0
        }

        return 0.0
    }

    private fun dispatchTelemetryEvent(context: Context, event: ListeningEvent) {
        val jsonPayload = gson.toJson(event)
        val apiManager = ApiManager(context)

        apiManager.postTelemetryEvent(jsonPayload, object : RequestNetwork.RequestListener {
            override fun onResponse(tag: String?, response: String?, responseHeaders: HashMap<String?, Any?>?) {
                Log.i(TAG, "Successfully synced telemetry event: ${event.eventId}")
                flushOfflineQueue(context)
            }

            override fun onErrorResponse(tag: String?, message: String?) {
                Log.w(TAG, "Failed to send telemetry event, saving offline: $message")
                queueEventOffline(context, event)
            }
        })
    }

    private fun queueEventOffline(context: Context, event: ListeningEvent) {
        val prefs = context.getSharedPreferences("melodify_telemetry", Context.MODE_PRIVATE)
        val existingJson = prefs.getString(PREF_OFFLINE_EVENTS, "[]") ?: "[]"
        val type = object : TypeToken<MutableList<ListeningEvent>>() {}.type
        val events: MutableList<ListeningEvent> = try {
            gson.fromJson(existingJson, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
        events.add(event)
        prefs.edit().putString(PREF_OFFLINE_EVENTS, gson.toJson(events)).apply()
    }

    fun flushOfflineQueue(context: Context) {
        val prefs = context.getSharedPreferences("melodify_telemetry", Context.MODE_PRIVATE)
        val existingJson = prefs.getString(PREF_OFFLINE_EVENTS, "[]") ?: "[]"
        if (existingJson == "[]") return

        val type = object : TypeToken<MutableList<ListeningEvent>>() {}.type
        val events: MutableList<ListeningEvent> = try {
            gson.fromJson(existingJson, type) ?: mutableListOf()
        } catch (e: Exception) {
            return
        }

        if (events.isEmpty()) return

        val apiManager = ApiManager(context)
        val payload = gson.toJson(events)

        apiManager.postTelemetryBatch(payload, object : RequestNetwork.RequestListener {
            override fun onResponse(tag: String?, response: String?, responseHeaders: HashMap<String?, Any?>?) {
                Log.i(TAG, "Flushed ${events.size} offline telemetry events")
                prefs.edit().remove(PREF_OFFLINE_EVENTS).apply()
            }

            override fun onErrorResponse(tag: String?, message: String?) {
                Log.w(TAG, "Failed to flush offline telemetry queue: $message")
            }
        })
    }

    private fun getOrCreateUserId(context: Context): String {
        val prefs = context.getSharedPreferences("melodify_user_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("user_guid", null)
        if (id == null) {
            id = "usr_" + UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString("user_guid", id).apply()
        }
        return id
    }
}

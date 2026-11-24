package com.samyak.lrclib

import com.samyak.lrclib.models.Track
import com.samyak.lrclib.models.bestMatchingFor
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.math.abs

/**
 * Source: https://github.com/samyak2403/
 */
object LrcLib {
    private val client by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }

            defaultRequest {
                url("https://lrclib.net")
            }

            expectSuccess = true
        }
    }

    private suspend fun queryLyrics(
        artist: String,
        title: String,
        album: String? = null,
    ) = client
        .get("/api/search") {
            parameter("track_name", title)
            parameter("artist_name", artist)
            if (album != null) parameter("album_name", album)
        }.body<List<Track>>()
        .filter { it.syncedLyrics != null }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ) = runCatching {
        val tracks = queryLyrics(artist, title, album)

        val res = tracks.bestMatchingFor(duration)?.syncedLyrics?.let(LrcLib::Lyrics)
        if (res != null) {
            return@runCatching res.text
        } else {
            throw IllegalStateException("Lyrics unavailable")
        }
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        callback: (String) -> Unit,
    ) {
        val tracks = queryLyrics(artist, title, album)
        var count = 0
        var plain = 0
        tracks.forEach {
            if (count <= 4) {
                if (it.syncedLyrics != null && duration == -1) {
                    count++
                    it.syncedLyrics.let(callback)
                } else {
                    if (it.syncedLyrics != null && abs(it.duration - duration) <= 2) {
                        count++
                        it.syncedLyrics.let(callback)
                    }
                    if (it.plainLyrics != null && abs(it.duration - duration) <= 2 && plain == 0) {
                        count++
                        plain++
                        it.plainLyrics.let(callback)
                    }
                }
            }
        }
    }

    suspend fun lyrics(
        artist: String,
        title: String,
    ) = runCatching {
        queryLyrics(artist = artist, title = title, album = null)
    }

    @JvmInline
    value class Lyrics(
        val text: String,
    ) {
        val sentences: Map<Long, String>?
            get() =
                runCatching {
                    val timestampRegex = Regex("""^\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?](.*)""")
                    buildMap {
                        put(0L, "")
                        text.trim().lines().forEach { line ->
                            val trimmed = line.trim()
                            val match = timestampRegex.find(trimmed)
                            if (match != null) {
                                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                                val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                                val fractionStr = match.groupValues[3]
                                val millis = when (fractionStr.length) {
                                    1 -> fractionStr.toLong() * 100L
                                    2 -> fractionStr.toLong() * 10L
                                    3 -> fractionStr.toLong()
                                    else -> 0L
                                }
                                val totalTimeMs = minutes * 60 * 1000L + seconds * 1000L + millis
                                val lyric = match.groupValues[4].trim()
                                put(totalTimeMs, lyric)
                            }
                        }
                    }
                }.getOrNull()
    }
}

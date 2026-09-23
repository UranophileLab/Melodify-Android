package dev.melodify.uranophilelab.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import dev.melodify.uranophilelab.records.SongResponse.Song
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import org.jaudiotagger.tag.mp4.field.Mp4TagReverseDnsField
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.Executors

object TrackDownloader {
    private const val TAG = "TrackDownloader"
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun sanitizeFileName(name: String?): String {
        if (name.isNullOrBlank()) return "Untitled"
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }

    fun isAlreadyDownloaded(title: String?): Boolean {
        if (title.isNullOrBlank()) return false
        val safeTitle = sanitizeFileName(title)
        val musicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Melodify"
        )
        if (!musicDir.exists()) {
            return false
        }
        val extensions = listOf(".mp4", ".m4a", ".mp3", "")
        return extensions.any { ext -> File(musicDir, "$safeTitle$ext").exists() }
    }

    fun getDownloadedTracks(context: Context): MutableList<DownloadedTrack?> {
        val data: MutableList<DownloadedTrack?> = ArrayList()
        val musicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Melodify"
        )
        if (!musicDir.exists()) {
            return data
        }
        val files = musicDir.listFiles() ?: return data
        val uidFieldId = "----:" + context.packageName + ":TrackUID"

        for (file in files) {
            if (!file.isFile || file.length() == 0L) continue
            try {
                val f = AudioFileIO.read(file)
                val tag = f.tag
                val audioHeader = f.audioHeader

                val tagTitle = tag?.getFirst(FieldKey.TITLE)
                val title: String = if (!tagTitle.isNullOrBlank()) {
                    tagTitle
                } else {
                    file.nameWithoutExtension
                }
                val artist: String = tag?.getFirst(FieldKey.ARTIST) ?: ""
                val album: String = tag?.getFirst(FieldKey.ALBUM) ?: ""
                val year: String = tag?.getFirst(FieldKey.YEAR) ?: ""
                val bitrate = audioHeader?.bitRate ?: "344"
                val trackLength = audioHeader?.trackLength?.toString() ?: "0"

                var trackUID: String? = null
                if (tag != null) {
                    val field = tag.getFirstField(uidFieldId)
                    if (field != null) {
                        try {
                            trackUID = field.toString()
                            try {
                                val m = field.javaClass.getMethod("getContent")
                                val valObj = m.invoke(field)
                                if (valObj != null) trackUID = valObj.toString()
                            } catch (_: NoSuchMethodException) {
                            }
                        } catch (_: Exception) {
                        }
                    } else {
                        val it = tag.fields
                        while (it.hasNext()) {
                            val fField = it.next()
                            val id = fField.id
                            if (id != null && id.equals(uidFieldId, ignoreCase = true)) {
                                try {
                                    trackUID = fField.toString()
                                    break
                                } catch (_: Exception) {
                                }
                            }
                        }
                    }
                }

                val coverImageBytes = tag?.firstArtwork?.binaryData
                val bitmap = if (coverImageBytes != null) {
                    decodeSampledBitmapFromByteArray(coverImageBytes, 300, 300)
                } else null

                val downloadedTrack = DownloadedTrack(
                    file,
                    title,
                    artist,
                    album,
                    year,
                    bitrate,
                    trackLength,
                    bitmap,
                    trackUID
                )
                data.add(downloadedTrack)
            } catch (e: Exception) {
                Log.e(TAG, "Error reading file ${file.name}: " + e.message)
            }
        }
        return data
    }

    private fun decodeSampledBitmapFromByteArray(data: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode sampled artwork", e)
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun downloadAndEmbedMetadata(context: Context, song: Song, listener: TrackDownloadListener) {
        val appContext = context.applicationContext
        val audioUrl = song.downloadUrl?.lastOrNull()?.url ?: ""
        val imageUrl = song.image?.lastOrNull()?.url ?: ""
        val rawTitle = song.name() ?: ""
        val safeTitle = sanitizeFileName(rawTitle)
        val artist = song.artists?.primary?.firstOrNull()?.name() ?: ""
        val album = song.album?.name() ?: ""

        if (audioUrl.isBlank()) {
            mainHandler.post { listener.onError("Audio URL is unavailable") }
            return
        }

        executor.execute {
            mainHandler.post { listener.onStarted() }
            Log.d(TAG, "⬇️ Downloading and embedding metadata for: $rawTitle")
            var tempFile: File? = null
            var artworkFile: File? = null

            try {
                tempFile = File(appContext.cacheDir, "$safeTitle.mp4")
                URL(audioUrl).openStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val audioFile = AudioFileIO.read(tempFile)
                val tag = audioFile.getTagOrCreateAndSetDefault()

                tag.setField(FieldKey.TITLE, rawTitle)
                tag.setField(FieldKey.ARTIST, artist)
                tag.setField(FieldKey.ALBUM, album)
                if (!song.year.isNullOrBlank()) {
                    tag.setField(FieldKey.YEAR, song.year)
                }

                val uidField = Mp4TagReverseDnsField(
                    "----",
                    appContext.packageName,
                    "TrackUID",
                    song.id ?: ""
                )
                tag.setField(uidField)

                if (imageUrl.isNotBlank()) {
                    try {
                        artworkFile = File(appContext.cacheDir, "artwork_${System.currentTimeMillis()}.jpg")
                        URL(imageUrl).openStream().use { input ->
                            FileOutputStream(artworkFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        val artwork = ArtworkFactory.createArtworkFromFile(artworkFile)
                        tag.deleteArtworkField()
                        tag.setField(artwork)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to embed artwork: ${e.message}")
                    }
                }

                audioFile.setTag(tag)
                audioFile.commit()

                val resolver = appContext.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.TITLE, rawTitle)
                    put(MediaStore.Audio.Media.ARTIST, artist)
                    put(MediaStore.Audio.Media.ALBUM, album)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                    put(MediaStore.Audio.Media.IS_MUSIC, true)
                }

                val musicDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    "Melodify"
                )
                if (!musicDir.exists()) {
                    musicDir.mkdirs()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.put(MediaStore.Audio.Media.DISPLAY_NAME, "$safeTitle.mp4")
                    values.put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/Melodify")
                    val audioCollection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    val newUri = resolver.insert(audioCollection, values)
                        ?: throw Exception("Failed to insert into MediaStore")

                    tempFile.inputStream().use { input ->
                        resolver.openOutputStream(newUri)?.use { output ->
                            input.copyTo(output)
                        } ?: throw Exception("Failed to open MediaStore output stream")
                    }
                } else {
                    val targetFile = File(musicDir, "$safeTitle.mp4")
                    tempFile.copyTo(targetFile, overwrite = true)
                    values.put(MediaStore.Audio.Media.DISPLAY_NAME, "$safeTitle.mp4")
                    values.put(MediaStore.Audio.Media.DATA, targetFile.absolutePath)
                    val newUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                    val scanUri = newUri ?: Uri.fromFile(targetFile)
                    appContext.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, scanUri))
                }

                Log.d(TAG, "✅ Downloaded and tagged: $rawTitle")
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading track: ${e.message}", e)
                mainHandler.post { listener.onError(e.message ?: "Unknown download error") }
            } finally {
                tempFile?.let { if (it.exists()) it.delete() }
                artworkFile?.let { if (it.exists()) it.delete() }
                mainHandler.post { listener.onFinished() }
            }
        }
    }

    interface TrackDownloadListener {
        fun onStarted()
        fun onFinished()
        fun onError(errorMessage: String?)
    }

    data class DownloadedTrack(
        val file: File?,
        val title: String?,
        val artist: String?,
        val album: String?,
        val year: String?,
        val bitrate: String?,
        val trackLength: String?,
        val coverImage: Bitmap?,
        val trackUID: String?
    )
}

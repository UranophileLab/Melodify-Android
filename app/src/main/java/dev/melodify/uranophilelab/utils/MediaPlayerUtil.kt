package dev.melodify.uranophilelab.utils

import android.media.MediaPlayer

class MediaPlayerUtil private constructor() : MediaPlayer() {
    private val TAG = "MediaPlayerUtil"

    companion object {
        @Volatile
        private var INSTANCE: MediaPlayerUtil? = null

        val instance: MediaPlayerUtil
            get() {
                return INSTANCE ?: synchronized(this) {
                    INSTANCE ?: MediaPlayerUtil().also { INSTANCE = it }
                }
            }
    }
}

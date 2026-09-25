package com.example.ui.util

import android.content.Context
import android.media.MediaPlayer
import com.example.R

/** Plays the user-provided wrong-PIN/access-denied sound. */
object SirenAudioPlayer {
    private var player: MediaPlayer? = null

    fun playWrongPin(context: Context) {
        stop()
        try {
            player = MediaPlayer.create(context.applicationContext, R.raw.wrong).apply {
                setOnCompletionListener { mp -> mp.release(); if (player === mp) player = null }
                start()
            }
        } catch (_: Exception) {
            player = null
        }
    }

    fun stop() {
        try { player?.stop() } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        player = null
    }
}

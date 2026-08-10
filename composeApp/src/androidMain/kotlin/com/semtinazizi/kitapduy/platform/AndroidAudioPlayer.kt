package com.semtinazizi.kitapduy.platform

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Android AudioPlayer implementasyonu - Media3/ExoPlayer kullanır
 * commonMain'deki AudioPlayer interface'ini implement eder
 */
class AndroidAudioPlayer : AudioPlayer {
    private var exoPlayer: ExoPlayer? = null
    private var context: Context? = null

    fun setContext(ctx: Context) {
        context = ctx
        exoPlayer = ExoPlayer.Builder(ctx).build()
    }

    override fun initialize() {
        // Context setContext() ile set edilmeli
    }

    override fun play(url: String) {
        val player = exoPlayer ?: return
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    override fun pause() {
        exoPlayer?.pause()
    }

    override fun resume() {
        exoPlayer?.play()
    }

    override fun stop() {
        exoPlayer?.stop()
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    override fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }

    override fun getDuration(): Long {
        return exoPlayer?.duration ?: 0L
    }

    override fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying ?: false
    }

    override fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
}

/**
 * Android BuildConfig değerlerini KMP'ye aktarır
 * Bu dosya androidMain'de olduğu için BuildConfig'e erişebilir
 */
object BuildConfigHelper {
    var supabaseUrl: String = ""
    var supabaseKey: String = ""
    var cfAccountId: String = ""
    var cfPublicUrl: String = ""
}

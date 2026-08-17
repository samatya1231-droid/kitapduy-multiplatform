package com.semtinazizi.kitapduy.platform

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.semtinazizi.kitapduy.platform.AudioMediaItem

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

    override fun playQueue(items: List<AudioMediaItem>, startIndex: Int, startPositionMs: Long) {
        val player = exoPlayer ?: return
        
        val mediaItems = items.map { item ->
            MediaItem.Builder()
                .setMediaId(item.id)
                .setUri(item.url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.title)
                        .setArtist(item.artist)
                        .setArtworkUri(if (item.artworkUrl.isNotBlank()) Uri.parse(item.artworkUrl) else null)
                        .setIsPlayable(true)
                        .build()
                )
                .build()
        }
        
        player.setMediaItems(mediaItems, startIndex, startPositionMs)
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

    override fun setPlaybackSpeed(speed: Float) {
        val params = androidx.media3.common.PlaybackParameters(speed)
        exoPlayer?.playbackParameters = params
    }

    private var atmospherePlayer: ExoPlayer? = null

    override fun playAtmosphere(url: String, volume: Float) {
        val ctx = context ?: return
        if (atmospherePlayer == null) {
            atmospherePlayer = ExoPlayer.Builder(ctx).build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
            }
        }
        val mediaItem = MediaItem.fromUri(url)
        atmospherePlayer?.setMediaItem(mediaItem)
        atmospherePlayer?.volume = volume
        atmospherePlayer?.prepare()
        atmospherePlayer?.play()
    }

    override fun stopAtmosphere() {
        atmospherePlayer?.stop()
        atmospherePlayer?.release()
        atmospherePlayer = null
    }

    override fun setAtmosphereVolume(volume: Float) {
        atmospherePlayer?.volume = volume
    }

    override fun release() {
        exoPlayer?.release()
        exoPlayer = null
        stopAtmosphere()
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

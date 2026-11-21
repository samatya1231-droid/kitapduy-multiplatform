package com.semtinazizi.kitapduy.platform

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.semtinazizi.kitapduy.platform.AudioMediaItem
import com.semtinazizi.kitapduy.util.UrlSanitizer

/**
 * Android AudioPlayer implementasyonu - Media3/ExoPlayer kullanır
 * commonMain'deki AudioPlayer interface'ini implement eder
 */
class AndroidAudioPlayer : AudioPlayer {
    private var exoPlayer: ExoPlayer? = null
    private var context: Context? = null
    private var queuedItems: List<AudioMediaItem> = emptyList()
    private var playbackError: String? = null
    private var reachedEnd: Boolean = false
    private var previewPlayer: ExoPlayer? = null

    fun setContext(ctx: Context) {
        context = ctx
        androidAppContext = ctx.applicationContext
        exoPlayer = ExoPlayer.Builder(ctx).build()
        exoPlayer?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                playbackError = null
                reachedEnd = false
                val item = queuedItems.getOrNull(exoPlayer?.currentMediaItemIndex ?: -1) ?: return
                if (item.startPositionMs > 0) exoPlayer?.seekTo(item.startPositionMs)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    playbackError = null
                    reachedEnd = false
                } else if (playbackState == Player.STATE_ENDED) {
                    reachedEnd = true
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val currentId = exoPlayer?.currentMediaItem?.mediaId
                if (currentId == "kitapduy_preroll_ad" || currentId == "kitapduy_postroll_ad") {
                    if (exoPlayer?.hasNextMediaItem() == true) {
                        exoPlayer?.seekToNextMediaItem()
                        exoPlayer?.prepare()
                        exoPlayer?.play()
                    } else {
                        exoPlayer?.stop()
                    }
                    return
                }
                playbackError = "Ses dosyası yüklenemedi. Lütfen bağlantınızı kontrol edip tekrar deneyin."
            }
        })
    }

    override fun initialize() {
        // Context setContext() ile set edilmeli
    }

    override fun play(url: String) {
        val player = exoPlayer ?: return
        playbackError = null
        val mediaItem = MediaItem.fromUri(UrlSanitizer.sanitizeUrl(url))
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    override fun playQueue(items: List<AudioMediaItem>, startIndex: Int, startPositionMs: Long) {
        val player = exoPlayer ?: return
        if (items.isEmpty()) return
        playbackError = null
        queuedItems = items
        
        val mediaItems = items.map { item ->
            MediaItem.Builder()
                .setMediaId(item.id)
                .setUri(UrlSanitizer.sanitizeUrl(item.url))
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
        
        val index = startIndex.coerceIn(items.indices)
        player.setMediaItems(mediaItems, index, if (startPositionMs > 0) startPositionMs else items[index].startPositionMs)
        player.prepare()
        player.play()
    }

    override fun pause() {
        exoPlayer?.pause()
    }

    override fun resume() {
        val player = exoPlayer ?: return
        playbackError = null
        if (player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        }
        player.play()
    }

    override fun stop() {
        playbackError = null
        exoPlayer?.stop()
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    override fun getCurrentPosition(): Long {
        return (exoPlayer?.currentPosition ?: 0L).coerceAtLeast(0L)
    }

    override fun getDuration(): Long {
        val raw = exoPlayer?.duration ?: 0L
        return if (raw > 0L) raw else 0L
    }

    override fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying ?: false
    }

    override fun isBuffering(): Boolean {
        val player = exoPlayer ?: return false
        return player.playbackState == Player.STATE_BUFFERING && player.playWhenReady
    }

    override fun getPlaybackError(): String? = playbackError

    override fun clearPlaybackError() {
        playbackError = null
    }
    override fun currentMediaId(): String? = exoPlayer?.currentMediaItem?.mediaId

    override fun setPlaybackSpeed(speed: Float) {
        val params = androidx.media3.common.PlaybackParameters(speed)
        exoPlayer?.playbackParameters = params
    }

    private var atmospherePlayer: ExoPlayer? = null

    override fun playAtmosphere(url: String, volume: Float) {
        val ctx = context ?: return
        if (atmospherePlayer == null) {
            atmospherePlayer = ExoPlayer.Builder(ctx).build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
        }
        val mediaItem = MediaItem.fromUri(UrlSanitizer.sanitizeUrl(url))
        atmospherePlayer?.setMediaItem(mediaItem)
        atmospherePlayer?.volume = volume.coerceIn(0f, 1f)
        atmospherePlayer?.prepare()
        atmospherePlayer?.play()
    }

    override fun pauseAtmosphere() {
        atmospherePlayer?.pause()
    }

    override fun resumeAtmosphere() {
        atmospherePlayer?.play()
    }

    override fun stopAtmosphere() {
        atmospherePlayer?.stop()
        atmospherePlayer?.release()
        atmospherePlayer = null
    }

    override fun setAtmosphereVolume(volume: Float) {
        atmospherePlayer?.volume = volume.coerceIn(0f, 1f)
    }

    override fun hasReachedTrackEnd(): Boolean = reachedEnd

    override fun skipToMediaId(mediaId: String): Boolean {
        val player = exoPlayer ?: return false
        val targetIndex = queuedItems.indexOfFirst { it.id == mediaId }
        if (targetIndex < 0) return false
        val startPos = queuedItems[targetIndex].startPositionMs.coerceAtLeast(0L)
        player.seekTo(targetIndex, startPos)
        player.prepare()
        player.play()
        return true
    }

    override fun startPreview(url: String, startPositionMs: Long) {
        stopPreview()
        val ctx = context ?: return
        val player = ExoPlayer.Builder(ctx).build()
        previewPlayer = player
        player.setMediaItem(MediaItem.fromUri(UrlSanitizer.sanitizeUrl(url)))
        player.prepare()
        if (startPositionMs > 0L) player.seekTo(startPositionMs)
        player.play()
    }

    override fun seekPreviewTo(positionMs: Long) {
        previewPlayer?.seekTo(positionMs.coerceAtLeast(0L))
    }

    override fun stopPreview() {
        previewPlayer?.stop()
        previewPlayer?.release()
        previewPlayer = null
    }

    override fun isPreviewPlaying(): Boolean = previewPlayer?.isPlaying == true

    override fun release() {
        exoPlayer?.release()
        exoPlayer = null
        stopAtmosphere()
        stopPreview()
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

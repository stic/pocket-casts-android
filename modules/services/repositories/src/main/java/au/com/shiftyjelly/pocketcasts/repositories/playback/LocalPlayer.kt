package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.os.SystemClock
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.repositories.playback.LocalPlayer.Companion.VOLUME_NORMAL
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages audio focus with local media player.
 */
abstract class LocalPlayer(
    onPlayerEvent: suspend (Player, PlayerEvent) -> Unit
) : Player {

    val onPlayerEvent: suspend (PlayerEvent) -> Unit = { onPlayerEvent(this, it) }

    companion object {
        // The volume we set the media player to seekToTimeMswhen we lose audio focus, but are allowed to reduce the volume instead of stopping playback.
        const val VOLUME_DUCK = 1.0f // We don't actually duck the volume
        // The volume we set the media player when we have audio focus.
        const val VOLUME_NORMAL = 1.0f
    }

    // playback position for starting or resuming from focus lost
    @Volatile private var positionMs: Int = 0

    private var seekingToPositionMs: Int = 0
    private var seekRetryAllowed: Boolean = false

    protected var isHLS: Boolean = false

    override var episodeUuid: String? = null

    override var episodeLocation: EpisodeLocation? = null
    override val url: String?
        get() = (episodeLocation as? EpisodeLocation.Stream)?.uri

    override val filePath: String?
        get() = (episodeLocation as? EpisodeLocation.Downloaded)?.filePath

    override val isRemote: Boolean
        get() = false

    override val isStreaming: Boolean
        get() = episodeLocation is EpisodeLocation.Stream

    override val name: String
        get() = "System"

    abstract suspend fun handlePrepare()
    abstract fun handleStop()
    abstract suspend fun handlePause()
    abstract suspend fun handlePlay()
    abstract suspend fun handleSeekToTimeMs(positionMs: Int)
    abstract fun handleIsBuffering(): Boolean
    abstract fun handleIsPrepared(): Boolean
    abstract suspend fun handleCurrentPositionMs(): Int

    override suspend fun load(currentPositionMs: Int) {
        withContext(Dispatchers.Main) {
            this@LocalPlayer.positionMs = currentPositionMs
            handlePrepare()
            seekToTimeMs(currentPositionMs)
        }
    }

    // downloaded episodes don't buffer
    override suspend fun isBuffering(): Boolean {
        return withContext(Dispatchers.Main) {
            if (filePath != null) false else handleIsBuffering()
        }
    }

    override suspend fun play(currentPositionMs: Int) {
        withContext(Dispatchers.Main) {
            this@LocalPlayer.positionMs = currentPositionMs

            handlePrepare()
            playIfAllowed()
        }
    }

    override suspend fun pause() {
        if (isPlaying()) {
            handlePause()
            positionMs = handleCurrentPositionMs()
        }
        onPlayerEvent(PlayerEvent.PlayerPaused)
    }

    override suspend fun stop() {
        withContext(Dispatchers.Main) {
            positionMs = handleCurrentPositionMs()
            handleStop()
        }
    }

    override suspend fun getCurrentPositionMs(): Int {
        return withContext(Dispatchers.Main) {
            handleCurrentPositionMs()
        }
    }

    protected suspend fun onError(event: PlayerEvent.PlayerError) {
        onPlayerEvent(event)
    }

    private suspend fun playIfAllowed() {
        setVolume(VOLUME_NORMAL)

        // already playing?
        if (isPlaying()) {
            onPlayerEvent(PlayerEvent.PlayerPlaying)
        } else {
            // check the player is seeked to the correct position
            val playerPositionMs = getCurrentPositionMs()
            // check if the player is where it's meant to be, allow for a 2 second variance
            if (Math.abs(positionMs - playerPositionMs) > 2000) {
                onSeekStart(positionMs)
                handleSeekToTimeMs(positionMs)
            }
            handlePlay()
            onPlayerEvent(PlayerEvent.PlayerPlaying)
        }
    }

    private fun onSeekStart(positionMs: Int) {
        this.seekingToPositionMs = positionMs
        this.seekRetryAllowed = true
    }

    protected suspend fun onSeekComplete(positionMs: Int) {
        // Fix for the BLU phone. With a new media player (also after a hibernate) the MediaTek player call switches to an invalid time.
        if (positionMs < seekingToPositionMs - 5000 && seekRetryAllowed) {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Player issue was meant to be %.3f but was %.3f", seekingToPositionMs / 1000f, positionMs / 1000f)
            SystemClock.sleep(100)
            seekRetryAllowed = false
            handleSeekToTimeMs(seekingToPositionMs)
            return
        }
        this.positionMs = positionMs
        onPlayerEvent(PlayerEvent.SeekComplete(positionMs))
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "LocalPlayer onSeekComplete %.3f", positionMs / 1000f)
    }

    protected suspend fun onDurationAvailable() {
        onPlayerEvent(PlayerEvent.DurationAvailable)
    }

    protected suspend fun onCompletion() {
        onPlayerEvent(PlayerEvent.Completion(episodeUuid))
    }

    protected suspend fun onBufferingStateChanged() {
        if (isStreaming) {
            onPlayerEvent(PlayerEvent.BufferingStateChanged)
        }
    }

    protected suspend fun onMetadataAvailable(episodeMetadata: EpisodeFileMetadata) {
        onPlayerEvent(PlayerEvent.MetadataAvailable(episodeMetadata))
    }

    override suspend fun seekToTimeMs(positionMs: Int) {
        withContext(Dispatchers.Main) {
            if (positionMs < 0) {
                return@withContext
            }

            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "LocalPlayer seekToToTimeMs %.3f", positionMs / 1000f)

            this@LocalPlayer.positionMs = positionMs
            if (handleIsPrepared()) {
                onSeekStart(positionMs)
                handleSeekToTimeMs(positionMs)
            }
        }
    }

    override fun setEpisode(episode: Playable) {
        this.episodeUuid = episode.uuid
        this.isHLS = episode.isHLS
        episodeLocation = if (episode.isDownloaded) {
            EpisodeLocation.Downloaded(episode.downloadedFilePath)
        } else {
            EpisodeLocation.Stream(episode.downloadUrl)
        }
    }

    override suspend fun setPlaybackEffects(playbackEffects: PlaybackEffects) {}
}

package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.media3.common.Player

interface PlayerFactory {

    fun createCastPlayer(
        onPlayerEvent: (PocketCastsPlayer, PlayerEvent) -> Unit,
        player: Player,
    ): PocketCastsPlayer

    fun createSimplePlayer(
        onPlayerEvent: (PocketCastsPlayer, PlayerEvent) -> Unit,
        player: Player,
    ): PocketCastsPlayer
}

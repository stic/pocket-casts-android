package au.com.shiftyjelly.pocketcasts.repositories.playback

interface PlayerFactory {

    fun createCastPlayer(onPlayerEvent: suspend (Player, PlayerEvent) -> Unit): Player
    fun createSimplePlayer(onPlayerEvent: suspend (Player, PlayerEvent) -> Unit): Player
}

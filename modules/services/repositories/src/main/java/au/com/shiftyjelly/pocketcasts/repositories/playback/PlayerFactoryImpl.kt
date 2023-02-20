package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PlayerFactoryImpl @Inject constructor(
    private val settings: Settings,
    private val statsManager: StatsManager,
    @ApplicationContext private val context: Context
) : PlayerFactory {

    override fun createCastPlayer(onPlayerEvent: suspend (Player, PlayerEvent) -> Unit): Player =
        CastPlayer(context, onPlayerEvent)

    override fun createSimplePlayer(onPlayerEvent: suspend (Player, PlayerEvent) -> Unit): Player =
        SimplePlayer(settings, statsManager, context, onPlayerEvent)
}

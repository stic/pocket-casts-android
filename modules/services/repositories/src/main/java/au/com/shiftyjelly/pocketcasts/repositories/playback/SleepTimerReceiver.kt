package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SleepTimerReceiver :
    BroadcastReceiver(),
    CoroutineScope by CoroutineScope(Dispatchers.Default) {

    @Inject lateinit var playbackManager: PlaybackManager

    override fun onReceive(context: Context, intent: Intent) {
        launch {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Paused from sleep timer.")
            Toast.makeText(
                context,
                "Sleep timer stopped your podcast.\nNight night!",
                Toast.LENGTH_LONG
            ).show()
            playbackManager.pause(playbackSource = AnalyticsSource.AUTO_PAUSE)
            playbackManager.updateSleepTimerStatus(running = false)
        }
    }
}

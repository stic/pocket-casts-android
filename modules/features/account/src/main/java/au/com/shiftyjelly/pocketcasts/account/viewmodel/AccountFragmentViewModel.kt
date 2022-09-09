package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.PendingIntent
import android.content.Context
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AccountFragmentViewModel @Inject constructor(
    userManager: UserManager,
    private val syncServerManager: SyncServerManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val signInState = LiveDataReactiveStreams.fromPublisher(userManager.getSignInState())

    fun launchGoogleSignIn(onSuccess: (PendingIntent) -> Unit, onFailure: (Exception) -> Unit) {
        val request = GetSignInIntentRequest.builder()
            .setServerClientId(Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID)
            .build()

        Identity.getSignInClient(context)
            .getSignInIntent(request)
            .addOnSuccessListener { pendingIntent -> onSuccess(pendingIntent) }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    fun signInWithGoogle(credential: SignInCredential) {
        val idToken = credential.googleIdToken ?: return
        viewModelScope.launch {
            try {
                syncServerManager.loginGoogle(idToken)
            } catch (e: Exception) {
                Timber.e(e, "Pocket Casts login with Google failed.")
            }
        }
    }
}

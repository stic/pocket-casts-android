package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AccountFragmentViewModel @Inject constructor(
    userManager: UserManager,
    private val syncServerManager: SyncServerManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val signInState = LiveDataReactiveStreams.fromPublisher(userManager.getSignInState())

    fun signInWithGoogle(onSignInResult: (IntentSenderRequest) -> Unit) {
        viewModelScope.launch {
            val signInClient = Identity.getSignInClient(context)
            val signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                    BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        // use the Google Cloud credentials OAuth Server Client ID, not the Android Client ID.
                        .setServerClientId(Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID)
                        // Todo is this right? only show accounts previously used to sign in.
                        .setFilterByAuthorizedAccounts(false)
                        .build()
                )
                .build()

            try {
                val result = signInClient.beginSignIn(signInRequest).await()

                // Now construct the IntentSenderRequest the launcher requires
                val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent).build()
                onSignInResult(intentSenderRequest)
            } catch (e: Exception) {
                // No saved credentials found. Launch the One Tap sign-up flow, or
                // do nothing and continue presenting the signed-out UI.
                Timber.e(e, "Unable to sign in with Google")
            }
        }
    }

    fun signInWithGoogleResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) {
            if (result.data?.action == ActivityResultContracts.StartIntentSenderForResult.ACTION_INTENT_SENDER_REQUEST) {
                // TODO remove this deprecation
                @Suppress("DEPRECATION")
                val exception = result.data?.getSerializableExtra(ActivityResultContracts.StartIntentSenderForResult.EXTRA_SEND_INTENT_EXCEPTION) as? Exception
                Timber.e(exception, "Google Sign-In failed.")
            } else {
                Timber.e("Google Sign-In failed.")
            }
            return
        }
        val oneTapClient = Identity.getSignInClient(context)
        val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
        val idToken = credential.googleIdToken
        if (idToken != null) {
            // Got an ID token from Google. Use it to authenticate
            // with your backend.
            Timber.i("GOOGLE_SIGN_IN $idToken")
        } else {
            Timber.i("GOOGLE_SIGN_IN Null Token")
        }
    }
}

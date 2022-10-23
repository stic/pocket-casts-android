package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.AccountAuth
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AccountFragmentViewModel @Inject constructor(
    userManager: UserManager,
    private val accountAuth: AccountAuth,
    private val syncServerManager: SyncServerManager,
    settings: Settings,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val signInState = LiveDataReactiveStreams.fromPublisher(userManager.getSignInState())

    private val isGooglePlayServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    private val isFeatureFlagSingleSignOnEnabled = settings.isFeatureFlagSingleSignOnEnabled()
    val showContinueWithGoogleButton = true // isGooglePlayServicesAvailable && isFeatureFlagSingleSignOnEnabled

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
            val exception = if (result.data?.action == ActivityResultContracts.StartIntentSenderForResult.ACTION_INTENT_SENDER_REQUEST) {
                // TODO remove this deprecation
                @Suppress("DEPRECATION")
                result.data?.getSerializableExtra(ActivityResultContracts.StartIntentSenderForResult.EXTRA_SEND_INTENT_EXCEPTION) as? Exception
            } else {
                null
            }

            LogBuffer.e(LogBuffer.TAG_SINGLE_SIGN_ON, exception, "Google Sign-In failed.")
            return
        }
        val oneTapClient = Identity.getSignInClient(context)
        val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
        val token = credential.googleIdToken
        if (token == null) {
            LogBuffer.e(LogBuffer.TAG_SINGLE_SIGN_ON, "Google Sign-In token null.")
            return
        }
        viewModelScope.launch {
            // val result =
            accountAuth.signInWithGoogleToken(token)
//            when (result) {
//                is AccountAuth.AuthResult.Success -> {
//                    signInState.postValue(SignInState.Success)
//                }
//                is AccountAuth.AuthResult.Failed -> {
//                    val message = result.message
//                    val errors = mutableSetOf(SignInError.SERVER)
//                    signInState.postValue(SignInState.Failure(errors, message))
//                }
//            }
        }
    }
}

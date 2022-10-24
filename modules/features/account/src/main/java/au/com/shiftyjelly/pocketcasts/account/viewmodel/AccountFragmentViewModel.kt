package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.content.Context
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.AccountAuth
import au.com.shiftyjelly.pocketcasts.account.BuildConfig
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    val showContinueWithGoogleButton = BuildConfig.DEBUG || (isGooglePlayServicesAvailable && isFeatureFlagSingleSignOnEnabled)

    fun beginSignInGoogleOneTap(
        googleOneTapSignInLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
        googleLegacySignInLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>
    ) {
        viewModelScope.launch {
            // try to sign in with Google One Tap
            try {
                val beginSignInRequest = BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // use the Google Cloud credentials OAuth Server Client ID, not the Android Client ID.
                    .setServerClientId(Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID)
                    // don't just show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(false)
                    .build()
                val signInRequest = BeginSignInRequest.builder()
                    .setGoogleIdTokenRequestOptions(beginSignInRequest)
                    .build()
                val result = Identity.getSignInClient(context).beginSignIn(signInRequest).await()
                val intentRequest = IntentSenderRequest.Builder(result.pendingIntent).build()
                googleOneTapSignInLauncher.launch(intentRequest)
            } catch (e: Exception) {
                LogBuffer.e(LogBuffer.TAG_SINGLE_SIGN_ON, e, "Unable to sign in with Google One Tap")
                // it's common for the One Tap to fail so try the legacy Google Sign-In
                beginSignInGoogleLegacy(googleLegacySignInLauncher)
            }
        }
    }

    private suspend fun beginSignInGoogleLegacy(googleLegacySignInLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>) {
        val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(context)
        val token = lastSignedInAccount?.idToken
        if (token == null) {
            try {
                val request = GetSignInIntentRequest.builder()
                    .setServerClientId(Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID)
                    .build()
                val signInIntent = Identity.getSignInClient(context).getSignInIntent(request).await()
                val intentSenderRequest = IntentSenderRequest.Builder(signInIntent.intentSender).build()
                googleLegacySignInLauncher.launch(intentSenderRequest)
            } catch (e: Exception) {
                LogBuffer.e(LogBuffer.TAG_SINGLE_SIGN_ON, e, "Unable to sign in with Google Legacy")
            }
        } else {
            signInWithGoogleToken(token)
        }
    }

    private suspend fun signInWithGoogleToken(token: String) {
        accountAuth.signInWithGoogleToken(token)
    }

    fun onGoogleOneTapSignInResult(result: ActivityResult, googleLegacySignInLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>) {
        viewModelScope.launch {
            try {
                onGoogleSignInResult(result)
            } catch (e: Exception) {
                if (e is ApiException && e.statusCode == CommonStatusCodes.CANCELED) {
                    // user declined to sign in
                    return@launch
                }
                LogBuffer.e(LogBuffer.TAG_SINGLE_SIGN_ON, e, "Unable to get sign in credentials from Google One Tap result.")
                beginSignInGoogleLegacy(googleLegacySignInLauncher)
            }
        }
    }

    fun onGoogleLegacySignInResult(result: ActivityResult) {
        viewModelScope.launch {
            try {
                onGoogleSignInResult(result)
            } catch (e: Exception) {
                LogBuffer.e(LogBuffer.TAG_SINGLE_SIGN_ON, e, "Unable to get sign in credentials from Google Legacy result.")
            }
        }
    }

    private suspend fun onGoogleSignInResult(result: ActivityResult) {
        val credential = Identity.getSignInClient(context).getSignInCredentialFromIntent(result.data)
        val token = credential.googleIdToken ?: throw Exception("Unable to sign in because no token was returned.")
        signInWithGoogleToken(token)
    }
}

package au.com.shiftyjelly.pocketcasts.account

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.localization.helper.LocaliseHelper
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsThread
import au.com.shiftyjelly.pocketcasts.servers.ServerCallback
import au.com.shiftyjelly.pocketcasts.servers.ServerManager
import au.com.shiftyjelly.pocketcasts.servers.model.AuthResultModel
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager.*
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenResponse
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Singleton
class AccountAuth @Inject constructor(
    private val settings: Settings,
    private val serverManager: ServerManager,
    private val syncServerManager: SyncServerManager,
    private val podcastManager: PodcastManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val KEY_SIGN_IN_SOURCE = "sign_in_source"
        private const val KEY_ERROR_CODE = "error_code"
    }

    suspend fun signInWithEmailAndPassword(email: String, password: String, signInSource: SignInSource): String {
        val analyticsProperties = mapOf(KEY_SIGN_IN_SOURCE to signInSource.analyticsValue)
        try {
            val response = syncServerManager.loginPocketCasts(email = email, password = password)
            analyticsTracker.track(AnalyticsEvent.USER_SIGNED_IN, analyticsProperties)
            signInSuccessful(
                userUuid = response.uuid,
                email = response.email,
                refreshToken = response.refreshToken,
                accessToken = response.accessToken,
                clientId = "pocketcasts"
            )
        }
        catch (e: Exception) {
            analyticsTracker.track(AnalyticsEvent.USER_SIGNIN_FAILED, analyticsProperties)
            throw e
        }
    }

    suspend fun registerWithEmailAndPassword(email: String, password: String): String {
        // Todo
        try {
            val resource = syncServerManager.userRegister(email = email, password = password)
            if (resource is SyncServerResponse.Success) {

                analyticsTracker.track(AnalyticsEvent.USER_ACCOUNT_CREATED)
            }
            else {

            }

        }
        catch (e: Exception) {
//            val message = LocaliseHelper.serverMessageIdToMessage(serverMessageId, ::getResourceString)
//                ?: userMessage
//                ?: getResourceString(LR.string.error_login_failed)
//
//            val errorCodeValue = serverMessageId ?: TracksAnalyticsTracker.INVALID_OR_NULL_VALUE
//            analyticsTracker.track(AnalyticsEvent.USER_ACCOUNT_CREATION_FAILED, mapOf(KEY_ERROR_CODE to errorCodeValue))
        }
    }

    suspend fun signInWithGoogleToken(email: String, idToken: String) {
        val clientId = Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID
        val response = tokenUsingAuthorizationCode(authorizationCode = idToken, clientId = clientId)
        val userUuid = syncServerManager.userUuid()
        signInSuccessful(
            userUuid = userUuid,
            email = email,
            refreshToken = response.refreshToken,
            accessToken = response.accessToken,
            clientId = clientId
        )
    }

    suspend fun tokenUsingAuthorizationCode(authorizationCode: String, clientId: String): TokenResponse {
        return syncServerManager.tokenUsingAuthorizationCode(code = authorizationCode, clientId = clientId)
    }

    suspend fun tokenUsingRefreshToken(refreshToken: String, clientId: String): TokenResponse {
        return syncServerManager.tokenUsingRefreshToken(refreshToken, clientId)
    }

    suspend fun createUserWithEmailAndPassword(email: String, password: String): AuthResult {
        return withContext(Dispatchers.IO) {
            val authResult = registerWithSyncServer(email, password)
            if (authResult is AuthResult.Success) {
                signInSuccessful(email, password, authResult.result)
            }
            authResult
        }
    }

    private suspend fun loginToSyncServer(email: String, password: String): AuthResult {
        return suspendCoroutine { continuation ->
            serverManager.loginToSyncServer(
                email, password,
                object : ServerCallback<AuthResultModel> {
                    override fun dataReturned(result: AuthResultModel?) {
                        continuation.resume(AuthResult.Success(result))
                    }

                    override fun onFailed(
                        errorCode: Int,
                        userMessage: String?,
                        serverMessageId: String?,
                        serverMessage: String?,
                        throwable: Throwable?
                    ) {
                        val message = LocaliseHelper.serverMessageIdToMessage(serverMessageId, ::getResourceString)
                            ?: userMessage
                            ?: getResourceString(LR.string.error_login_failed)
                        continuation.resume(
                            AuthResult.Failed(
                                message = message,
                                serverMessageId = serverMessageId
                            )
                        )
                    }
                }
            )
        }
    }

    fun resetPasswordWithEmail(email: String, complete: (AuthResult) -> Unit) {
        serverManager.forgottenPasswordToSyncServer(
            email,
            object : ServerCallback<String> {
                override fun dataReturned(result: String?) {
                    complete(AuthResult.Success(null))
                    analyticsTracker.track(AnalyticsEvent.USER_PASSWORD_RESET)
                }

                override fun onFailed(
                    errorCode: Int,
                    userMessage: String?,
                    serverMessageId: String?,
                    serverMessage: String?,
                    throwable: Throwable?
                ) {
                    val message = LocaliseHelper.serverMessageIdToMessage(serverMessageId, ::getResourceString)
                        ?: userMessage
                        ?: getResourceString(LR.string.profile_reset_password_failed)
                    complete(
                        AuthResult.Failed(
                            message = message,
                            serverMessageId = serverMessageId
                        )
                    )
                }
            }
        )
    }

    private suspend fun signInSuccessful(userUuid: String, email: String, refreshToken: String?, accessToken: String, clientId: String) {
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Signed in successfully to $clientId")
        // Store details in Android Account Manager
        if (refreshToken != null) {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Saving $email to account manager")
            val account = Account(email, AccountConstants.ACCOUNT_TYPE)
            val accountManager = AccountManager.get(context)
            accountManager.addAccountExplicitly(account, refreshToken, null)
            accountManager.setAuthToken(account, AccountConstants.TOKEN_TYPE, accessToken)
            accountManager.setUserData(account, AccountConstants.UUID, userUuid)

            settings.setUsedAccountManager(true)
        } else {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Sign in marked as successful but we didn't get a token back.")
        }

        settings.setLastModified(null)
        RefreshPodcastsThread.clearLastRefreshTime()
        podcastManager.markAllPodcastsUnsynced()
        podcastManager.refreshPodcasts("login")
    }

    private fun getResourceString(stringId: Int): String {
        return context.resources.getString(stringId)
    }

    sealed class AuthResult {
        data class Success(val result: AuthResultModel?) : AuthResult()
        data class Failed(val message: String, val serverMessageId: String?) : AuthResult()
    }
}

enum class SignInSource(val analyticsValue: String) {
    AccountAuthenticator("account_manager"),
    SignInViewModel("sign_in_view_model"),
    PocketCastsApplication("pocketcasts_application"),
}

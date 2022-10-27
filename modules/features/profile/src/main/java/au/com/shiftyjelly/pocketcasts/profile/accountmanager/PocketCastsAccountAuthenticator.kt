package au.com.shiftyjelly.pocketcasts.profile.accountmanager

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import au.com.shiftyjelly.pocketcasts.account.AccountActivity
import au.com.shiftyjelly.pocketcasts.account.AccountAuth
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenResponse
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import timber.log.Timber

class PocketCastsAccountAuthenticator(val context: Context, private val accountAuth: AccountAuth) : AbstractAccountAuthenticator(context) {
    override fun getAuthTokenLabel(authTokenType: String?): String {
        return AccountConstants.TOKEN_TYPE
    }

    override fun confirmCredentials(response: AccountAuthenticatorResponse?, account: Account?, options: Bundle?): Bundle {
        return Bundle()
    }

    override fun updateCredentials(response: AccountAuthenticatorResponse?, account: Account?, authTokenType: String?, options: Bundle?): Bundle {
        return Bundle()
    }

    /**
     * Update the user's access token using the refresh token.
     */
    override fun getAuthToken(response: AccountAuthenticatorResponse?, account: Account?, authTokenType: String?, options: Bundle?): Bundle {
        val accountManager = AccountManager.get(context)
        var accessToken = accountManager.peekAuthToken(account, authTokenType)
        if (accessToken.isNullOrEmpty() && account != null) {
            runBlocking {
                try {
                    val refreshToken = accountManager.getPassword(account)
                    val clientId = accountManager.getUserData(account, AccountConstants.CLIENT_ID) ?: AccountConstants.CLIENT_ID_POCKET_CASTS

                    val tokenResponse = try {
                        accountAuth.tokenUsingRefreshToken(refreshToken = refreshToken, clientId = clientId)
                    } catch (e: HttpException) {
                        if (e.code() == 401 && clientId == AccountConstants.CLIENT_ID_POCKET_CASTS) {
                            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Failed to refresh token, trying legacy method.")
                            getAuthTokenLegacy(email = account.name, password = refreshToken)
                        } else {
                            throw e
                        }
                    }

                    Timber.d("Successfully refreshed access token.")
                    accessToken = tokenResponse.accessToken
                    if (tokenResponse.refreshToken != null) {
                        accountManager.setPassword(account, tokenResponse.refreshToken)
                    }

                    return@runBlocking bundleOf(
                        AccountManager.KEY_ACCOUNT_NAME to account.name,
                        AccountManager.KEY_ACCOUNT_TYPE to account.type,
                        AccountManager.KEY_AUTHTOKEN to accessToken
                    )
                } catch (e: Exception) {
                    LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Failed to refresh token.")
                }
            }
        }

        // Could not get auth token so display sign in sign up screens
        val intent = Intent(context, AccountActivity::class.java).apply {
            putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        }
        return bundleOf(AccountManager.KEY_INTENT to intent)
    }

    private suspend fun getAuthTokenLegacy(email: String, password: String): TokenResponse {
        val response = accountAuth.authorizeWithEmailAndPassword(email = email, password = password)
        return accountAuth.tokenUsingAuthorizationCode(response.code)
    }

    override fun hasFeatures(response: AccountAuthenticatorResponse?, account: Account?, features: Array<out String>?): Bundle {
        return Bundle()
    }

    override fun editProperties(response: AccountAuthenticatorResponse?, accountType: String?): Bundle {
        return Bundle()
    }

    override fun addAccount(response: AccountAuthenticatorResponse?, accountType: String?, authTokenType: String?, requiredFeatures: Array<out String>?, options: Bundle?): Bundle {
        val intent = Intent(context, AccountActivity::class.java)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        return bundleOf(AccountManager.KEY_INTENT to intent)
    }
}

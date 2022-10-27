package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokenRequest(
    @field:Json(name = "grant_type") val grantType: String, // either 'refresh_token' or 'authorization_code'
    @field:Json(name = "code") val code: String?,
    @field:Json(name = "refresh_token") val refreshToken: String?,
    @field:Json(name = "client_id") val clientId: String?
) {

    companion object {
        fun buildAuthorizationRequest(code: String, clientId: String) = TokenRequest(
            grantType = "authorization_code",
            code = code,
            refreshToken = null,
            clientId = clientId
        )

        fun buildRefreshRequest(refreshToken: String, clientId: String) = TokenRequest(
            grantType = "refresh_token",
            code = null,
            refreshToken = refreshToken,
            clientId = clientId
        )
    }
}

package au.com.shiftyjelly.pocketcasts.servers.sync.login

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginGoogleResponse(
    @field:Json(name = "email") val email: String,
    @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "is_new") val isNew: Boolean,
    @field:Json(name = "access_token") val accessToken: String,
    @field:Json(name = "token_type") val tokenType: String,
    @field:Json(name = "refresh_token") val refreshToken: String,
    @field:Json(name = "expires_in") val expiresIn: Long
)

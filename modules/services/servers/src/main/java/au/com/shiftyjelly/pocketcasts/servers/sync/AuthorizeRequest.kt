package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthorizeRequest(
    @field:Json(name = "response_type") val responseType: String = "code",
    @field:Json(name = "client_id") val clientId: String,
    @field:Json(name = "state") val state: String,
    @field:Json(name = "email") val email: String,
    @field:Json(name = "password") val password: String
)

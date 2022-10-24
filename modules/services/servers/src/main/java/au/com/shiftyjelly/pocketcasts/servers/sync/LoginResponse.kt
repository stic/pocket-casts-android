package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @field:Json(name = "token") val token: String? = null,
    @field:Json(name = "uuid") val uuid: String? = null,
    @field:Json(name = "email") val email: String? = null
)

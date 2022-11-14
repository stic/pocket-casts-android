package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserRegisterResponse(
    @field:Json(name = "success") val success: Boolean,
    @field:Json(name = "message") val message: String?,
    @field:Json(name = "token") val token: String,
    @field:Json(name = "uuid") val uuid: String
)

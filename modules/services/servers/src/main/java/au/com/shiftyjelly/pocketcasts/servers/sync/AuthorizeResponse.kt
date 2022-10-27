package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthorizeResponse(
    @field:Json(name = "code") val code: String,
    @field:Json(name = "state") val state: String
)

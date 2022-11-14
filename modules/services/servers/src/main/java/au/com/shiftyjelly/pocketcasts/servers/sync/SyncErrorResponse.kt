package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncErrorResponse(
    @field:Json(name = "errorMessage") val errorMessage: String,
    @field:Json(name = "errorMessageId") val errorMessageId: String
)

package au.com.shiftyjelly.pocketcasts.servers.sync

import android.content.Context
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.localization.helper.LocaliseHelper
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import retrofit2.HttpException
import timber.log.Timber

fun HttpException.parseErrorMessage(): String? {
    val errorBody = this.response()?.errorBody()
    if (errorBody != null) {
        val errorMoshi = Moshi.Builder().build()

        try {
            val response = errorMoshi.adapter(PCErrorBody::class.java).fromJson(errorBody.source())
            if (response != null) {
                return response.message
            }
        } catch (e: Exception) {
            return null
        }
    }

    return null
}

fun HttpException.parseErrorMessageLocalized(context: Context): String? {
    val response = this.response() ?: return null
    var message: String? = null
    val errorBody = response.errorBody()
    if (errorBody != null) {
        try {
            val errorMoshi = Moshi.Builder().build()
            val errorResponse = errorMoshi.adapter(PCErrorBody::class.java).fromJson(errorBody.source())
            if (errorResponse != null) {
                message = LocaliseHelper.serverMessageIdToMessage(errorResponse.messageId) { stringId -> context.resources.getString(stringId) }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
    return message ?: response.message()
}

@JsonClass(generateAdapter = true)
data class PCErrorBody(
    @field:Json(name = "errorMessage") val message: String,
    @field:Json(name = "errorMessageId") val messageId: String?
)

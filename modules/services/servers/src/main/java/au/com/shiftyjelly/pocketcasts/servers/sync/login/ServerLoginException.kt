package au.com.shiftyjelly.pocketcasts.servers.sync.login

data class ServerLoginException(
    val errorMessageId: String?,
    var errorMessage: String?
) : Exception(errorMessage ?: "")

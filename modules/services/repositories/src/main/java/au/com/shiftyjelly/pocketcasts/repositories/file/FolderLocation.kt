package au.com.shiftyjelly.pocketcasts.repositories.file

import au.com.shiftyjelly.pocketcasts.preferences.model.StorageChoiceSetting

data class FolderLocation(
    val filePath: String,
    val label: String,
    val analyticsLabel: String,
) {
    fun toStorageChoiceSetting(): StorageChoiceSetting =
        StorageChoiceSetting(filePath, label)
}

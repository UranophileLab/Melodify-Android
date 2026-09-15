package dev.melodify.uranophilelab.model.history

data class AlbumHistoryItem(
    val id: String?,
    val title: String?,
    val subtitle: String?,
    val imageUrl: String?,
    val timestamp: Long = System.currentTimeMillis()
)

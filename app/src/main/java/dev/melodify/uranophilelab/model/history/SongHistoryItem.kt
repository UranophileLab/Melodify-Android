package dev.melodify.uranophilelab.model.history

data class SongHistoryItem(
    val id: String?,
    val title: String?,
    val artist: String?,
    val imageUrl: String?,
    val timestamp: Long = System.currentTimeMillis()
)

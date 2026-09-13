package dev.melodify.uranophilelab.records

import dev.melodify.uranophilelab.records.SongResponse.Song


data class ArtistAllSongs(
    val success: Boolean,
    val data: Data?
) {
    
    data class Data(
        val total: Int,
        val songs: MutableList<Song?>?
    )
}

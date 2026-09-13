package dev.melodify.uranophilelab.model

import dev.melodify.uranophilelab.utils.TextParserUtil


data class AlbumItem(
    val albumTitle: String?,
    val albumSubTitle: String?,
    val albumCover: String?,
    val id: String?
) {
    fun albumTitle(): String {
        return TextParserUtil.parseHtmlText(albumTitle)
    }

    fun albumSubTitle(): String {
        return TextParserUtil.parseHtmlText(albumSubTitle)
    }
}

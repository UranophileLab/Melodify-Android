package dev.melodify.uranophilelab.model

import dev.melodify.uranophilelab.utils.TextParserUtil


data class ArtistItem(
    val name: String?,
    val image: String?,
    val id: String?
) {
    fun name(): String {
        return TextParserUtil.parseHtmlText(name)
    }
}

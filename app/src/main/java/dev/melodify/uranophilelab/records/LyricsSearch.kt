package dev.melodify.uranophilelab.records

import dev.melodify.uranophilelab.utils.TextParserUtil


data class LyricsSearch(
    val success: Boolean,
    val data: Data?
) {
    
    data class Data(
        val lyrics: String?,
        val copyright: String?,
        val snippet: String?
    ) {
        fun lyrics(): String {
            return TextParserUtil.parseHtmlText(lyrics)
        }
    }
}

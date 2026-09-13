package dev.melodify.uranophilelab.model

import dev.melodify.uranophilelab.utils.TextParserUtil


data class BasicDataRecord(
    val id: String?,
    val title: String?,
    val subtitle: String?,
    val image: String?
) {
    fun title(): String {
        return TextParserUtil.parseHtmlText(title)
    }

    fun subtitle(): String {
        return TextParserUtil.parseHtmlText(subtitle)
    }
}

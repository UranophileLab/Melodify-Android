package dev.melodify.uranophilelab.utils

import androidx.core.text.HtmlCompat

object TextParserUtil {
    /**
     * @param htmlText HTML text to be parsed
     * @return Parsed text
     */
    fun parseHtmlText(htmlText: String?): String {
        return HtmlCompat.fromHtml(htmlText ?: "", HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    }
}

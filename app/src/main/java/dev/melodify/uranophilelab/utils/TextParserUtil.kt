package dev.melodify.uranophilelab.utils

import android.util.LruCache
import androidx.core.text.HtmlCompat

object TextParserUtil {
    private val htmlCache = LruCache<String, String>(500)

    /**
     * @param htmlText HTML text to be parsed
     * @return Parsed text
     */
    fun parseHtmlText(htmlText: String?): String {
        if (htmlText.isNullOrEmpty()) return ""

        // Fast-path: If string has no HTML entities or tags, return directly
        if (!htmlText.contains('&') && !htmlText.contains('<')) {
            return htmlText
        }

        val cached = htmlCache.get(htmlText)
        if (cached != null) {
            return cached
        }

        val parsed = HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        htmlCache.put(htmlText, parsed)
        return parsed
    }
}

package com.stocknews.app

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Streaming RSS 2.0 / Atom parser. Tolerates the schema variations seen across
 * Bloomberg's Google News bridge, Investing.com's RSS, and Yahoo Finance's RSS.
 */
class RssParser {

    fun parse(input: InputStream, source: NewsSource): List<NewsArticle> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(input, null)
        }

        val articles = mutableListOf<NewsArticle>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                val name = parser.name
                if (name.equals("item", ignoreCase = true) ||
                    name.equals("entry", ignoreCase = true)
                ) {
                    readItem(parser, source)?.let(articles::add)
                }
            }
            event = parser.next()
        }
        return articles
    }

    private fun readItem(parser: XmlPullParser, source: NewsSource): NewsArticle? {
        var title: String? = null
        var link: String? = null
        var pubDate: String? = null
        var description: String? = null

        val endTag = parser.name
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.END_TAG -> if (parser.name.equals(endTag, ignoreCase = true)) {
                    val finalTitle = title?.trim().orEmpty()
                    val finalLink = link?.trim().orEmpty()
                    if (finalTitle.isEmpty() || finalLink.isEmpty()) return null
                    return NewsArticle(
                        title = decodeEntities(finalTitle),
                        link = finalLink,
                        source = source,
                        publishedAt = parseDate(pubDate),
                        description = description?.let { decodeEntities(stripHtml(it)).trim() }
                            ?.takeIf { it.isNotEmpty() }
                    )
                }
                XmlPullParser.START_TAG -> when (parser.name.lowercase(Locale.US)) {
                    "title" -> title = readText(parser)
                    "link" -> {
                        // Atom feeds use <link href="..."/>, RSS uses <link>...</link>.
                        val href = parser.getAttributeValue(null, "href")
                        link = if (!href.isNullOrBlank()) {
                            skipToEndTag(parser)
                            href
                        } else {
                            readText(parser)
                        }
                    }
                    "pubdate", "published", "updated", "dc:date" -> pubDate = readText(parser)
                    "description", "summary", "content" -> description = readText(parser)
                }
            }
        }
        return null
    }

    private fun readText(parser: XmlPullParser): String {
        val sb = StringBuilder()
        val depth = parser.depth
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> sb.append(parser.text)
                XmlPullParser.END_TAG -> if (parser.depth <= depth) return sb.toString()
            }
        }
        return sb.toString()
    }

    private fun skipToEndTag(parser: XmlPullParser) {
        val depth = parser.depth
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.END_TAG && parser.depth <= depth) return
        }
    }

    private fun parseDate(raw: String?): Date? {
        if (raw.isNullOrBlank()) return null
        for (format in DATE_FORMATS) {
            try {
                return format.parse(raw.trim())
            } catch (_: Exception) {
                // try next format
            }
        }
        return null
    }

    private fun stripHtml(input: String): String =
        input.replace(HTML_TAG_REGEX, "").replace(WHITESPACE_REGEX, " ")

    private fun decodeEntities(input: String): String = input
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")

    companion object {
        private val HTML_TAG_REGEX = Regex("<[^>]+>")
        private val WHITESPACE_REGEX = Regex("\\s+")

        private val DATE_FORMATS: List<SimpleDateFormat> = listOf(
            // RFC 822 — used by RSS 2.0
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
            SimpleDateFormat("dd MMM yyyy HH:mm:ss zzz", Locale.US),
            // ISO 8601 — used by Atom
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
        )
    }
}

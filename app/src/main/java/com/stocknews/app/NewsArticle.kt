package com.stocknews.app

import java.util.Date

data class NewsArticle(
    val title: String,
    val link: String,
    val source: NewsSource,
    val publishedAt: Date?,
    val description: String?
)

enum class NewsSource(val displayName: String, val feedUrl: String) {
    BLOOMBERG(
        "Bloomberg",
        "https://news.google.com/rss/search?q=site:bloomberg.com+stock+market&hl=en-US&gl=US&ceid=US:en"
    ),
    INVESTING(
        "Investing.com",
        "https://www.investing.com/rss/news_25.rss"
    ),
    YAHOO_FINANCE(
        "Yahoo Finance",
        "https://finance.yahoo.com/news/rssindex"
    )
}

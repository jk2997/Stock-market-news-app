package com.stocknews.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class NewsRepository(
    private val client: OkHttpClient = defaultClient(),
    private val parser: RssParser = RssParser()
) {

    /**
     * Fetches all configured feeds in parallel and returns a single list sorted
     * newest-first. Failed sources are reported via [SourceError] but do not
     * cause the whole load to fail — the user still sees whatever did load.
     */
    suspend fun loadHeadlines(): NewsResult = coroutineScope {
        val deferred = NewsSource.values().map { source ->
            async(Dispatchers.IO) { source to runCatching { fetch(source) } }
        }

        val articles = mutableListOf<NewsArticle>()
        val errors = mutableListOf<SourceError>()
        deferred.awaitAll().forEach { (source, result) ->
            result.fold(
                onSuccess = { articles += it },
                onFailure = { errors += SourceError(source, it.message ?: it.javaClass.simpleName) }
            )
        }

        val sorted = articles.sortedByDescending { it.publishedAt?.time ?: 0L }
        NewsResult(sorted, errors)
    }

    private suspend fun fetch(source: NewsSource): List<NewsArticle> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(source.feedUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} from ${source.displayName}")
            }
            val body = response.body ?: throw IllegalStateException("Empty body from ${source.displayName}")
            body.byteStream().use { stream -> parser.parse(stream, source) }
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) StockMarketNews/1.0"

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}

data class NewsResult(
    val articles: List<NewsArticle>,
    val errors: List<SourceError>
)

data class SourceError(val source: NewsSource, val message: String)

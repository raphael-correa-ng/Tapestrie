package com.rcs.htmlcrawlerdemo

import com.rcs.trie.Trie
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ExecutorService

class HtmlCrawler(
    private val baseUrl: String,
    private val htmlTokenizer: HtmlTokenizer,
    private val htmlUrlFinder: HtmlUrlFinder,
    private val htmlClient: HtmlClient,
    private val executorService: ExecutorService
) {

    fun crawlAndIndex(): Trie<ConcurrentLinkedDeque<HtmlIndexEntry>> {
        println("Initializing crawler with baseURL=${this.baseUrl}")

        val currentTimeMillis = System.currentTimeMillis()
        val trie: Trie<ConcurrentLinkedDeque<HtmlIndexEntry>> = Trie()
        val pagesIndexed = crawl("", ConcurrentHashMap(), trie)
        val durationMillis = System.currentTimeMillis() - currentTimeMillis

        println("Done initializing crawler; " +
                "indexed $pagesIndexed HTML pages and ${trie.size} unique tokens; " +
                "took ${durationMillis}ms")

        return trie
    }

    private fun crawl(
        relativeUrl: String,
        visited: ConcurrentHashMap<String, Boolean?>,
        trie: Trie<ConcurrentLinkedDeque<HtmlIndexEntry>>
    ): Int {
        // Use putIfAbsent to check and mark the URL atomically
        if (visited.putIfAbsent(relativeUrl, true) != null) {
            return 0 // URL already visited
        }

        val htmlContent: String
        try {
            htmlContent = htmlClient.getAsString(baseUrl + relativeUrl)
        } catch (e: IOException) {
            e.printStackTrace()
            println("Error fetching ${baseUrl + relativeUrl} - not indexing page")
            return 0
        }

        indexPage(relativeUrl, htmlContent, trie)

        val newCounts = htmlUrlFinder.findRelativeUrls(htmlContent)
            .map { u -> fixUrl("/$relativeUrl", u) }
            .map { u -> executorService.submit<Int> { crawl(u, visited, trie) } }
            .sumOf { it.get() }

        return 1 + newCounts
    }

    private fun fixUrl(relativeUrl: String, additionalPath: String): String {
        var fixedUrl = additionalPath
        var prefixUrl = relativeUrl.substring(0, relativeUrl.lastIndexOf("/"))
        while (fixedUrl.startsWith("../")) {
            fixedUrl = fixedUrl.replace("../", "")
            prefixUrl = prefixUrl.substring(0, prefixUrl.lastIndexOf("/"))
        }
        return prefixUrl + fixedUrl
    }

    private fun indexPage(relativeUrl: String, htmlContent: String, trie: Trie<ConcurrentLinkedDeque<HtmlIndexEntry>>) {
        println("Indexing ${baseUrl + relativeUrl}")

        htmlTokenizer.tokenize(htmlContent)
            .forEach { entry ->
                val token = entry.key
                val occurrences = entry.value
                val indexEntries = trie.getExactly(token) ?: ConcurrentLinkedDeque()
                // stores only relative URLs in order to minimize storage space
                // the full URL must then be reconstructed on retrieval!
                val newEntry = HtmlIndexEntry(relativeUrl, occurrences)
                indexEntries.add(newEntry)
                indexEntries.sortedBy { it.occurrences }
                trie.put(token, indexEntries)
            }
    }
}
package com.rcs.htmlcrawlerdemo

import com.rcs.trie.FuzzySubstringMatchingStrategy
import com.rcs.trie.Trie
import java.io.IOException
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

class HtmlCrawler(
    private val baseUrl: String,
    private val htmlTokenizer: HtmlTokenizer,
    private val htmlUrlFinder: HtmlUrlFinder,
    private val htmlClient: HtmlClient,
    private val executorService: ExecutorService
) {

    private val trie: Trie<LinkedList<HtmlIndexEntry>> = Trie() // maps a token to a set of URLs where the token can be found

    private var initialized = false

    fun init() {
        println("Initializing crawler with baseURL=${this.baseUrl}")

        val currentTimeMillis = System.currentTimeMillis()

        var counts: Pair<Int, Int>

        // synchronization prevents searching while crawling/indexing
        synchronized(this.trie) {
            trie.clear()
            counts = crawl("", ConcurrentHashMap())
        }

        val durationMillis = System.currentTimeMillis() - currentTimeMillis

        println("Done initializing crawler; " +
                "indexed ${counts.first} HTML pages and ${counts.second} unique tokens; took $durationMillis ms")

        this.initialized = true
    }

    fun search(searchRequest: SearchRequest): List<String> {
        if (!initialized) {
            throw IllegalStateException("Crawler has not been initialized; call HtmlCrawler.init() first")
        }

        val normalizedKeyword = searchRequest.normalizedKeyword()

        var resultsWithoutBaseUrl: Collection<HtmlIndexEntry>

        // synchronization prevents searching while crawling/indexing
        synchronized(this.trie) {
            resultsWithoutBaseUrl = when(searchRequest.strategy) {
                SearchStrategy.EXACT -> {
                    trie.getExactly(normalizedKeyword) ?: setOf()
                }
                SearchStrategy.SUBSTRING -> {
                    trie.matchBySubstring(normalizedKeyword)
                        .flatMap { it.value }
                }
                SearchStrategy.FUZZY -> {
                    // for this large data set, it's better to limit the
                    // fuzzy strategy to the more restrictive MATCH_PREFIX
                    // other strategies will still work, but will be slow
                    trie.matchBySubstringFuzzy(normalizedKeyword, 2, FuzzySubstringMatchingStrategy.EXACT_PREFIX)
                        .flatMap { it.value }
                }
            }
        }

        return resultsWithoutBaseUrl
            .map { it.url }
            .map { relativeUrl: String -> baseUrl + relativeUrl }
            .toList()
    }

    /**
     * returns
     * Pair.first = number of pages indexed
     * Pair.second = number of unique words indexed
     */
    private fun crawl(relativeUrl: String, visited: ConcurrentHashMap<String, Boolean?>): Pair<Int, Int> {
        // Use putIfAbsent to check and mark the URL atomically
        if (visited.putIfAbsent(relativeUrl, true) != null) {
            return Pair(0, 0) // URL already visited
        }

        val htmlContent: String
        try {
            htmlContent = htmlClient.getAsString(baseUrl + relativeUrl)
        } catch (e: IOException) {
            println("Error fetching ${baseUrl + relativeUrl} - not indexing page")
            return Pair(0, 0)
        }

        val wordsIndexed = indexPage(relativeUrl, htmlContent)

        val newCounts = htmlUrlFinder.findRelativeUrls(htmlContent)
            .map { u -> fixUrl("/$relativeUrl", u) }
            .map { u -> executorService.submit<Pair<Int, Int>> { crawl(u, visited) } }
            .map { it.get() }
            .fold(Pair(0, 0)) { result, next ->
                Pair(next.first + result.first, next.second + result.second)
            }

        return Pair(1 + newCounts.first, wordsIndexed + newCounts.second)
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

    private fun indexPage(relativeUrl: String, htmlContent: String): Int {
        println("Indexing ${baseUrl + relativeUrl}")

        var newWordsIndexed = 0

        htmlTokenizer.tokenize(htmlContent)
            .forEach { entry ->
                val token = entry.key
                val occurrences = entry.value

                synchronized(token) {
                    val newKeys = trie.getExactly(token) ?: LinkedList()

                    synchronized(newKeys) {
                        if (newKeys.isEmpty()) {
                            newWordsIndexed++
                        }

                        // stores only relative URLs in order to minimize storage space
                        // the full URL must then be reconstructed on retrieval!
                        val newEntry = HtmlIndexEntry(relativeUrl, occurrences)
                        newKeys.add(newEntry)

                        newKeys.sortByDescending { it.occurrences }

                        trie.put(token, newKeys)
                    }
                }
            }

        return newWordsIndexed
    }
}
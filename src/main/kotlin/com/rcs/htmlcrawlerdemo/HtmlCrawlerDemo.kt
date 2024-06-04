package com.rcs.htmlcrawlerdemo

import java.util.concurrent.Executors

fun main() {
    // linux manual pages
    val baseUrl = "https://docs.huihoo.com/linux/man/20100621/"

    val htmlCrawler = HtmlCrawler(
        baseUrl,
        HtmlTokenizer(),
        HtmlUrlFinder(),
        HtmlClient(),
        Executors.newVirtualThreadPerTaskExecutor())

    htmlCrawler.init()
    // Done initializing crawler; indexed 1860 HTML pages and 21181 unique tokens; took 23599ms (or 6108ms reading from cache)

    val start = System.currentTimeMillis()

    val searchRequest = SearchRequest("computer", SearchStrategy.FUZZY)
    val results = htmlCrawler.search(searchRequest)

    println("Search took ${System.currentTimeMillis() - start}ms")

    println(results)
}
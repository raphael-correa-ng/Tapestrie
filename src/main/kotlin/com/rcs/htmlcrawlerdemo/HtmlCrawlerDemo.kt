package com.rcs.htmlcrawlerdemo

import com.google.gson.Gson
import spark.Request
import spark.Response
import spark.Spark.post

fun main() {
    val gson = Gson()

    // linux manual pages
    val baseUrl = "https://docs.huihoo.com/linux/man/20100621/"

    val htmlCrawler = HtmlCrawler(
        baseUrl,
        HtmlTokenizer(),
        HtmlUrlFinder(),
        HtmlClient()
    )

    val htmlSearcher = HtmlSearcher(baseUrl, htmlCrawler)

    htmlSearcher.crawlAndIndex()
    // Done initializing crawler; indexed 1860 HTML pages and 21181 unique tokens; took 23599ms (or 6108ms reading from cache)

    post("/search") { req: Request, res: Response ->
        val searchRequest = gson.fromJson(req.body(), SearchRequest::class.java)
        println("Searching for $searchRequest")
        val start = System.currentTimeMillis()
        val results = htmlSearcher.search(searchRequest)
        println("Search took ${System.currentTimeMillis() - start}ms")
        println("Found ${results.size} hits: ${results.map { it.string }}")
        res.header("Content-Type", "application/json")
        gson.toJson(results)
    }
}
package uy.exchangerates

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking

/** A local server answering with the API's real shapes, so the client is tested over HTTP. */
class RatesClientTest {
    private lateinit var server: HttpServer
    private var lastPath = ""
    private var lastAuth: String? = null
    private var answer: Pair<Int, String> = 404 to "{}"

    @BeforeTest fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { ex ->
            lastPath = ex.requestURI.toString()
            lastAuth = ex.requestHeaders.getFirst("Authorization")
            val bytes = answer.second.toByteArray()
            ex.responseHeaders.add("Content-Type", "application/json")
            ex.sendResponseHeaders(answer.first, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
        server.start()
    }

    @AfterTest fun stop() = server.stop(0)

    private fun client(key: String? = "app-key") = RatesClient("http://127.0.0.1:${server.address.port}/api/", key)

    @Test fun officialWithTheKey() = runBlocking {
        answer = 200 to """{"date":"2026-11-25","day":"2026-11-24","perLocal":{"UYU":{"buy":1,"sell":1},"USD":{"buy":39.5,"sell":41.9}},"published":{},"quote":"UYU","source":"https://www.bcu.gub.uy"}"""
        val bank = client().official("2026-11-25")
        assertEquals("/api/official?date=2026-11-25", lastPath)
        assertEquals("Bearer app-key", lastAuth)
        assertEquals(Spread(39.5, 41.9), bank.perLocal["USD"])
    }

    @Test fun marketAndPesosPerUnit() = runBlocking {
        answer = 200 to """{"date":"2026-11-25","day":"2026-11-24","perUsd":{"USD":1,"UYU":40.5,"EUR":0.81},"base":"USD","credit":{"label":"Rates By Exchange Rate API","href":"https://www.exchangerate-api.com"}}"""
        val market = client().market()
        assertEquals(40.5, market.perLocal()["USD"])
        assertEquals(50.0, market.perLocal()["EUR"]!!, 1e-9)
    }

    @Test fun historyPath() = runBlocking {
        answer = 200 to """{"pair":"USD/UYU","source":"official","from":"2026-11-01","to":"2026-11-30","days":[{"day":"2026-11-24","buy":39.5,"sell":41.9}]}"""
        val days = client().history("usd/uyu", from = "2026-11-01")
        assertEquals("/api/pairs/USD-UYU/history?from=2026-11-01&source=official", lastPath)
        assertEquals(listOf("2026-11-24"), days.map { it.day })
    }

    @Test fun theServersErrors() {
        answer = 403 to """{"error":"this API is for its allowed sites and keys"}"""
        val e = assertFailsWith<RatesException> { runBlocking { client(key = null).pairs() } }
        assertEquals(403, e.status)
        assertEquals("this API is for its allowed sites and keys", e.message)
    }
}

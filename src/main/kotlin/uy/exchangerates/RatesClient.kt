package uy.exchangerates

import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Reads an exchange-rates-uy server (github.com/guillecura/exchange-rates-uy): the bank's and the market's rates, the
 * pairs, a pair's history, a conversion. The same API and names as the server and its JavaScript and Swift clients.
 *
 *     val rates = RatesClient("https://your-server/api", key = "…")
 *     val bank = rates.official()                  // the BCU's pesos per unit, buying and selling
 *     val usd = rates.history("USD/UYU", from = "2026-01-01")
 *
 * Every call suspends and runs on [Dispatchers.IO]. A key in a phone app can be read out of it: it says which app is
 * asking, it does not protect the server.
 */
class RatesClient(
    baseUrl: String,
    private val key: String? = null,
    private val timeoutMillis: Int = 15_000,
) {
    private val base = baseUrl.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true }

    /** The bank's reading on [date] (YYYY-MM-DD, the last on or before it; today by default). */
    suspend fun official(date: String? = null): Official = call("official", mapOf("date" to date))

    /** The market's table on [date]. */
    suspend fun market(date: String? = null): Market = call("market", mapOf("date" to date))

    /** The server's pairs, each with both rates, on [date]. */
    suspend fun pairs(date: String? = null): List<PairRate> = call<PairsAnswer>("pairs", mapOf("date" to date)).pairs

    /** The server's currencies, named in [lang]. */
    suspend fun currencies(lang: String? = null): List<Currency> = call<CurrenciesAnswer>("currencies", mapOf("lang" to lang)).currencies

    /** One pair (`USD/UYU`), day by day, from one source. At most ten years per call. */
    suspend fun history(pair: String, from: String? = null, to: String? = null, source: Source = Source.OFFICIAL): List<HistoryDay> =
        call<HistoryAnswer>(
            "pairs/${pair.uppercase().replace("/", "-")}/history",
            mapOf("from" to from, "to" to to, "source" to source.wire),
        ).days

    /** An amount in another currency. */
    suspend fun convert(
        amount: Double,
        from: String,
        to: String,
        date: String? = null,
        source: Source = Source.MARKET,
        side: Side? = null,
    ): Conversion = call(
        "convert",
        mapOf("amount" to amount.toString(), "from" to from, "to" to to, "date" to date, "source" to source.wire, "side" to side?.wire),
    )

    private suspend inline fun <reified T> call(path: String, query: Map<String, String?>): T = withContext(Dispatchers.IO) {
        val q = query.filterValues { it != null }.entries.sortedBy { it.key }
            .joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, Charsets.UTF_8)}" }
        val connection = URI("$base/$path${if (q.isEmpty()) "" else "?$q"}").toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            connection.setRequestProperty("Accept", "application/json")
            key?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            val status = connection.responseCode
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (status !in 200..299) {
                val said = runCatching { json.decodeFromString<Failure>(body).error }.getOrNull()
                throw RatesException(status, said ?: "HTTP $status")
            }
            json.decodeFromString<T>(body)
        } finally {
            connection.disconnect()
        }
    }

    @Serializable private data class PairsAnswer(val pairs: List<PairRate>)
    @Serializable private data class CurrenciesAnswer(val currencies: List<Currency>)
    @Serializable private data class HistoryAnswer(val days: List<HistoryDay>)
    @Serializable private data class Failure(val error: String)
}

package uy.exchangerates

import kotlinx.serialization.Serializable

/** Buying and selling: the bank's shape. */
@Serializable
data class Spread(val buy: Double, val sell: Double)

/** The bank's reading on or before a day: each currency in pesos, and what it published against the dollar. */
@Serializable
data class Official(
    val date: String,
    val day: String,
    val perLocal: Map<String, Spread>,
    val published: Map<String, Spread>,
    val quote: String,
    val source: String,
)

/** The market's credit, which its licence asks to show beside its figures. */
@Serializable
data class Credit(val label: String, val href: String)

/** The market's table on or before a day: how many of each currency buy one dollar. */
@Serializable
data class Market(
    val date: String,
    val day: String,
    val perUsd: Map<String, Double>,
    val base: String,
    val credit: Credit,
) {
    /** Pesos (or any [local]) per unit of each currency: the local column over the currency's. */
    fun perLocal(local: String = "UYU"): Map<String, Double> {
        val l = perUsd[local]?.takeIf { it > 0 } ?: return emptyMap()
        return perUsd.filterValues { it > 0 }.mapValues { l / it.value }
    }
}

@Serializable
data class OfficialRate(val day: String, val buy: Double, val sell: Double)

@Serializable
data class MarketRate(val day: String, val rate: Double)

/** One of the server's pairs with both rates. */
@Serializable
data class PairRate(
    val pair: String,
    val base: String,
    val quote: String,
    val favourite: Boolean,
    val official: OfficialRate? = null,
    val market: MarketRate? = null,
)

/** One of the server's currencies: its name in the language asked, its mark, its places, whether the bank quotes it. */
@Serializable
data class Currency(val iso: String, val name: String, val symbol: String, val places: Int, val official: Boolean)

/** One day of a pair's history. */
@Serializable
data class HistoryDay(val day: String, val buy: Double, val sell: Double)

@Serializable
data class Conversion(
    val amount: Double,
    val from: String,
    val to: String,
    val date: String,
    val day: String,
    val source: String,
    val side: String? = null,
    val rate: Double,
    val result: Double,
)

enum class Source(val wire: String) { OFFICIAL("official"), MARKET("market") }

enum class Side(val wire: String) { BUY("buy"), SELL("sell"), MID("mid") }

/** What a call that failed says: the status and the server's own words. */
class RatesException(val status: Int, message: String) : Exception(message)

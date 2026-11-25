# exchange-rates-uy for Kotlin

The Android and JVM client for [exchange-rates-uy](https://github.com/guillecura/exchange-rates-uy): read the official rate (Banco Central del Uruguay: buying and selling) and the market's from an exchange-rates-uy server, yours or anybody's. The same API, names and shapes as the server and its JavaScript and Swift clients.

## Install

Through [JitPack](https://jitpack.io/#guillecura/exchange-rates-uy-kotlin), from a tag:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories { maven("https://jitpack.io") }
}

// build.gradle.kts
dependencies {
    implementation("com.github.guillecura:exchange-rates-uy-kotlin:1.0.0")
}
```

A plain JVM library (Kotlin, coroutines, kotlinx.serialization; no Android dependency), so it also works in a Ktor or Spring server. On Android, the app needs `android.permission.INTERNET`.

## Use

```kotlin
import uy.exchangerates.RatesClient
import uy.exchangerates.Source

val rates = RatesClient("https://your-server/api", key = "…")

val bank = rates.official()                       // the BCU's pesos per unit
bank.perLocal["USD"]                              // Spread(buy=40.69, sell=40.70)

val market = rates.market()                       // every currency per dollar
market.perLocal()["EUR"]                          // pesos per euro
market.credit.label                               // "Rates By Exchange Rate API": show it beside its figures

val pairs = rates.pairs()                         // the server's pairs, each with both rates
val usd = rates.history("USD/UYU", from = "2026-01-01")
val result = rates.convert(100.0, from = "USD", to = "UYU", source = Source.OFFICIAL)
```

Every call suspends and runs on `Dispatchers.IO`. A refused call throws `RatesException` with the status and the server's own words.

| Call | Endpoint |
| --- | --- |
| `official(date)` | `/official` |
| `market(date)` | `/market` |
| `pairs(date)` | `/pairs` |
| `currencies(lang)` | `/currencies` |
| `history(pair, from, to, source)` | `/pairs/USD-UYU/history` |
| `convert(amount, from, to, date, source, side)` | `/convert` |

`date` is `YYYY-MM-DD`: the last reading on or before that day.

### The key

When the server only lets its own apps in (exchange-rates-uy's `access`), the key goes with every call as `Authorization: Bearer`. A key inside an app can be read out of it: it says which app is asking, it does not protect the server. Give each app its own and keep the server's keys for servers.

## Develop

```sh
./gradlew test
```

## License

MIT, by [Guille Cura](https://github.com/guillecura).

[![Test](https://github.com/nortano/comed-hourly-pricing/actions/workflows/test.yml/badge.svg)](https://github.com/nortano/comed-hourly-pricing/actions/workflows/test.yml)
[![Lint](https://github.com/nortano/comed-hourly-pricing/actions/workflows/lint.yml/badge.svg)](https://github.com/nortano/comed-hourly-pricing/actions/workflows/lint.yml)

# ComEd Hourly Pricing

A Wear OS app that shows real-time [ComEd](https://hourlypricing.comed.com/) electricity prices on your watch. Displays the current 5-minute price and hourly average across three surfaces: an Activity, a Complication, and a Tile.

## Features

- **Live price display** — fetches the 5-minute spot price and the current hour average from the ComEd API
- **Color-coded price tiers** — at a glance, know if power is cheap, elevated, or expensive
- **Three Wear OS surfaces** — full Activity, lockscreen Complication, and home screen Tile
- **Background sync** — WorkManager keeps prices fresh every 15 minutes
- **Offline fallback** — all surfaces fall back to the last cached price when the network is unavailable

## Price Tiers

| Tier | Condition | Color |
|---|---|---|
| Free | ≤ 0.0¢ | Cyan |
| Normal | 0.0 – 5.0¢ | Green |
| Elevated | 5.0 – 9.9¢ | Yellow |
| High | > 9.9¢ | Red |
| Unknown | Non-numeric / unavailable | White |

## Architecture

- **Single Gradle module** (`:app`) — no multi-module setup
- **No DI framework** — dependencies are manually wired at each call site
- **Three Wear OS surfaces:**
  - `MainActivity` — Compose UI with ViewModel
  - `PriceComplicationService` — `SHORT_TEXT` complication (cache-only)
  - `PriceTileService` — Wear Tile; only the refresh chip hits the network
- **`PriceCacheStore`** — DataStore-backed cache; every `save()` triggers updates to both the Complication and Tile
- **`SyncWorker`** — CoroutineWorker running on a 15-minute periodic schedule

## Requirements

- Android Studio (Meerkat or later recommended)
- Android SDK with Wear OS support
- `local.properties` at the repo root with `sdk.dir` pointing to your Android SDK:
  ```
  sdk.dir=/path/to/Android/sdk
  ```
- A Wear OS device or emulator running API 30+

## Building

```bash
# Debug build
./gradlew :app:assembleDebug

# Release build (R8 minification enabled)
./gradlew :app:assembleRelease

# Install on a connected device/emulator
./gradlew :app:installDebug
```

## Testing

```bash
# All unit tests (JVM, no device required)
./gradlew :app:testDebugUnitTest

# Single test class
./gradlew :app:testDebugUnitTest --tests "com.nortano.comedhourlypricing.ui.PriceTierTest"

# Instrumented / UI tests (requires device or emulator)
./gradlew :app:connectedAndroidTest
```

## Lint & Code Quality

```bash
# Android lint (report written to app/build/reports/lint-results-debug.html)
./gradlew :app:lint

# ktlint check
./gradlew ktlintCheck
```

## CI

GitHub Actions runs on every pull request targeting `main`:

| Workflow | Jobs |
|---|---|
| `lint.yml` | `ktlintCheck` + `lintDebug` |
| `test.yml` | Unit tests + Kover coverage report |

## Key Files

| Path | Purpose |
|---|---|
| `app/src/main/java/.../data/PriceRepository.kt` | Network fetch + cache write |
| `app/src/main/java/.../data/local/PriceCacheStore.kt` | DataStore cache + side-effect updates |
| `app/src/main/java/.../data/remote/RetrofitClient.kt` | Retrofit singleton; base URL `https://hourlypricing.comed.com/` |
| `app/src/main/java/.../data/sync/SyncWorker.kt` | 15-min background sync |
| `app/src/main/java/.../ui/PriceUiState.kt` | `PriceTier` enum + thresholds |
| `app/src/main/java/.../presentation/PriceTileService.kt` | Tile; only the refresh chip hits the network |
| `gradle/libs.versions.toml` | Single source of truth for all dependency versions |

## API

Prices are fetched from the public ComEd Hourly Pricing API — no API key required.

```
GET https://hourlypricing.comed.com/api?type=currenthouraverage
GET https://hourlypricing.comed.com/api?type=5minutefeed
```

Both endpoints return a JSON array of objects with `millisUTC` and `price` string fields.

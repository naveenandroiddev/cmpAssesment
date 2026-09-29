# Market Insights & Execution — Compose Multiplatform reference module

A live bond and equity board, analyst research, and a validated trade ticket — one
implementation shared by Android, iOS and desktop.

## Run it

```bash
./gradlew :app:desktop:run          # fastest loop: real screen, real feed, ~2 s
./gradlew :app:android:assembleDebug
./gradlew :shared:assembleMarketKitXCFramework   # iOS framework (needs full Xcode)
./gradlew :core:model:desktopTest :core:domain:desktopTest :feature:marketinsights:desktopTest
```

## Module map

```
app/android          Android host: one Activity, no logic
app/desktop          JVM host: fastest place to iterate on UI and performance
iosApp               SwiftUI shell + Swift Package wrapping the Kotlin XCFramework
shared               Umbrella: composition root, Compose entry points, Swift-facing API
feature/marketinsights   MVI presentation + the Compose screen
core/designsystem     Theme and the performance-critical composables
data/market           Simulated high-frequency feed, repository implementations
core/domain           TradeValidator, use cases, repository contracts (no UI, no platform)
core/model            Price, Quote, Instrument, ResearchCall — pure Kotlin
build-logic           Convention plugins: one definition of how a module is built
```

Where the interesting code lives:

- `core/designsystem/component/PriceFlash.kt` — draw-phase animation, and why the obvious
  version is the slow one
- `core/designsystem/component/QuoteGridRow.kt` — custom `Layout`, single measure pass
- `core/designsystem/component/Sparkline.kt` — allocation-free ring buffer, cached `Path`
- `feature/marketinsights/MarketRowState.kt` — the fast lane
- `feature/marketinsights/MarketInsightsViewModel.kt` — the two-lane state model
- `core/domain/ObserveMarketBoard.kt` — conflation and frame pacing
- `core/domain/TradeValidator.kt` — the shared pre-trade rules

## Requirements

JDK 17, Android SDK 37, Xcode 16+ for iOS, Apple-silicon Mac

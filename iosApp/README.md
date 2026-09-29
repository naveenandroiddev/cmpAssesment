# iOS integration

The iOS app is a SwiftUI shell that hosts the shared Compose screen, plus one fully native
SwiftUI screen driven by the same shared business logic — proof that the shared module is
not tied to Compose.

```
iosApp/
├── MarketInsights/
│   ├── MarketInsightsApp.swift              SwiftUI app, one shared component
│   ├── MarketInsightsComposeScreen.swift    UIViewControllerRepresentable → Compose
│   └── NativeTicketPreview.swift            native SwiftUI, shared TradeValidator
└── MarketKitPackage/
    ├── Package.swift                        binaryTarget wrapping the XCFramework
    └── Sources/MarketKitWrapper/
        └── MarketBoardStream.swift          Flow → ObservableObject / AsyncStream
```

## Building the framework

```bash
./gradlew :shared:assembleMarketKitXCFramework
# → shared/build/XCFrameworks/release/MarketKit.xcframework
```

Requires full Xcode, not just Command Line Tools:

```bash
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
```

## Why Swift Package Manager rather than embedding the framework directly

| | SPM `binaryTarget` (chosen) | Direct framework embed | CocoaPods |
|---|---|---|---|
| iOS engineer needs Kotlin toolchain | No, once published | Yes | Yes |
| Versioned, checksummed artifact | Yes | No | Yes |
| Works with Xcode previews / multiple targets | Yes | Awkward | Yes |
| Extra tooling | None | None | Ruby, Podfile |

During local development `Package.swift` points at the Gradle output path, so a Kotlin
change is `./gradlew :shared:assembleMarketKitXCFramework` + rebuild. For releases, CI
uploads the zipped XCFramework and updates `url` + `checksum`.

## What the boundary deliberately hides

`shared/src/iosMain/.../IosInterop.kt` is the only Kotlin file Swift is meant to care
about. It exists because three Kotlin idioms do not survive the Objective-C export, and
absorbing them once is cheaper than having every Swift engineer rediscover them:

| Kotlin | What Swift actually sees | What we expose instead |
|---|---|---|
| `Flow<T>` | an opaque object it cannot collect | `subscribe(onBoard:) -> KotlinCancellable` |
| `suspend fun` | completion handler with a nullable error | nothing suspends at the boundary |
| `sealed interface` | plain classes, no exhaustive `switch` | flattened `ValidationSummary` of `Bool` + `[String]` |
| `value class Price` | erased to `Int64`, not constructible in Swift | `…Micros: Int64` parameters, formatting done in Kotlin |

If that file grows past a few hundred lines, adopt [SKIE](https://skie.touchlab.co), which
generates Swift-friendly bindings for flows and sealed classes. We have not taken that
dependency yet — the decision is recorded in ADR-001 and is reversible in a day.

## Memory management notes for the Swift side

Kotlin/Native uses a tracing garbage collector, so mutable shared state and background
threads are fine. The one real hazard is a Kotlin coroutine that is never cancelled: it
keeps its captured Swift closure alive, and with it everything the closure references.
`MarketBoardStream` cancels in `stop()` and `deinit`; follow that pattern for anything that
subscribes.

# ADR-001 --- Architecture for the Market Insights & Execution Module

**Status:** Accepted\
**Date:** 2026-09-25\
**Decision owner:** Mobile Architecture\
**Audience:** Product, UX, Backend Architecture, Mobile Engineering\
**Supersedes:** None

------------------------------------------------------------------------

## 1. Context and problem statement

The module presents a live bond and equity market board, analyst
research alongside it, and a trade ticket.

The architecture has three hard requirements:

  -----------------------------------------------------------------------
  Requirement                         Target / interpretation
  ----------------------------------- -----------------------------------
  **Precision**                       A displayed or submitted price must
                                      be exact. No rounding drift.

  **Latency**                         The board should track the market
                                      at screen refresh rate without
                                      visible stutter, including on a
                                      three-year-old Android device.

  **Consistency**                     Android, iOS and desktop must apply
                                      identical trading rules.

  **Feed volume**                     Approximately 1,600 quote
                                      updates/second across 24
                                      instruments.

  **Display rate**                    60 FPS; the feed can therefore
                                      produce substantially more events
                                      than can be rendered.
  -----------------------------------------------------------------------

The architectural problem is therefore not simply how to consume a
stream. It is how to preserve correctness while controlling the amount
of work that reaches the rendering pipeline.

## 2. Goals and non-goals

### Goals

-   One source of truth for trading rules
-   Exact price arithmetic
-   Smooth rendering under high-frequency updates
-   Shared domain and maximum practical UI reuse
-   Testable architecture with explicit boundaries
-   Consistent behavior across Android, iOS and desktop

### Non-goals

-   Using the client as the security boundary
-   Guaranteeing trade execution from the client
-   Rendering every market-data event
-   Removing all native platform code
-   Selecting the final market-data transport before Backend
    Architecture decides it

## 3. Architecture decision

Use **Compose Multiplatform** for the shared product UI and **Kotlin
Multiplatform** for business and data logic.

Organize the implementation as:

-   Multi-module Clean Architecture
-   Unidirectional MVI presentation
-   Thin Android, iOS and desktop application hosts
-   Shared domain and model layers
-   Repository boundary around market-data transport
-   Explicit composition root rather than a DI framework

### High-level architecture

``` text
app/android      app/desktop      iosApp (SwiftUI)
       \               |               /
        \              |              /
             shared (umbrella / iOS framework)
                         |
          feature/marketinsights
             UI + MVI presentation
              /                  \
             /                    \
   core/designsystem        data/market
                                |
                         core/domain
                   TradeValidator + use cases
                                |
                         core/model
                     Price + Quote + Order
```

`core/domain` and `core/model` do not know about Compose, Android or
iOS. This keeps trading rules independently testable and portable.

## 4. Module boundaries

  -----------------------------------------------------------------------
  Module                              Responsibility
  ----------------------------------- -----------------------------------
  `core/model`                        `Price`, `Quote`, `Instrument`,
                                      `Order`, validation results. No
                                      UI/platform dependencies.

  `core/domain`                       Trading rules, validators and use
                                      cases.

  `data/market`                       Feed connection, decoding,
                                      normalization, repository,
                                      snapshots and stream sharing.

  `feature/marketinsights`            MVI state, intents,
                                      reducers/effects and shared
                                      board/ticket UI.

  `core/designsystem`                 Shared visual components,
                                      typography, spacing and interaction
                                      patterns.

  `app/android`                       Android composition root,
                                      lifecycle, platform services and
                                      navigation.

  `iosApp`                            SwiftUI navigation/system
                                      integration and shared-framework
                                      boundary.

  `app/desktop`                       Desktop composition root and
                                      host-specific integration.
  -----------------------------------------------------------------------

## 5. Market-data flow and backpressure

The feed is an event stream while the UI is a frame-based system. The UI
consumes the latest available snapshot once per frame rather than
rendering every event.

``` text
Feed (~1,600 updates/s)
          |
          v
   Decode / normalize
          |
          v
   Market repository
          |
          +-------> Audit / analytics
          |         (every event where required)
          |
          v
    Latest snapshot
          |
     frame sampler
          |
          v
   UI state holders
          |
          v
       Compose
       60 FPS
```

If multiple quote updates arrive before the next frame, only the latest
value can be visible in that frame. Rendering intermediate states wastes
CPU/GPU time.

Dropping intermediate display updates is therefore intentional
backpressure, not data loss for consumers that require the full event
stream.

The repository boundary keeps the open transport decision --- WebSocket
versus FIX/SBE gateway --- out of presentation code.

## 6. Two-lane state model

The screen has two different state lifecycles.

### Screen state

Slow-changing state:

-   Filters
-   Research
-   Selected instrument
-   Open trade ticket
-   Validation result

### Market state

High-frequency state:

-   Bid
-   Ask
-   Last price
-   Quantity
-   Market status

``` text
MarketSnapshot
    |
    +-- RowState[instrumentId]
           |
           +-- BidCell
           +-- AskCell
           +-- LastPriceCell

ScreenState
    |
    +-- FilterBar
    +-- ResearchPanel
    +-- TradeTicket
```

A quote update should not cause the entire screen tree to be
reconsidered. Only the affected row/cells should observe the changed
market state.

## 7. Price and money representation

Prices are represented as integer micro-units in a 64-bit value.
Floating-point types are excluded from the domain model.

``` kotlin
@JvmInline
value class Price(val microUnits: Long) {

    operator fun plus(other: Price): Price =
        Price(microUnits + other.microUnits)
}
```

Conversion to/from decimal presentation is isolated at the boundary.
Business rules operate on the exact representation.

### Server authority

The client validator is a latency and UX optimization. The server
remains authoritative for authorization, account state, limits, market
conditions and final pre-trade validation.

## 8. Trade execution flow

``` text
User edits ticket
       |
       v
  MVI Intent
       |
       v
TradeValidator (shared domain)
       |
   valid? ---- no ----> validation error state
       |
      yes
       |
       v
 Order use case
       |
       v
 Market repository / execution gateway
       |
       v
     Server
       |
       +----> authoritative validation
       |
       v
 Execution / order status
       |
       v
 State update -> UI
```

Validation should be deterministic and side-effect free wherever
possible so the same rules can run in Android, iOS and desktop tests.

## 9. Presentation architecture --- MVI

MVI provides explicit state transitions and predictable cross-platform
behavior.

  -----------------------------------------------------------------------
  Element                             Responsibility
  ----------------------------------- -----------------------------------
  **Intent**                          User intent such as `Refresh`,
                                      `SelectInstrument`, `EditQuantity`,
                                      `SubmitOrder`.

  **Reducer**                         Produces the next immutable screen
                                      state.

  **Effect**                          Handles one-off operations such as
                                      navigation, submission or transient
                                      messaging.

  **State**                           Observable contract for slow-moving
                                      screen state. High-frequency row
                                      state remains separate.
  -----------------------------------------------------------------------

The implementation should remain lightweight rather than
framework-heavy.

## 10. Lifecycle and concurrency

### Android

-   ViewModel owns screen state.
-   Lifecycle-aware collection prevents UI work when the screen is
    inactive.
-   A short background grace period avoids reconnecting on every
    rotation.
-   Market-data ownership should outlive transient configuration changes
    where appropriate.

### iOS / Kotlin Native

Kotlin/Native's current memory model removes the old freezing
constraint, but cancellation and ownership across the Swift/Kotlin
boundary remain explicit concerns.

Subscriptions should expose explicit cancellation ownership to avoid
long-lived Kotlin coroutines retaining Swift closures.

### Shared feed

``` text
Screen A ----\
Screen B -----+---- Shared Market Stream ---- Feed
Screen C ----/
```

Multiple observers should share one upstream market subscription.

## 11. Testing strategy

  --------------------------------------------------------------------------
  Layer                   Tests                      What the test protects
  ----------------------- -------------------------- -----------------------
  **Domain**              Price arithmetic,          Cross-platform business
                          `TradeValidator`, order    consistency and
                          rules                      precision

  **Data**                Snapshot construction,     Feed normalization and
                          malformed messages,        repository correctness
                          reconnection, ordering     

  **Performance**         1,600 updates/sec          No unbounded queueing
                          simulation, frame          or avoidable rendering
                          sampling, CPU/memory       work
                          measurements               

  **Presentation**        Reducer/state-transition   Predictable MVI
                          tests                      behavior

  **UI**                  Board and ticket           Critical user flows
                          interaction tests          

  **Contract**            Same validator scenarios   Prevention of
                          on Android/iOS/desktop     platform-specific
                                                     trading-rule drift
  --------------------------------------------------------------------------

### Performance acceptance

Measure:

-   Frame time
-   Dropped frames / jank
-   CPU utilization
-   Memory growth
-   Allocation pressure
-   Feed-to-frame latency
-   Snapshot freshness

A benchmark that only proves the code can process 1,600 messages/sec is
insufficient. The real acceptance criterion is rendering behavior under
load.

## 12. Alternatives considered

  -----------------------------------------------------------------------
  Decision                Alternative             Reason for current
                                                  direction
  ----------------------- ----------------------- -----------------------
  **CMP shared UI**       Separate native         Reduces UI duplication
                          Android + SwiftUI       and behavioral drift;
                                                  native remains an
                                                  escape hatch.

  **KMP domain**          Independent domain      Trading rules are too
                          logic per platform      important to duplicate.

  **Integer `Price`**     `Double` / `Float`      Floating-point
                                                  representation is
                                                  inappropriate for exact
                                                  financial values.

  **Frame sampling**      Render every feed event The display cannot show
                                                  multiple states inside
                                                  one frame.

  **Manual DI**           Hilt / Koin             Current dependency
                                                  graph is small enough
                                                  for explicit
                                                  construction.

  **MVI**                 Unstructured ViewModel  Explicit transitions
                          state                   are easier to reason
                                                  about across platforms.
  -----------------------------------------------------------------------

## 13. Security and correctness boundaries

The client is responsible for fast validation feedback and UX.

The server is responsible for:

-   Authentication
-   Authorization
-   Account state
-   Position and risk checks
-   Market conditions
-   Final validation
-   Order acceptance/rejection
-   Audit authority

The client must never be treated as a security boundary.

Audit consumers should subscribe to the authoritative event stream
rather than the frame-sampled UI stream.

Price formatting is presentation-only; the domain never receives a value
that has already been rounded for display.

## 14. Operational considerations and observability

  -----------------------------------------------------------------------
  Signal                              Why it matters
  ----------------------------------- -----------------------------------
  **Feed lag / last event age**       Detects stale market data.

  **Snapshot-to-frame latency**       Shows whether presentation is
                                      keeping pace with the feed.

  **Dropped display ticks**           Useful diagnostic metric for
                                      display backpressure.

  **Frame time / jank**               Direct indicator of user-perceived
                                      responsiveness.

  **Reconnect count / duration**      Identifies unstable sessions and
                                      reconnect storms.

  **Order validation failures**       Separates user-facing rule failures
                                      from transport/server failures.
  -----------------------------------------------------------------------

## 15. Risks and mitigations

  -----------------------------------------------------------------------
  Risk                                Mitigation
  ----------------------------------- -----------------------------------
  **CMP/iOS maturity or               Keep SwiftUI as the host and keep
  platform-specific UI gap**          shared/native boundaries thin
                                      enough to replace an individual
                                      screen.

  **Interop friction with Kotlin      Contain Swift-facing adapters in
  types**                             one interop layer. Consider SKIE
                                      only if the adapter surface becomes
                                      costly.

  **Over-sharing UI**                 Share genuinely common product UI;
                                      retain native implementations for
                                      platform-specific behavior.

  **Feed overwhelms downstream        Separate every-event consumers from
  consumers**                         the frame-sampled UI consumer and
                                      enforce bounded
                                      buffering/backpressure.

  **Client/server validation drift**  Use shared contract scenarios and
                                      keep server validation
                                      authoritative.
  -----------------------------------------------------------------------

## 16. Open decisions and follow-ups

  -----------------------------------------------------------------------
  Decision                Owner                   Current position
  ----------------------- ----------------------- -----------------------
  **WebSocket vs FIX/SBE  Backend Architecture    Open. Repository
  gateway**                                       boundary isolates this
                                                  from presentation.

  **Server-side pre-trade Backend + Product       Required. Client
  validation**                                    validator is not a
                                                  security boundary.

  **Swift interop         Mobile Architecture     Start with contained
  generation**                                    adapters; revisit SKIE
                                                  if the surface grows.

  **Performance           Mobile QA               Include at least one
  acceptance device                               older Android reference
  matrix**                                        device and current
                                                  iOS/desktop targets.
  -----------------------------------------------------------------------

## 17. Expected outcome

The design optimizes for:

1.  **Correctness** of financial values and trading rules
2.  **Predictable rendering cost** when the feed is faster than the
    display
3.  **Cross-platform consistency** without forcing every platform to be
    identical

The architecture also preserves practical escape hatches:

-   Transport can change behind the repository.
-   An individual iOS screen can be reimplemented natively without
    changing domain logic.
-   Dependency injection can evolve if the graph becomes larger.
-   Shared UI can be selectively replaced when platform-specific
    behavior justifies it.

These choices keep the architecture practical rather than locking the
product into one implementation path.

------------------------------------------------------------------------

## Decision record note

This ADR records the architecture, reasoning and trade-offs for the
proposed module.

Exact performance figures and platform compatibility claims should be
validated against the final dependency versions and target device matrix
during implementation.

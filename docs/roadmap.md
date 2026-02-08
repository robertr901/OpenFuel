# OpenFuel Roadmap

## Milestone 0 — Foundation (MVP+ groundwork)
**Scope**
- Local-first architecture with Room + DataStore.
- Core screens: Home, Add Food, Food Detail, Settings.
- Navigation structure and design system primitives.
- Offline logging loop and export (local-only).
- Domain logic with unit tests.
- Threat model and verification docs.

**Acceptance criteria**
- App builds and runs offline with no network calls by default.
- Users can log a meal and see daily totals.
- Export creates a JSON file with schemaVersion and appVersion.
- Core domain logic covered by unit tests.

## Milestone 1 — MVP polish
**Scope**
- Editing and deleting meal entries.
- Daily goal editing UI with validation.
- Improved quick add presets and recent foods ranking.
- Basic empty states and error messaging.

**Acceptance criteria**
- Users can correct mistakes without data loss.
- Goals are editable and reflected in UI.
- UX feels stable and consistent across screens.

**Progress update (this worktree)**
- Completed: Home date navigation (previous/next day) and selected-date ViewModel state.
- Completed: Meal-entry edit/delete flows with confirmation and immediate totals refresh.
- Completed: Local-only Room food search with escaped `LIKE` and 300ms debounced query.
- Completed: Bottom-navigation app shell with top-level Today/Foods/Settings destinations and stable tab back stack behavior.
- Completed: Foods Library tab with local-only Room-backed list and search, plus detail navigation.
- Completed: Empty-state and failure-surface polish (snackbars for entry update/delete and export failures).
- Completed: Safer decimal parsing for user input, including comma-decimal support (`1,5`).
- Completed: Global goals persisted in local DataStore (no server), reflected on Home progress and Settings.
- Completed: New unit coverage for day windows, ViewModel date/update/delete flows, goal validation, and search escaping.
- Completed: New unit coverage for decimal parsing, Foods library ViewModel search flow, and DataStore goals repository behavior.
- Partial: quick add presets/ranking are still basic; no ranking model yet.

## Milestone 2 — Insights & personalization
**Scope**
- Trends (weekly/monthly summaries) computed locally.
- Custom meal types and tagging.
- Advanced unit conversions and serving size management.

**Acceptance criteria**
- Trends work offline with deterministic calculations.
- Personalization settings are local-only and exportable.

**Progress update (this worktree)**
- Completed: Pro-gated Insights tab with local-only 7-day and 30-day trend summaries.
- Completed: Local consistency score heuristic with pure domain calculator and unit tests.
- Completed: Local entitlements repository (`isPro`) backed by DataStore with debug-only override switch in Settings.

## Milestone 3 — Optional online enhancements (opt-in)
**Scope**
- User-initiated online food lookup and barcode lookup.
- Clear data provenance and privacy notices.
- Fast-fail and resilient network layer with strict guardrails.

**Acceptance criteria**
- Online lookup is triggered only by explicit user actions (no background calls).
- Local capture remains reliable when offline or when network calls fail.

**Progress update (this worktree)**
- Completed: OpenFoodFacts integration with `Retrofit` + `OkHttp` timeouts and retries disabled.
- Completed: `UserInitiatedNetworkGuard` and tokenized network-call enforcement path in ViewModels/data source.
- Completed: Add Food explicit "Search online" flow with local-first UI and online preview save/save-log actions.
- Completed: Barcode scan screen with ML Kit + CameraX, plus lookup retry/error handling.
- Completed: Food model and Room migration for barcode uniqueness and favorites persistence.
- Completed: Add Food now surfaces favorites and recent logged foods for faster capture loops.
- Completed: Online search now defaults ON with user override in Settings, and online actions are blocked cleanly when disabled.
- Completed: Add Food fast-capture segmented modes (`Recents`, `Favourites`, `Local`, `Online`) and richer online preview sheet.
- Completed: Local "Report incorrect food" flag for imported foods (device-local only).
- Completed: Unified Add Food search UX with a single query input, explicit online action, and deterministic online state rendering (idle/loading/empty/error).
- Completed: Search domain merge policy for local+online results, with local-first ordering and dedupe safeguards.
- Completed: Provider abstraction with a registry scaffold (OpenFoodFacts active; USDA/Nutritionix/Edamam stubs documented and disabled).
- Completed: Deterministic instrumentation coverage for unified search controls/filter behavior and online-disabled gating.
- Completed: Mapping hardening for partial OpenFoodFacts payloads (stable derived IDs, sanitized nutrient values, missing-field tolerance).

## Phase 8 — Provider execution architecture (completed)
**Completed**
- Multi-provider `ProviderExecutor` with deterministic merge/dedupe, structured statuses, guard-token enforcement, and timeout guardrails.
- Real provider selection via `DefaultFoodCatalogProviderRegistry.providersFor(...)` with runtime gating for settings/build/capabilities.
- Deterministic `StaticSampleCatalogProvider` added for multi-provider tests and instrumentation determinism.
- Room-backed provider result cache with TTL (24h), explicit fast-path reads, and no silent background network refresh.
- Local-only provider diagnostics pipeline (latest per-provider status/timing, overall timing, cache hit/miss) surfaced in debug Settings.
- Add Food and Scan Barcode online actions now run through `ProviderExecutor`.
- Deterministic instrumentation coverage expanded to assert online button triggers deterministic provider results.

## Phase 9 — Real provider, cache refresh, and UX polish (completed)
**Completed**
- OpenFoodFacts provider execution remains active in production builds when online lookup is enabled.
- Provider failure mapping now distinguishes `NETWORK_UNAVAILABLE`, `HTTP_ERROR`, `PARSING_ERROR`, `RATE_LIMITED`, and `GUARD_REJECTED`.
- Provider cache now includes explicit payload versioning (`cacheVersion`) with safe invalidation on version mismatch.
- Corrupted cache payloads are treated as misses and overwritten on the next successful fetch.
- Add Food now includes explicit `Refresh online` action that uses `FORCE_REFRESH` with no silent background network refresh.
- Online result cards now surface provider provenance labels (`OFF`, `Sample`, provider key fallback).
- Debug diagnostics now expose deterministic execution count and cache-hit indicator for repeated explicit actions.
- Save and Save+Log flows now normalize partial online candidates safely (default name, trimmed brand/barcode, bounded nutrients).
- Dedupe fallback avoids hiding distinct results when brand/serving context is missing.
- Deterministic instrumentation coverage expanded for refresh execution behavior under test runner deterministic providers.

## Phase 10 — Intelligence seam (completed)
**Completed**
- Added a new domain intelligence boundary (`IntelligenceService`) with a pure Kotlin rule-based implementation.
- Added deterministic `FoodTextIntent` parsing models (`FoodTextItem`, `QuantityUnit`, `Confidence`) with no Android dependencies.
- Added Add Food `Quick add (text)` helper UI with preview list and explicit user selection.
- Selecting a quick-add preview item now prefills the existing unified search query only; no auto-save, auto-log, or auto-online search.
- Added deterministic instrumentation coverage for quick-add text parsing preview and query prefill behavior.

## Phase 11/12 candidates
- Voice-to-text capture behind an explicit user button, routed through the same intelligence seam.
- Optional on-device ML parser swap-in behind the `IntelligenceService` interface (local-only, no cloud inference).
- Optional OCR ingredient capture behind explicit action with local-only processing and strict privacy UX.

## Deferred from this worktree
- Photo-based logging.
- Cloud sync and multi-device support.
- Additional online providers beyond OpenFoodFacts.
- Advanced quick-add presets and ranking heuristics.
- Per-day goal overrides (goals are intentionally global in this phase).
- Billing integration for live Pro purchase entitlements (debug/local flag currently used).
- Advanced export payload formats (UI gate added; implementation deferred).
- Instrumentation coverage for camera scanning and Compose scanner interactions (Room DAO instrumentation test added; scanner UI tests deferred).

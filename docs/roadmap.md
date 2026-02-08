# OpenFuel Roadmap

This roadmap separates product milestones (user outcomes) from implementation phases (architecture and feature increments). The app is local-first by default. Online functionality is opt-in and user-initiated only.

> Note: Phases 4–7 were intermediate iterations that are now fully represented in the milestones and completed phases below.
> Verification commands in this document and companion docs are repo-relative; run Android gates from `android/`.

## Milestone 0: Foundation (MVP+ groundwork)
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

## Milestone 1: MVP polish
**Scope**
- Editing and deleting meal entries.
- Daily goal editing UI with validation.
- Improved quick add presets and recent foods ranking.
- Basic empty states and error messaging.

**Acceptance criteria**
- Users can correct mistakes without data loss.
- Goals are editable and reflected in UI.
- UX feels stable and consistent across screens.

**Progress update**
- Completed: Home date navigation (previous/next day) and selected-date ViewModel state.
- Completed: Meal-entry edit/delete flows with confirmation and immediate totals refresh.
- Completed: Local-only Room food search with escaped `LIKE` and 300ms debounced query.
- Completed: Bottom navigation app shell with top-level Today/Foods/Settings destinations and stable tab back stack behaviour.
- Completed: Foods Library tab with local-only Room-backed list and search, plus detail navigation.
- Completed: Empty state and failure surface polish (snackbars for entry update/delete and export failures).
- Completed: Safer decimal parsing for user input, including comma-decimal support (`1,5`).
- Completed: Global goals persisted in local DataStore (no server), reflected on Home progress and Settings.
- Completed: Unit coverage for day windows, ViewModel date/update/delete flows, goal validation, and search escaping.
- Completed: Unit coverage for decimal parsing, Foods library ViewModel search flow, and DataStore goals repository behaviour.
- Partial: Quick add presets and ranking are still basic; no ranking model yet.

## Milestone 2: Insights and personalisation
**Scope**
- Trends (weekly/monthly summaries) computed locally.
- Custom meal types and tagging.
- Advanced unit conversions and serving size management.

**Acceptance criteria**
- Trends work offline with deterministic calculations.
- Personalisation settings are local-only and exportable.

**Progress update**
- Completed: Pro-gated Insights tab with local-only 7-day and 30-day trend summaries.
- Completed: Local consistency score heuristic with pure domain calculator and unit tests.
- Completed: Local entitlements repository (`isPro`) backed by DataStore with debug-only override switch in Settings.

## Milestone 3: Optional online enhancements (opt-in)
**Scope**
- User-initiated online food lookup and barcode lookup.
- Clear data provenance and privacy notices.
- Fast-fail and resilient network layer with strict guardrails.

**Acceptance criteria**
- Online lookup is triggered only by explicit user actions (no background calls).
- Local capture remains reliable when offline or when network calls fail.

**Progress update**
- Completed: OpenFoodFacts integration with Retrofit + OkHttp timeouts and retries disabled.
- Completed: `UserInitiatedNetworkGuard` and tokenised network-call enforcement path in ViewModels/data source.
- Completed: Add Food explicit “Search online” flow with local-first UI and online preview Save/Save+Log actions.
- Completed: Barcode scan screen with ML Kit + CameraX, plus lookup retry/error handling.
- Completed: Food model and Room migration for barcode uniqueness and favourites persistence.
- Completed: Add Food surfaces favourites and recent logged foods for faster capture loops.
- Completed: Online search defaults ON with user override in Settings; online actions are blocked cleanly when disabled.
- Completed: Add Food fast-capture segmented modes (`Recents`, `Favourites`, `Local`, `Online`) and richer online preview sheet.
- Completed: Local “Report incorrect food” flag for imported foods (device-local only).
- Completed: Unified Add Food search UX with a single query input, explicit online action, and deterministic online state rendering (idle/loading/empty/error).
- Completed: Search domain merge policy for local+online results, with local-first ordering and dedupe safeguards.
- Completed: Provider abstraction with a registry scaffold (OpenFoodFacts active; other providers stubbed, documented, and disabled).
- Completed: Deterministic instrumentation coverage for unified search controls/filter behaviour and online-disabled gating.
- Completed: Mapping hardening for partial OpenFoodFacts payloads (stable derived IDs, sanitised nutrient values, missing-field tolerance).

## Phase 8: Provider execution architecture (completed)
**Completed**
- Multi-provider `ProviderExecutor` with deterministic merge/dedupe, structured statuses, guard token enforcement, and timeout guardrails.
- Real provider selection via `DefaultFoodCatalogProviderRegistry.providersFor(...)` with runtime gating for settings/build/capabilities.
- Deterministic `StaticSampleCatalogProvider` for multi-provider tests and instrumentation determinism.
- Room-backed provider result cache with TTL (24h), explicit fast-path reads, and no silent background network refresh.
- Local-only provider diagnostics pipeline (latest per-provider status/timing, overall timing, cache hit/miss) surfaced in debug Settings.
- Add Food and Scan Barcode online actions run through `ProviderExecutor`.
- Instrumentation coverage expanded to assert online button triggers deterministic provider results.

## Phase 9: Real provider, cache refresh, and UX polish (completed)
**Completed**
- OpenFoodFacts provider execution active in production builds when online lookup is enabled.
- Provider failure mapping distinguishes `NETWORK_UNAVAILABLE`, `HTTP_ERROR`, `PARSING_ERROR`, `RATE_LIMITED`, and `GUARD_REJECTED`.
- Provider cache includes payload versioning (`cacheVersion`) with safe invalidation on version mismatch.
- Corrupted cache payloads treated as misses and overwritten on next successful fetch.
- Add Food includes explicit `Refresh online` action using `FORCE_REFRESH` with no silent background refresh.
- Online result cards surface provider provenance labels (`OFF`, `Sample`, provider key fallback).
- Debug diagnostics expose deterministic execution count and cache-hit indicator for repeated explicit actions.
- Save and Save+Log normalise partial online candidates safely (default name, trimmed brand/barcode, bounded nutrients).
- Dedupe fallback avoids hiding distinct results when brand/serving context is missing.
- Instrumentation coverage expanded for refresh execution behaviour under deterministic providers.

## Phase 10: Intelligence seam (completed)
**Completed**
- Added a domain intelligence boundary (`IntelligenceService`) with a pure Kotlin rule-based implementation.
- Added deterministic `FoodTextIntent` parsing models (`FoodTextItem`, `QuantityUnit`, `Confidence`) with no Android dependencies.
- Added Add Food “Quick add (text)” helper UI with preview list and explicit user selection.
- Selecting a quick-add preview item prefills the existing unified search query only (no auto-save, auto-log, or auto-online search).
- Added deterministic instrumentation coverage for quick-add text parsing preview and query prefill behaviour.

## Phase 11: Voice capture (completed, local-only, explicit action)
**Completed**
- Added `VoiceTranscriber` seam (`domain/voice`) with structured non-throwing result model.
- Added Android `RecognizerIntent`-based implementation with explicit-action start and bounded capture duration.
- Added voice controls to Add Food “Quick add (text)” dialog:
  - voice start button
  - listening state with cancel
  - friendly unavailable/error states
  - transcribed text inserted into existing quick-add input field
- Kept existing behavior constraints:
  - no always-listening
  - no background capture/service
  - no auto-search
  - no auto-save/log
  - no provider/cache/online execution changes
- Extended deterministic runner wiring to inject fake voice transcriber in androidTest.
- Added deterministic instrumentation smoke coverage for voice quick-add flow without microphone/system speech dependency.

## Phase 12a: Add Food UX and accessibility polish (completed)
**Completed**
- Reworked Add Food information hierarchy into calmer sections with compact quick actions and a local-search-first layout.
- Kept online actions explicit and visually subordinate (`Search online`, `Refresh online`) without behavior changes.
- Collapsed debug provider diagnostics details behind a debug-only advanced expander while retaining deterministic execution counters.
- Standardized Add Food spacing/typography rhythm and instrument-style numeric readability.
- Normalized key app iconography to Material Rounded for bottom navigation and major action surfaces.
- Improved keyboard-first flow and TalkBack semantics in Add Food quick-add/voice interactions.
- Extended deterministic instrumentation assertions for Add Food diagnostics expander behavior.

## Phase 12b: Intelligence seam hardening (completed)
**Completed**
- Added deterministic golden corpus tests for `RuleBasedIntelligenceService` parse/normalize behavior.
- Locked expected outputs for separators, punctuation, mixed-case units, comma-decimal quantities, brands, and garbage input.
- Tightened `IntelligenceService` contract documentation around deterministic, non-throwing behavior.
- Hardened parser edge handling for leading noise tokens and decimal-comma quantities without changing user-visible quick-add flow.
- Reduced parser-internal duplication via helper cleanup with no behavior changes.
- Preserved all guardrails:
  - no provider executor changes
  - no registry/cache schema changes
  - no online guard or network behavior changes
  - deterministic offline androidTest strategy remains unchanged

## Phase 12c: Unified quick add + voice EUX refinement (completed)
**Completed**
- Replaced split quick-add actions with one `Quick add` entry in Add Food quick actions.
- Unified quick-add dialog keeps text parsing primary and moves macro logging behind collapsed `Manual details`.
- Integrated voice capture as a trailing mic action in the quick-add text field.
- Voice UI states are explicit and deterministic (`Idle`, `Listening`, `Result`, `Unavailable/Error`) with clear cancel/error affordances.
- Preserved behavior guardrails:
  - no auto-search
  - no auto-save/log from parsed or voice text
  - no provider executor/registry/cache/online guard changes
- Updated deterministic instrumentation coverage to assert unified quick-add and voice listening transitions with fake transcriber injection.

## Phase 12: On-device ML parser plug-in (still local-only)
**Scope**
- Introduce an optional `IntelligenceService` implementation that uses on-device ML, with rule-based as the default fallback.
- Do not ship binary model files in-repo unless explicitly requested.
- If experimentation is needed:
  - Support loading a model from app-private storage (developer-only manual step), or a local debug-only asset with strict review.
- Keep outputs safe and explainable:
  - `FoodTextIntent.confidence` stays LOW/MEDIUM/HIGH.
  - Use `warnings` when output is ambiguous or missing quantities.
- Add golden test cases comparing ML output to expected structured intent for a small corpus.

**Acceptance criteria**
- Rule-based parser remains available and can be forced via Settings (debug only) to preserve determinism and diagnose regressions.
- If no model is present, the app must fall back to rule-based parsing with identical UI behaviour.
- No network calls for inference. No server storage. No analytics.

## Phase 13: Security/docs/release hardening (completed)
**Completed**
- Documentation refreshed to repo-relative verification instructions across README/docs.
- Deterministic instrumentation gate (`:app:connectedDebugAndroidTest`) documented as first-class in release guidance.
- Security evidence documentation aligned with runtime guardrails and explicit-action networking.
- Added deterministic documentation consistency unit tests to prevent drift on privacy/networking claims.

## Phase 14: Entitlements, paywall, and advanced export (completed)
**Completed**
- Release entitlement wiring now uses Play Billing behind the existing `EntitlementService` seam.
- Added deterministic paywall UX on Pro-gated surfaces with explicit `Upgrade` and `Restore purchases` actions.
- Added entitlement refresh on app foreground entry and explicit restore action (no background polling).
- Added Pro Advanced Export:
  - JSON export path (versioned)
  - CSV export path
  - optional redaction mode for privacy-conscious sharing.
- Added deterministic instrumentation coverage for paywall lock/restore behavior using test fakes (no real billing UI/network in tests).

## Phase 15: Smart local suggestions (no guessing calories)
**Scope**
- Local-only suggestion helpers that accelerate capture without inventing nutrition:
  - “Did you mean …?” suggestions based on local food library, recents, favourites, and simple string similarity.
  - Auto-normalise common units/typos in query text (user-visible and editable).
- Optional: quick-add templates generated from local history only (for example, “usual breakfast”), with explicit confirmation before adding.
- Extend the intelligence seam to surface suggestion metadata:
  - provenance = LOCAL_HISTORY | LOCAL_LIBRARY | USER_TYPED (no remote).
- Improve UX around ambiguous intent:
  - If quantity missing, show a warning pill and keep quantity null.
  - Allow quick edits of quantity/unit in the quick-add preview before running search.

**Acceptance criteria**
- Suggestions deterministic for a given local dataset.
- No heavy background processing. Any indexing runs on explicit screen open or app idle with strict time budget.
- No health advice, no nutritional recommendations, no medical claims.

## Phase 16: Optional camera text capture (OCR) and accessibility polish (local-only)
**Scope**
- Add an explicit “Scan text” action inside Quick add to OCR a receipt/label/ingredients list.
- Requires camera permission; request only when the user taps “Scan text”, with a clear rationale.
- OCR runs locally only (ML Kit on-device text recognition if available, or equivalent).
- Extracted text is shown to the user for editing before parsing (never auto-log).
- Pipe edited text through `IntelligenceService.parseFoodText(...)`.
- Accessibility and resilience:
  - Keyboard-first flows for quick add.
  - Better error states: offline speech unavailable, OCR unavailable, permission denied, etc.

**Acceptance criteria**
- No automatic image retention. If images are used, process in memory and discard immediately.
- No network calls. No telemetry. No background capture.
- Instrumentation tests verify UI wiring and state transitions without depending on real camera frames (use fake inputs).

## Longer-term (Phase 17+ ideas, not planned yet)
- Optional on-device embeddings for better local matching and dedupe (local-only).
- Private export/import improvements (encrypted export, user-controlled keys).
- Multi-device sync only if end-to-end encrypted and user-owned (explicitly not in scope yet).

## Deferred
- Photo-based logging.
- Cloud sync and multi-device support.
- Additional online providers beyond OpenFoodFacts.
- Advanced quick add presets and ranking heuristics.
- Per-day goal overrides (goals intentionally global so far).
- Full instrumentation coverage for camera scanning and Compose scanner interactions (Room DAO instrumentation test added; scanner UI tests deferred).

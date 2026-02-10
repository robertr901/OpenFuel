# OpenFuel Roadmap

This roadmap separates product milestones (user outcomes) from implementation phases (architecture and feature increments). The app is local-first by default. Online functionality is opt-in and user-initiated only.

> Note: Phases 4–7 were intermediate iterations that are now fully represented in the milestones and completed phases below.
> Verification commands in this document and companion docs are repo-relative; run Android gates from `android/`.
> Phase 25 direction reset is the current planning baseline for next-phase sequencing: `docs/phase-25/phase-25-plan.md`.
> Schema policy for phases 26-31: no schema changes or migrations unless a future phase explicitly opens schema work.

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
- Completed: OpenFoodFacts integration with Retrofit + bounded OkHttp timeouts and explicit-action single retry for idempotent GET requests.
- Completed: `UserInitiatedNetworkGuard` and tokenised network-call enforcement path in ViewModels/data source.
- Completed: Add Food explicit “Search online” flow with local-first UI and online preview Save/Save+Log actions.
- Completed: Barcode scan screen with ML Kit + CameraX, plus lookup retry/error handling.
- Completed: Food model and Room migration for barcode uniqueness and favourites persistence.
- Completed: Add Food surfaces favourites and recent logged foods for faster capture loops.
- Completed: Online search defaults ON with user override in Settings; online actions are blocked cleanly when disabled.
- Completed: Add Food unified local-first search with one query field, explicit online actions, and sectioned Local/Online results.
- Completed: Local “Report incorrect food” flag for imported foods (device-local only).
- Completed: Unified Add Food search UX with a single query input, explicit online action, and deterministic online state rendering (idle/loading/empty/error).
- Completed: Search domain merge policy for local+online results, with local-first ordering and dedupe safeguards.
- Completed: Provider abstraction with registry-driven availability and real Open Food Facts + USDA + Nutritionix integrations.
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

## Phase 16: Unified search + USDA provider integration (completed)
**Completed**
- Kept one Add Food query field and one unified results list with clear Local and Online sections.
- Preserved explicit-action online behavior:
  - local results update as the user types
  - online results execute only on `Search online` / `Refresh online` / explicit barcode lookup action.
- Added USDA FoodData Central provider implementation behind the existing provider seam and registry.
- Added local API key configuration path via `android/local.properties` (`USDA_API_KEY`) with no secrets committed.
- Added graceful missing-key handling with clear user-visible messaging and no crashes.
- Added deterministic USDA parsing/mapping unit tests (no live network dependency).
- Added deterministic coverage that online provider execution does not run until explicit user action.

## Phase 17: Multi-provider online search orchestration (completed)
**Completed**
- Added deterministic `OnlineSearchOrchestrator` execution contract for explicit online actions.
- Surfaced `Online sources` disclosure in Add Food with per-provider statuses (`ok`, `empty`, `failed`, `disabled`, `needs setup`).
- Added provider-availability flags in `android/local.properties` for Open Food Facts and USDA.
- Preserved explicit-action-only online behavior (no online execution on typing, no background polling/silent refresh).

## Phase 18: Provider integrations hardening (completed)
**Completed**
- Added a real Nutritionix provider integration behind existing provider seams (`NutritionixRemoteFoodDataSource` + `NutritionixCatalogProvider`).
- Added local credentials/config support via `android/local.properties`:
  - `ONLINE_PROVIDER_NUTRITIONIX_ENABLED`
  - `NUTRITIONIX_APP_ID`
  - `NUTRITIONIX_API_KEY`
- Extended provider availability policy with explicit `needs setup` behavior for missing Nutritionix credentials.
- Hardened deterministic multi-provider merge in orchestrator:
  - key precedence `barcode` > `exact name(+brand)` > `fuzzy`
  - richer nutrition payload preferred on duplicates
  - deterministic tie-break by provider priority and stable identity.
- Preserved explicit-action guardrails, deterministic tests, and no schema/migration changes.

## Phase 19: Online provider reliability hardening (completed)
**Completed**
- Hardened Open Food Facts reliability for explicit actions with bounded connect/read/write/call timeouts.
- Added at-most-one explicit-action retry for idempotent Open Food Facts GET requests (no background retry loops).
- Added Settings `Online provider setup` status section (`Configured` / `Needs setup` / `Disabled`) without revealing secret values.
- Reduced Add Food online error duplication by using clearer top-level summaries and source-level status details.
- Improved deterministic multi-provider dedupe ranking and tie-breaking to reduce false merges.
- Preserved explicit-action-only networking, local-first defaults, and no cache schema/migration changes.

## Phase 20: Release review and quality hardening (completed)
**Completed**
- Performed end-to-end release review across UX, reliability, data quality, privacy/security, accessibility, maintainability, and docs.
- Added lightweight CI PR gate workflow that runs:
  - `./gradlew test`
  - `./gradlew assembleDebug`
  - `./gradlew :app:connectedDebugAndroidTest`
- Added documentation consistency test coverage that asserts CI workflow contains all three release gates.
- Refined Add Food online messaging so top-level error/setup banners are suppressed when results are already available (provider statuses remain visible in `Online sources`).
- Updated docs to align with runtime behavior and explicit-action networking constraints.

## Current direction: phases 26-31 (Phase 25 reset)
For current planning, use the Phase 25 reset pack as the source of truth:
- Canonical roadmap and scope controls: `docs/phase-25/phase-25-plan.md`
- Decision anchors: `docs/phase-25/decision-log.md`
- Acceptance thresholds: `docs/phase-25/acceptance-criteria.md`
- Deterministic test strategy: `docs/phase-25/test-strategy.md`

### Phase 26: Reliability and UX Clarity Baseline
- Goal: remove high-friction reliability and clarity issues without feature expansion.
- Non-goals: no new providers, no schema changes, no new permissions.

### Phase 27: Data Trust Layer v1
- Goal: strengthen provenance, serving/unit consistency, and explainability.
- Non-goals: no cloud validation service, no AI inference.

### Phase 28: Shared Core for Cross-Platform (KMP)
- Goal: extract shared domain rules and mapping logic for Android and iOS parity.
- Non-goals: no full iOS release in this phase, no schema changes.

### Phase 29: iOS MVP (offline-first parity)
- Goal: ship iOS MVP for core logging, export, and explicit online lookup.
- Non-goals: no cloud sync, no advanced wearable parity.

### Phase 30: Wear OS Utility Slice
- Goal: deliver narrow, high-value wrist workflows under explicit-action constraints.
- Non-goals: no background sync loops, no broad health-platform ingestion.

### Phase 31: Cross-Platform Stabilization and Release Readiness
- Goal: harden Android, iOS, and Wear quality and parity before expansion.
- Non-goals: no new headline features, no schema changes unless explicitly opened.

## Historical planning archive: phases 22-30 (superseded by Phase 25 reset)
The section below is retained for historical traceability. Current forward direction is phases 26-31 from the Phase 25 reset documents.
Each phase below is intentionally bounded. Any item that risks schema/migration churn, background networking, or non-deterministic testing is out of scope by default.

### Phase 22: Query normalization and input resilience
**Goal**
- Make text input normalization deterministic across punctuation, whitespace, brand casing, and mixed units.

**Why it matters**
- Better search hit-rate and fewer confusing misses without changing provider/network behavior.

**Minimal deliverables**
- Shared normalization utility for Add Food query + quick-add parser handoff.
- Canonical golden input/output corpus for normalization.
- ViewModel-level coverage that query changes preserve current explicit-action online behavior.

**Explicit non-goals**
- No ML inference changes.
- No provider executor/registry/cache changes.
- No background pre-processing jobs.

**Acceptance criteria**
- Same raw input always yields the same normalized query.
- Typing does not auto-trigger online execution.
- Existing query-related testTags and UI behavior remain stable.

**Test strategy**
- JVM golden tests for normalization corpus.
- Targeted ViewModel tests for online-state reset behavior on query edits.

**Risks/failure modes**
- Over-normalization may hide meaningful user input.
- Regression risk in dedupe keys if normalization contracts are changed inconsistently.

### Phase 23: Barcode flow reliability hardening
**Goal**
- Improve explicit scan-to-result reliability and user recovery messaging on real devices.

**Why it matters**
- Barcode is a primary speed path; failures are high-friction moments.

**Minimal deliverables**
- Better bounded retry UX copy for failed scans/lookups.
- Defensive debounce/cooldown to prevent duplicate lookup storms from rapid detections.
- Clearer offline and provider-setup failure states.

**Explicit non-goals**
- No new permissions.
- No background camera/scanner behavior.
- No provider cache schema changes.

**Acceptance criteria**
- Scan flow remains explicit-action and non-blocking.
- Repeated detections do not generate duplicate unstable requests.
- Offline and setup errors are user-readable and deterministic.

**Test strategy**
- JVM tests for scan-state transitions and debounce behavior.
- Deterministic instrumentation checks for stable scan error/retry rendering.

**Risks/failure modes**
- Overly aggressive debounce can drop legitimate scans.
- State-machine regressions can leave screen in stuck loading state.

### Phase 24: Serving and unit normalization v2
**Goal**
- Make serving-size interpretation safer and more consistent across provider payloads.

**Why it matters**
- Serving ambiguity drives most nutrition quality complaints.

**Minimal deliverables**
- Consolidated serving parser rules for common imported patterns.
- Defensive handling for weird or unknown units with safe fallback text.
- Regression suite for known problematic serving strings.

**Explicit non-goals**
- No new nutrient inference models.
- No schema migrations.
- No backend or cloud conversion service.

**Acceptance criteria**
- Defined fixture corpus maps to consistent serving outputs.
- Save and Save+Log flows remain robust for partial serving metadata.
- No crash on malformed serving strings.

**Test strategy**
- JVM fixture tests (provider mapping + parser).
- ViewModel save/log regression tests for partial candidates.

**Risks/failure modes**
- Parser changes can silently alter existing dedupe behavior.
- Unit assumptions may still vary across providers and locales.

### Phase 25: Accessibility confidence pass
**Goal**
- Raise confidence in TalkBack, focus order, keyboard flow, and large-text layouts across primary loops.

**Why it matters**
- Accessibility regressions are high severity and often missed by feature tests.

**Minimal deliverables**
- Core semantics/focus audit for Add Food, Today, and Settings.
- Targeted layout fixes for large font scaling.
- Stable a11y assertions for changed surfaces.

**Explicit non-goals**
- No full visual redesign.
- No new navigation patterns.

**Acceptance criteria**
- Critical controls remain reachable and readable at large text sizes.
- Focus order is deterministic and logical.
- Error states are announced in user-friendly language.

**Test strategy**
- Compose instrumentation semantics assertions on key flows.
- JVM/state tests where semantics are driven by UI state flags.

**Risks/failure modes**
- Semantics label changes can break existing automation if tags/roles drift.
- Layout tweaks can reintroduce clipping on smaller devices.

### Phase 26: Export trust and interoperability hardening
**Goal**
- Make export output easier to trust and easier to consume in external tools.

**Why it matters**
- Export is a privacy-critical user-owned data surface and a Pro anchor.

**Minimal deliverables**
- Tighten JSON/CSV schema docs with concrete examples.
- Redaction preview consistency checks (what user sees vs what file contains).
- Safer export error messaging with clear recovery actions.

**Explicit non-goals**
- No auto-upload/sync.
- No external service integration.

**Acceptance criteria**
- Exported JSON/CSV matches documented format.
- Redacted exports consistently remove configured fields.
- Export failure states are actionable and non-technical.

**Test strategy**
- JVM snapshot tests for JSON/CSV outputs (normal + redacted).
- ViewModel tests for export-state messaging.

**Risks/failure modes**
- CSV formatting drift can break spreadsheet compatibility.
- Redaction regressions could expose unintended context.

### Phase 27: Add Food performance budget enforcement
**Goal**
- Keep Add Food interaction latency stable as local dataset size grows.

**Why it matters**
- Daily adoption depends on predictable speed, not peak benchmark numbers.

**Minimal deliverables**
- Explicit latency budgets for query update -> local results render.
- Lightweight deterministic benchmark/smoke checks in local tooling.
- Small hotspot fixes only where budget breaches are observed.

**Explicit non-goals**
- No architecture rewrite.
- No speculative optimization work.

**Acceptance criteria**
- Measured interactions remain within defined budget on reference devices/data.
- No regressions in explicit-action online behavior.

**Test strategy**
- Deterministic local perf smoke scripts + targeted JVM tests for hotspots.
- Keep instrumentation assertions focused on final UI state, not timing flake.

**Risks/failure modes**
- Micro-optimizations can reduce readability without real user impact.
- Device variance can mask true regressions without fixed baselines.

### Phase 28: Security hardening pass (local-first)
**Goal**
- Reduce residual risk in key handling, logs, and explicit network boundaries.

**Why it matters**
- Security posture is a product feature, not an afterthought.

**Minimal deliverables**
- Secret-handling and log-surface audit with concrete fixes.
- Security/threat docs updated to match runtime exactly.
- Small static checks for accidental secret exposure patterns.

**Explicit non-goals**
- No backend/service expansion.
- No additional data collection.

**Acceptance criteria**
- No committed secrets.
- No sensitive user content in logs.
- Security and threat docs are consistent with runtime and tests.

**Test strategy**
- JVM/static checks (regex/grep style) for known high-risk patterns.
- Documentation consistency tests for explicit-action claims.

**Risks/failure modes**
- Over-broad static checks can create noisy false positives.
- Missed edge logs in debug-only paths can still leak context.

### Phase 29: Release automation maturity
**Goal**
- Make release readiness checks repeatable, auditable, and low-friction.

**Why it matters**
- Manual release drift causes avoidable production risk.

**Minimal deliverables**
- Tighten PR and release workflows around deterministic gate commands.
- Add checklist automation hooks for docs/runtime consistency checks.
- Publish a single canonical release runbook.

**Explicit non-goals**
- No CI vendor migration.
- No external hosted test dependencies.

**Acceptance criteria**
- PRs and release candidates run the same required gates reliably.
- Release checklist steps are scriptable and reproducible from repo.

**Test strategy**
- Workflow validation tests + docs consistency checks.
- Periodic dry-run release simulation.

**Risks/failure modes**
- Overly strict automation can slow iteration if noisy.
- Missing emulator stability handling can create false CI failures.

### Phase 30: Product simplification and utility pruning
**Goal**
- Remove low-value UI/flow complexity while preserving high-value capture speed.

**Why it matters**
- Simpler products are easier to trust, maintain, and scale.

**Minimal deliverables**
- Evidence-based simplification proposals from usage walkthroughs.
- Implement only highest-leverage reductions in steps/cognitive load.
- Guardrail tests proving no behavior regressions.

**Explicit non-goals**
- No feature expansion.
- No redesign-for-redesign’s-sake.

**Acceptance criteria**
- Primary log loop requires fewer choices/steps where changed.
- No regressions in explicit-action networking or privacy posture.
- Existing deterministic tests remain green with minimal churn.

**Test strategy**
- Deterministic instrumentation smoke tests on affected flows.
- ViewModel regression tests for message/state behavior.

**Risks/failure modes**
- Over-pruning can remove power-user workflows.
- Copy simplification can reduce clarity if not validated against error cases.

## Deferred
- Photo-based logging.
- Cloud sync and multi-device support.
- Additional online providers beyond OpenFoodFacts.
- Advanced quick add presets and ranking heuristics.
- Per-day goal overrides (goals intentionally global so far).
- Full instrumentation coverage for camera scanning and Compose scanner interactions (Room DAO instrumentation test added; scanner UI tests deferred).

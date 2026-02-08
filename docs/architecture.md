# Architecture

## Layered structure
```
ui (Compose screens, navigation)
  ↓
ui-state + viewmodels (StateFlow)
  ↓
repositories (interfaces in domain, implementations in data)
  ↓
data (Room, DataStore, export serializer)
  ↓
domain (pure logic, calculations, unit helpers)
```

## Package layout (Android app module)
- `com.openfuel.app.ui`: screens, navigation, design system
- `com.openfuel.app.viewmodel`: ViewModels and UI state models
- `com.openfuel.app.domain`: pure domain logic, repository interfaces, models
- `com.openfuel.app.data`: Room entities/DAO/db, repository implementations, DataStore
- `com.openfuel.app.export`: export serialization + file writing

## Data flow
1. UI events (e.g., log meal) call ViewModel intent methods.
2. ViewModel updates StateFlow UI state and invokes repositories.
3. Repositories persist to Room/DataStore (local-first source of truth).
4. DAO/DataStore emit flows, which ViewModel collects and maps to immutable UI state.
5. Online lookups are routed through a provider abstraction and require explicit user action tokens.

## Search architecture (Unified Search)
- `AddFoodScreen` presents one query input and explicit online actions:
  - `Search online` for cache-preferred lookup
  - `Refresh online` for explicit cache bypass (`FORCE_REFRESH`)
  - compact quick-actions section (`Scan barcode`, `Quick add`)
- `AddFoodViewModel` owns a single `UnifiedSearchState`:
  - local results update from debounced query (Room)
  - online results update only when the user explicitly requests online search
  - query changes clear stale online state and provider execution metadata
- `ProviderExecutor` is the online orchestration layer for both:
  - Add Food text search
  - barcode lookup
- `FoodCatalogProviderRegistry` resolves providers for each request type with runtime gating:
  - settings (`onlineLookupEnabled`)
  - build/debug diagnostics mode
  - provider capability support and configuration presence
- Execution policy:
  - bounded parallel execution (`supervisorScope`)
  - strict per-provider timeout and global budget
  - no exceptions leaked to UI; failures are returned as structured provider statuses
  - deterministic merge: provider priority order + stable dedupe key
- Dedupe strategy:
  - barcode when present
  - else normalized `name + brand + servingSize`
  - fallback to source-scoped identity when brand and serving context are missing
  - provenance retained per merged candidate (`providerId`)
- Online result cards surface provenance labels (`OFF`, `Sample`, or provider key) for explainability.
- Current providers:
  - OpenFoodFacts: network provider
  - Static sample provider: deterministic non-network provider for debug diagnostics/instrumentation determinism
  - USDA/Nutritionix/Edamam: scaffolds only, disabled

## Intelligence seam (Phase 10)
- New domain boundary: `com.openfuel.app.domain.intelligence`.
- `IntelligenceService` interface defines:
  - `parseFoodText(input)` for rule-based extraction of food text intent
  - `normaliseSearchQuery(input)` for deterministic query normalization
- `RuleBasedIntelligenceService` is pure Kotlin (no Android types, no network calls, no ML dependencies).
- Add Food integration is intentionally minimal:
  - UI adds a unified `Quick add` helper dialog (text-first input with optional manual-details expander)
  - parsed preview items are user-selectable
  - selecting an item only prefills the existing unified search query via the existing query update path
- Guardrails:
  - no automatic online search
  - no automatic save/log
  - no background execution
  - helper copy explicitly instructs users to review before adding
- Testability:
  - deterministic unit tests cover parser/normalizer behavior
  - deterministic instrumentation smoke test validates query prefill flow end-to-end offline

## Intelligence seam hardening (Phase 12b)
- The rule-based parser contract is explicitly non-throwing and deterministic for both parsing and normalization.
- A golden test corpus locks expected outputs for tricky inputs (units, separators, punctuation, brands, garbage input).
- Edge-case handling is hardened for:
  - comma-decimal quantities (for example `1,5 cups milk`)
  - uppercase unit tokens (`ML`, `CUPS`, etc.)
  - leading noise tokens (`add`, `log`, `today`, `now`) without changing raw user-entered text in preview.
- Internal parser helper naming/structure is clarified to reduce duplication while preserving output behavior.
- No provider execution, cache schema, online gating, or network behavior changes were made in this hardening phase.

## Voice transcriber seam (Phase 11)
- New domain boundary: `com.openfuel.app.domain.voice`.
- `VoiceTranscriber` defines one explicit-action entry point: `transcribeOnce(...)`.
- Result model is structured and non-throwing:
  - `Success(text)`
  - `Unavailable(reason)`
  - `Cancelled`
  - `Failure(message)` (sanitized user-safe message)
- Android implementation uses `RecognizerIntent` and only starts on explicit user tap from Add Food quick-add dialog.
- No always-listening, no background service, no audio persistence, no telemetry.
- Voice action is exposed as a mic trailing icon in the quick-add text field.
- Voice success only populates quick-add text input; parsing/preview still flows through `IntelligenceService.parseFoodText(...)`.
- Selecting a preview item still only prefills unified search query; no auto-search, auto-save, or auto-log.
- Deterministic androidTest runner injects a fake `VoiceTranscriber` to keep tests offline and independent of microphone/system speech UI.

## Quick add EUX refinement (Phase 12c)
- Add Food now has a single `Quick add` entry point to reduce competing controls.
- Inside the dialog:
  - text parsing remains primary
  - voice capture is integrated in-field (mic trailing action)
  - manual macro logging is still available behind a collapsed `Manual details` expander.
- Voice UI states are explicit and calm: `Idle`, `Listening`, `Result`, and `Unavailable/Error`.
- Guardrails unchanged:
  - no auto-save or auto-log from parsed/voice text
  - no auto-search
  - no provider/cache/online execution behavior changes.

## Provider cache architecture
- Storage: Room table `provider_search_cache`.
- Cache key: normalized request input + provider id + request type.
- Cached payload: public nutrition candidate fields only (no secrets, no personal logs).
- TTL: 24h default (`ProviderExecutionPolicy.cacheTtl`).
- Cache versioning:
  - row field: `cacheVersion`
  - current version: `1`
  - bump policy: increment when serialized payload shape changes incompatibly
- Fast path:
  - if fresh cache hit and request uses `CACHE_PREFERRED`, return immediately
  - no silent background refresh
  - refresh is explicit user action (`FORCE_REFRESH` policy path)
- Safety:
  - version mismatches are purged and treated as cache misses
  - corrupted payload JSON is treated as cache miss and overwritten on next successful fetch

## Provider diagnostics (local-only)
- `InMemoryProviderExecutionDiagnosticsStore` records latest execution report:
  - per-provider elapsed time, status, item count
  - overall elapsed time
  - cache hit/miss counts
- Add Food debug diagnostics include execution count for deterministic verification of repeated explicit actions.
- Advanced provider diagnostics details are collapsed by default behind a debug-only expander in Add Food.
- Exposed in debug Settings diagnostics UI only.
- No remote telemetry, no server-side analytics, no personal-log upload.

## Online guardrails
- Network is optional and user-controllable via Settings.
- Online lookup is enabled by default, but every network call must be explicitly user initiated.
- `UserInitiatedNetworkGuard` issues short-lived tokens and validates them in remote data sources.
- No background sync, no periodic jobs, and no silent network retries.
- No silent no-op states:
  - online-disabled actions return immediate user-visible messaging
  - provider failures surface friendly copy plus debug diagnostics when available
- Failing or partial online payloads must degrade to local-safe UI states (empty/error, no crashes).

## Offline-first strategy
- All logs stored in Room locally.
- Settings stored in DataStore locally.
- App remains fully usable offline; online features only improve discovery.
- Imported online foods are persisted locally and then behave like native local foods.
- Export uses local-only FileProvider sharing.

## Resilience
- Writes are transactional where needed (Room).
- UI state is immutable, derived from stable flows.
- Domain logic is pure and tested (no Android dependencies).
- Search merge/dedupe rules are covered by unit tests.
- Navigation and unified search wiring are covered by deterministic instrumentation tests.

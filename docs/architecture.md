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
- `AddFoodScreen` presents one query input and explicit "Search online" action.
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
  - provenance retained per merged candidate (`providerId`)
- Current providers:
  - OpenFoodFacts: network provider
  - Static sample provider: deterministic non-network provider for debug diagnostics/instrumentation determinism
  - USDA/Nutritionix/Edamam: scaffolds only, disabled

## Provider cache architecture
- Storage: Room table `provider_search_cache`.
- Cache key: normalized request input + provider id + request type.
- Cached payload: public nutrition candidate fields only (no secrets, no personal logs).
- TTL: 24h default (`ProviderExecutionPolicy.cacheTtl`).
- Fast path:
  - if fresh cache hit and request uses `CACHE_PREFERRED`, return immediately
  - no silent background refresh
  - refresh is explicit user action (`FORCE_REFRESH` policy path)

## Provider diagnostics (local-only)
- `InMemoryProviderExecutionDiagnosticsStore` records latest execution report:
  - per-provider elapsed time, status, item count
  - overall elapsed time
  - cache hit/miss counts
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

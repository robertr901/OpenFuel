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

## Deferred from this worktree
- Photo-based logging.
- Cloud sync and multi-device support.
- Additional online providers beyond OpenFoodFacts.
- Advanced quick-add presets and ranking heuristics.
- Per-day goal overrides (goals are intentionally global in this phase).
- Billing integration for live Pro purchase entitlements (debug/local flag currently used).
- Advanced export payload formats (UI gate added; implementation deferred).
- Instrumentation coverage for camera scanning and Compose scanner interactions (Room DAO instrumentation test added; scanner UI tests deferred).

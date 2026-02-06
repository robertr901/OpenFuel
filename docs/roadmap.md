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

## Milestone 3 — Optional online enhancements (opt-in)
**Scope**
- Optional online food lookup (explicitly enabled).
- Clear data provenance and privacy notices.
- Rate-limited and resilient network layer.

**Acceptance criteria**
- Online lookup is OFF by default and clearly explained.
- App functions identically offline when disabled.

## Deferred from this worktree
- Barcode scanning.
- Photo-based logging.
- Cloud sync and multi-device support.
- Public food database integration.
- Advanced quick-add presets and ranking heuristics.
- Per-day goal overrides (goals are intentionally global in this phase).
- Instrumentation coverage for Compose dialogs/interactions (unit coverage added, UI tests deferred).

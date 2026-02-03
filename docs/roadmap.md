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

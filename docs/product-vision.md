# OpenFuel Product Vision

OpenFuel is a privacy-first, local-first nutrition tracker that makes logging fast, reliable, and understandable. The default experience works fully offline, with optional online lookups only when the user explicitly asks for them.

## Product pillars
- **Privacy-first, local-first**
  - Logs, foods, goals, and history stay on device by default.
  - Any online feature is opt-in, user-initiated, and clearly labelled.
  - No telemetry, no ads, no hidden identifiers.

- **Low-friction daily loop**
  - Capture, adjust, and move on: search, pick, log, done.
  - Strong defaults, minimal typing, minimal taps.
  - “Review before add” for anything imported or interpreted.

- **Trustworthy data**
  - Transparent totals and calculations.
  - Clear provenance: Local, Imported (source), or User-entered.
  - Consistent units and rounding rules, stable day-to-day totals.

- **Resilient offline**
  - Core loop never depends on a network connection.
  - When offline, the UI degrades gracefully with no dead ends.

- **Delightful and accessible**
  - Clean, calm UI that prioritises readability and speed.
  - Works with large text, screen readers, and one-handed use.

## Key differentiators
- **Local-first by default, online only by explicit action**
  - No background fetches. No surprise sync. No “mystery traffic”.
- **User-owned data**
  - Exportable, inspectable JSON with a documented schema and versioning.
- **Explainable nutrition**
  - Predictable units, visible assumptions, and safe defaults.
- **Built to scale**
  - Clear seams for future on-device intelligence and voice features without compromising privacy or determinism.

## What “world-class” means for OpenFuel
- **Trust**
  - Users can see where a food came from and how totals were computed.
- **Speed**
  - Most meal logs completed in under **20 seconds** from opening Add Food.
- **Reliability**
  - No data loss, no corrupt state, and recoverable flows after crashes/restarts.
- **Privacy integrity**
  - No network calls unless the user taps an online action.
- **Accessibility**
  - Fully usable at large font sizes and with assistive technologies.
- **Maintainability**
  - Small, reviewable changes; deterministic tests; clean architecture boundaries.

## Success signals (measurable)
- **Median time-to-log < 20s** for common flows (recent food, favourite, local search).
- **99%+ of sessions succeed offline** without errors or blocked UX paths.
- **0 data-loss bugs** (P0) in production.
- **0 unexpected network calls** in offline-only usage (verified via tests and tooling).
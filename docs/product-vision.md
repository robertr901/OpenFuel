# OpenFuel Product Vision

OpenFuel is a privacy-first, local-first nutrition ledger designed for fast daily logging that stays reliable offline.

## Phase 25 direction
- Canonical direction pack: `docs/phase-25/phase-25-plan.md`
- Supporting decisions and acceptance thresholds:
  - `docs/phase-25/decision-log.md`
  - `docs/phase-25/acceptance-criteria.md`

## Vision and principles
### What OpenFuel is
- A fast capture tool for everyday nutrition logging.
- A local-first system where personal logs stay on-device.
- An explainable app: users can see data provenance, assumptions, and outcomes.

### What OpenFuel is not
- Not an ad-supported or telemetry-driven product.
- Not an account-required platform.
- Not a black-box recommendation engine that auto-runs hidden network or inference flows.
- Not a social network for engagement loops.

### Priority JTBD (with trade-offs)
1. Fast daily logging with low friction.
   - Trade-off: prefer speed and stability over novelty features.
2. Trustworthy nutrition data and provenance.
   - Trade-off: avoid opaque inference and low-quality crowdsourced shortcuts.
3. Calm private trend review.
   - Trade-off: reject guilt-driven engagement patterns and manipulative prompts.

### Core loops
1. Fast local logging loop: search -> select -> log.
2. Explicit online discovery loop: user taps `Search online` or `Refresh online`, then chooses whether to save/log.
3. Review/export loop: user reviews local history and runs explicit export actions when needed.

### Quality bar
- Predictable: same inputs should yield the same outputs.
- Explainable: provider/source status and data provenance are visible.
- Reversible: users can edit/delete entries and recover from mistakes without data loss.

### Non-goals
- No telemetry, ads, trackers, accounts, or background analytics.
- No background networking, polling, or silent refresh.
- No automatic logging from quick-add parsing, voice capture, or online provider responses.
- No hidden cloud storage of personal logs.
- No AI coach or photo-calorie estimation as a core loop in this roadmap horizon.

## Product pillars
- Privacy-first, local-first by default.
- Low-friction daily capture.
- Trustworthy and bounded data handling.
- Accessibility and calm interaction design.
- Deterministic behavior and deterministic tests.

## Success signals (measurable)
- Median time-to-log under 20 seconds on common capture flows.
- High offline task completion with graceful fallback when online providers are unavailable.
- Zero unexpected online calls outside explicit user actions.
- Zero P0 data-loss regressions.

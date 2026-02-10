# OpenFuel Product Vision

OpenFuel is a privacy-first, local-first nutrition tracker designed for fast daily logging that stays reliable offline.

## Vision and principles
### What OpenFuel is
- A fast capture tool for everyday nutrition logging.
- A local-first system where personal logs stay on-device.
- An explainable app: users can see data provenance, assumptions, and outcomes.

### What OpenFuel is not
- Not an ad-supported or telemetry-driven product.
- Not an account/sync platform in its current direction.
- Not a black-box recommendation engine that auto-runs hidden network or inference flows.

### Core loops
1. Fast local logging loop: search -> select -> log.
2. Explicit online discovery loop: user taps `Search online` or `Refresh online`, then chooses whether to save/log.
3. Review/export loop: user reviews local history and runs explicit export actions when needed.

### Quality bar
- Predictable: same inputs should yield the same outputs.
- Explainable: provider/source status and data provenance are visible.
- Reversible: users can edit/delete entries and recover from mistakes without data loss.

### Non-goals
- No telemetry, ads, trackers, or background analytics.
- No background networking, polling, or silent refresh.
- No automatic logging from quick-add parsing, voice capture, or online provider responses.
- No hidden cloud storage of personal logs.

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

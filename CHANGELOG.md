# Changelog

All notable changes to OpenFuel are documented here.

## Unreleased

### Phase 40: Review B docs checkpoint
- Published Phase 40 review artefact in `docs/reviews/phase40-review-b.md`.
- Updated roadmap to lock phases 41-50 and outline phases 51-60.
- Added delta notes since Phase 35 and a constrained wearables future placeholder.
- Refreshed docs status surfaces (`README.md`, `docs/README.md`, `CHANGELOG.md`) for shipped phases.

### Phase 39: Trust and data quality v1 shipped
- Added deterministic local data-quality classification and shared trust cues across core food surfaces.
- Added review-and-fix correction affordances on Today, Foods, Add Food, and Weekly Review where confidence is low.
- Preserved explicit-action online triggers and regression coverage.

### Phase 38: Habit loops v1 shipped
- Consolidated Today to one primary Add Food CTA (`home_add_food_fab`) and demoted secondary affordances to link style.
- Added factual consistency feedback for the last 7 days without pressure language.
- Preserved reminder caps, quiet-hours controls, and explicit-action networking invariants.

### Phase 37: Weekly Review v1 shipped
- Added one-tap weekly review entry from core surfaces when eligible.
- Added local-only 7-day aggregation with calm summary and explainable suggested action.
- Added dismiss-for-week behaviour and deterministic state coverage.

### Phase 36: Foods 10x as one system shipped
- Unified local food row composition and actions across Foods and Add Food surfaces.
- Improved known-food flow coherence with consistent trust cues and correction affordances.
- Preserved provider orchestration boundaries and explicit-action online behaviour.

### Phase 35: Review A
- Published canonical evidence audit and risk register in `docs/reviews/phase35-review-a.md`.
- Rebaselined roadmap phases 36-40 from reviewed product and engineering findings.
- Aligned front-door project status docs with shipped reality.

### Phase 34: Goal Profiles v1 shipped
- Added first-run Goal Profile selection with optional skip and non-clinical profile language.
- Added reversible Goal Profile editing in Settings with profile-aware emphasis in Today and Insights.
- Applied profile defaults only when user goals are not customised.
- Added deterministic onboarding and profile regression coverage.

### Phase 33: Today 10x shipped
- Simplified Today hierarchy with progressive disclosure and stronger primary logging focus.
- Improved one-thumb action clarity and accessibility semantics for key Today controls.
- Added deterministic Today screen smoke coverage for collapsed and expanded states.

### Phase 32: Experience-first documentation reset
- Set a single product promise across active docs: fast logging with clear meaning.
- Replaced the forward roadmap with a canonical Phase 32 to Phase 50 plan.
- Added a docs index and archive boundaries for superseded plans.
- Aligned security, threat model, provider contract, and verification guidance.
- Rewrote README around Android quick start and deterministic verification gates.
- No production code, schema, permissions, or networking behaviour changes.

### Recent shipped phases (high-level)
- Phase 31: retention and growth plumbing baseline shipped with deterministic test coverage.
- Phase 30: Android UI consistency and reusable component pass shipped.
- Phase 29: front-end simplification pass for core logging surfaces shipped.
- Phase 28: shared-core extraction for deterministic domain rules shipped.
- Phase 27: data trust layer v1 shipped for provenance and confidence cues.
- Phase 26: reliability and UX clarity baseline shipped.

## Notes
- Detailed historical planning snapshots are archived under `docs/archive/2026-02-11/`.
- Verification gates and deterministic test policy are defined in `docs/verification.md`.

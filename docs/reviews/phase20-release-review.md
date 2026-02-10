# Phase 20 Release Review (Evidence-Based)

## Scope and method
- Scope: release-review pass only (no feature expansion), with minimal high-leverage fixes.
- Review inputs:
  - Runtime gates from `android/`: `./gradlew test`, `./gradlew assembleDebug`, `./gradlew :app:connectedDebugAndroidTest`.
  - Code/path inspection for Add Food, provider orchestration, setup policy, docs, and test seams.
  - Deterministic instrumentation and unit test behavior as primary evidence.

## UX assessment
### What works
- Add Food keeps one primary local-first query loop and explicit online actions (`Search online`, `Refresh online`) in one surface.
- `Online sources` gives understandable per-provider outcomes with user-safe wording.
- Settings now surfaces provider setup health without exposing key values.

### Friction found
- Prior behavior showed top-level online error/setup text even when valid results were already visible, which added noise.

### Fix shipped in Phase 20
- Top-level online error/setup message is now suppressed when online results are present; source-level statuses remain under `Online sources`.
- Evidence:
  - `android/app/src/main/java/com/openfuel/app/viewmodel/AddFoodViewModel.kt`
  - `android/app/src/test/java/com/openfuel/app/viewmodel/AddFoodViewModelTest.kt`

## Utility assessment
- High-value loops remain clear:
  - local food search/logging
  - explicit online lookup when needed
  - explicit quick-add helper (text/voice), no auto-log/auto-search
- Feature utility is strongest where behavior is deterministic and reversible.
- Recommendation: continue removing duplicate messaging and UI noise before adding net-new complexity.

## Reliability assessment
### Current strengths
- Explicit-action-only online path with user-initiated token enforcement.
- Per-provider statuses isolate failures (one provider failure does not blank all online results).
- Deterministic tests cover provider setup/missing config and key online state transitions.

### Remaining risks
- External provider latency/availability remains variable by definition.
- Emulator/device availability is still the common local failure mode for connected tests.

### Reliability hardening in this phase
- CI now enforces all three release gates on pull requests to catch regressions early.
- Evidence:
  - `.github/workflows/android-gates.yml`
  - `android/app/src/test/java/com/openfuel/app/docs/DocumentationConsistencyTest.kt`

## Data quality assessment
- Multi-provider dedupe/ranking and partial payload handling are already in place from previous phases.
- Phase 20 did not alter provider mapping behavior or cache schema.
- Remaining risk area: long-tail serving-size/unit normalization inconsistencies across providers (deferred to roadmap).

## Security and privacy assessment
### Verified posture
- Local-first by default, explicit-action-only online networking remains intact.
- No telemetry, ads, trackers, or background polling introduced.
- Provider setup surfaces show only configured/missing/disabled state and reason text; no secret values.

### Docs alignment updates
- Security narrative now explicitly references deterministic CI release gates.
- Threat model now includes entitlement/paywall flows as in-scope runtime behavior.
- Evidence:
  - `SECURITY.md`
  - `docs/threat-model.md`

## Accessibility assessment
- Existing semantics/testTag strategy remains stable; no regressions introduced.
- No a11y semantics removed in this phase.
- Remaining gap: broaden deterministic coverage for focus order and large-text clipping in a dedicated a11y phase.

## Maintainability assessment
### Strengths
- Clear seams remain in place: provider executor/orchestrator, availability policy, ViewModel state flows, deterministic test runner overrides.
- Minimal blast radius for this phase (targeted ViewModel logic + tests + docs + CI workflow).

### Complexity hotspots to watch
- Provider merge/dedupe logic remains dense and sensitive to edge cases.
- ViewModel message synthesis can drift unless protected by focused tests.

## Documentation truthfulness assessment
### Mismatches found and corrected
- Roadmap text claiming OFF retries were disabled did not match runtime (single explicit-action retry exists).
- Roadmap text still referenced old segmented Add Food search modes instead of current unified flow.
- Threat model marked billing out-of-scope despite shipped entitlement/paywall behavior.

### Updated files
- `README.md`
- `docs/verification.md`
- `docs/roadmap.md`
- `docs/threat-model.md`
- `SECURITY.md`
- `CHANGELOG.md`

## Testing and automation gaps
### Improved now
- Added PR-level CI workflow for deterministic release gates.
- Added unit test enforcement that CI workflow includes the three required gate commands.

### Still missing
- Stronger CI assertions for emulator boot-health diagnostics.
- Snapshot-style corpus coverage for serving-size edge normalization across providers.

## Next 10 phases roadmap (Phase 21-30, historical snapshot)
This roadmap snapshot is historical to the Phase 20 review context and is superseded by the Phase 25 direction reset:
- `docs/phase-25/phase-25-plan.md`
- `docs/roadmap.md`

### Phase 21: Provider contract conformance pack
- Goal: lock provider mapping behavior to a canonical fixture suite.
- Why: reduce regressions from upstream payload drift.
- Minimal deliverables: shared fixtures + provider contract test harness.
- Acceptance criteria: OFF/USDA/Nutritionix pass canonical mapping assertions.
- Test strategy: deterministic JVM fixture tests only.
- Risks/non-goals: no new providers, no schema changes.

### Phase 22: Query normalization hardening
- Goal: deterministic normalization for punctuation/unit/brand variants.
- Why: improves hit-rate and user trust without extra networking.
- Minimal deliverables: normalization corpus + utility cleanups.
- Acceptance criteria: stable expected outputs on golden corpus.
- Test strategy: unit golden tests + VM integration tests.
- Risks/non-goals: no ML expansion.

### Phase 23: Barcode loop reliability pass
- Goal: stabilize explicit scan-to-result behavior under poor conditions.
- Why: barcode flow is a core fast path.
- Minimal deliverables: clearer scan/retry states and debounce hardening.
- Acceptance criteria: no duplicate lookups, graceful retry copy.
- Test strategy: deterministic VM/instrumentation tests with fakes.
- Risks/non-goals: no new permissions/background camera.

### Phase 24: Serving-size normalization v2
- Goal: reduce ambiguous serving conversions from online imports.
- Why: better data quality and log correctness.
- Minimal deliverables: parser improvements + fallback text guidance.
- Acceptance criteria: known problematic serving strings normalize consistently.
- Test strategy: corpus-based unit tests + save/log regressions.
- Risks/non-goals: no nutrition inference.

### Phase 25: Accessibility confidence pass
- Goal: ensure keyboard/focus/TalkBack quality on core flows.
- Why: inclusion + fewer interaction failures.
- Minimal deliverables: semantics/focus fixes on Add Food/Settings/Insights.
- Acceptance criteria: stable focus order and readable controls at large text.
- Test strategy: deterministic compose semantics tests.
- Risks/non-goals: no visual redesign expansion.

### Phase 26: Export trustworthiness hardening
- Goal: improve export preview fidelity and redaction confidence.
- Why: export is high-value and privacy-sensitive.
- Minimal deliverables: stricter schema docs + output consistency tests.
- Acceptance criteria: previewed/exported JSON/CSV align deterministically.
- Test strategy: JVM snapshot tests.
- Risks/non-goals: no cloud upload/sync.

### Phase 27: Add Food performance budget pass
- Goal: protect responsiveness with larger local datasets.
- Why: daily usability at scale.
- Minimal deliverables: explicit perf budgets + lightweight benchmark checks.
- Acceptance criteria: key interactions stay within defined thresholds.
- Test strategy: local benchmark + deterministic smoke checks.
- Risks/non-goals: no large architecture rewrite.

### Phase 28: Security hardening evidence pass
- Goal: tighten key/logging/privacy guarantees with auditable checks.
- Why: security is a product differentiator.
- Minimal deliverables: key-handling audit, log scrub checks, threat-model refresh.
- Acceptance criteria: no sensitive content in logs; docs match runtime.
- Test strategy: static grep checks + docs consistency tests.
- Risks/non-goals: no backend introduction.

### Phase 29: Release automation maturity
- Goal: reduce manual release risk.
- Why: safer and faster release cadence.
- Minimal deliverables: stricter PR gate policy + release checklist automation hooks.
- Acceptance criteria: release checklist executable from repo with deterministic outputs.
- Test strategy: CI workflow assertions + docs tests.
- Risks/non-goals: no vendor/tooling overhaul.

### Phase 30: Product simplification pass
- Goal: remove low-value complexity from primary logging flows.
- Why: clarity and maintainability.
- Minimal deliverables: evidence-backed simplifications with regression tests.
- Acceptance criteria: fewer user steps with no behavior regressions.
- Test strategy: deterministic UI smoke + ViewModel tests.
- Risks/non-goals: no unrelated feature additions.

## Phase 20 high-leverage changes (implemented)
1. Suppressed top-level online error/setup banner when results already exist; provider statuses remain visible.
2. Added deterministic CI release-gate workflow for PRs.
3. Added documentation consistency test for CI gate presence.
4. Aligned README/verification/security/threat-model/roadmap/changelog with runtime behavior.

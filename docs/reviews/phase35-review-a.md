# Phase 35 Review A: Evidence Audit and Rebaseline (Phases 36-40)

Date: 2026-02-12  
Branch context: `phase-35-review-a` (created from `main`)  
Baseline commit reviewed: `2ae0ba4` (Phase 34 merged)

## Evidence sources used
- `README.md`
- `docs/product-vision.md`
- `docs/roadmap.md`
- `docs/architecture.md`
- `docs/provider_contract.md`
- `docs/threat-model.md`
- `SECURITY.md`
- `docs/verification.md`
- `.github/workflows/android-gates.yml`
- `android/app/src/main/java/com/openfuel/app/ui/screens/HomeScreen.kt`
- `android/app/src/main/java/com/openfuel/app/ui/screens/HistoryScreen.kt`
- `android/app/src/main/java/com/openfuel/app/ui/screens/FoodLibraryScreen.kt`
- `android/app/src/main/java/com/openfuel/app/ui/screens/AddFoodScreen.kt`
- `android/app/src/main/java/com/openfuel/app/ui/screens/InsightsScreen.kt`
- `android/app/src/main/java/com/openfuel/app/ui/screens/SettingsScreen.kt`
- `android/app/src/main/java/com/openfuel/app/viewmodel/HomeViewModel.kt`
- `android/app/src/main/java/com/openfuel/app/viewmodel/AddFoodViewModel.kt`
- `android/app/src/main/java/com/openfuel/app/viewmodel/ScanBarcodeViewModel.kt`
- `android/app/src/main/java/com/openfuel/app/viewmodel/InsightsViewModel.kt`
- `android/app/src/main/java/com/openfuel/app/viewmodel/SettingsViewModel.kt`
- `android/app/src/main/java/com/openfuel/app/domain/model/GoalProfileDefaults.kt`
- `android/app/src/main/java/com/openfuel/app/domain/retention/RetentionPolicy.kt`
- `android/app/src/main/java/com/openfuel/app/data/analytics/LocalAnalyticsStore.kt`
- `android/app/src/main/java/com/openfuel/app/AppContainer.kt`
- `android/app/src/androidTest/java/com/openfuel/app/ui/screens/UnifiedSearchSmokeTest.kt`
- `android/app/src/androidTest/java/com/openfuel/app/ui/screens/OnlineSearchGatingTest.kt`
- `android/app/src/androidTest/java/com/openfuel/app/ui/screens/GoalProfileOnboardingTest.kt`
- `android/app/src/androidTest/java/com/openfuel/app/OpenFuelAndroidTestRunner.kt`
- `android/app/src/test/java/com/openfuel/app/domain/retention/RetentionPolicyTest.kt`
- `android/app/src/test/java/com/openfuel/app/data/analytics/LocalAnalyticsStoreTest.kt`
- `android/app/src/test/java/com/openfuel/app/viewmodel/HomeViewModelTest.kt`

## What is true now (product and engineering)

### Today
- **Verified**: Today has a dominant primary action via `home_add_food_fab`, plus a calm empty-day state and progressive disclosure for empty meal slots and totals details.  
  Evidence: `android/app/src/main/java/com/openfuel/app/ui/screens/HomeScreen.kt`, `android/app/src/androidTest/java/com/openfuel/app/ui/screens/TodayScreenSmokeTest.kt`
- **Verified**: Fast Log reminder appears only under policy constraints and is user-dismissible.  
  Evidence: `android/app/src/main/java/com/openfuel/app/viewmodel/HomeViewModel.kt`, `android/app/src/main/java/com/openfuel/app/domain/retention/RetentionPolicy.kt`
- **Hypothesis**: For repeat users, “next log” may still exceed the intended under-10-second loop because the handoff to Add Food is still required.  
  Evidence: `android/app/src/main/java/com/openfuel/app/ui/screens/HomeScreen.kt` (no direct recents/favourites logging action on Today)

### History
- **Verified**: History is stable and minimal, showing logged days with day-level drill-in routing to Today.  
  Evidence: `android/app/src/main/java/com/openfuel/app/ui/screens/HistoryScreen.kt`, `android/app/src/main/java/com/openfuel/app/viewmodel/HistoryViewModel.kt`
- **Hypothesis**: History currently has low interpretive utility beyond date navigation because it lacks quick summary metadata per day.  
  Evidence: `android/app/src/main/java/com/openfuel/app/ui/screens/HistoryScreen.kt`

### Foods
- **Verified**: Foods supports local-only search and Add Food handoff without online side effects.  
  Evidence: `android/app/src/main/java/com/openfuel/app/ui/screens/FoodLibraryScreen.kt`, `android/app/src/main/java/com/openfuel/app/viewmodel/FoodLibraryViewModel.kt`
- **Hypothesis**: Known-food relog speed remains bounded by flow handoff rather than in-place log action on Foods.  
  Evidence: `android/app/src/main/java/com/openfuel/app/ui/screens/FoodLibraryScreen.kt`

### Add Food (explicit-action and trust checks)
- **Verified**: Query typing and debounce drive local search only; online execution occurs only via explicit actions (`Search online`, `Refresh online`) with user-initiated guard token.  
  Evidence: `android/app/src/main/java/com/openfuel/app/viewmodel/AddFoodViewModel.kt`, `android/app/src/androidTest/java/com/openfuel/app/ui/screens/UnifiedSearchSmokeTest.kt`
- **Verified**: Online trust cues are present (`Source`, `Completeness`, `Needs review`, `Why this result`) and top-level online status is deduplicated.  
  Evidence: `android/app/src/main/java/com/openfuel/app/ui/screens/AddFoodScreen.kt`, `android/app/src/main/java/com/openfuel/app/viewmodel/SearchUserCopy.kt`
- **Hypothesis**: Add Food online section still presents high information density when expanded, which may slow scan speed.  
  Evidence: `android/app/src/main/java/com/openfuel/app/ui/screens/AddFoodScreen.kt`

### Insights
- **Verified**: Insights is profile-aware, but core content remains Pro-gated with clear entry state.  
  Evidence: `android/app/src/main/java/com/openfuel/app/ui/screens/InsightsScreen.kt`, `android/app/src/main/java/com/openfuel/app/viewmodel/InsightsViewModel.kt`
- **Hypothesis**: Non-Pro users may not yet receive enough immediate utility from Insights beyond profile focus and paywall entry point.  
  Evidence: `android/app/src/main/java/com/openfuel/app/ui/screens/InsightsScreen.kt`

### Settings
- **Verified**: Settings includes reversible Goal Profile editing, goals customisation, reminder controls, provider setup visibility, export flows, and debug diagnostics controls.  
  Evidence: `android/app/src/main/java/com/openfuel/app/ui/screens/SettingsScreen.kt`, `android/app/src/main/java/com/openfuel/app/viewmodel/SettingsViewModel.kt`
- **Verified**: Goal defaults are applied only when `goalsCustomised` is false.  
  Evidence: `android/app/src/main/java/com/openfuel/app/viewmodel/HomeViewModel.kt`, `android/app/src/main/java/com/openfuel/app/viewmodel/SettingsViewModel.kt`, `android/app/src/test/java/com/openfuel/app/viewmodel/HomeViewModelTest.kt`

### Goal Profiles v1 (Phase 34)
- **Verified**: First-run profile picker exists, includes skip path, and is shown once by persisted onboarding completion state.  
  Evidence: `android/app/src/main/java/com/openfuel/app/ui/screens/HomeScreen.kt`, `android/app/src/main/java/com/openfuel/app/viewmodel/HomeViewModel.kt`, `android/app/src/androidTest/java/com/openfuel/app/ui/screens/GoalProfileOnboardingTest.kt`
- **Verified**: Profiles are non-clinical and include explicit disclaimer text.  
  Evidence: `android/app/src/main/java/com/openfuel/app/domain/model/GoalProfileDefaults.kt`, `android/app/src/main/java/com/openfuel/app/ui/screens/HomeScreen.kt`, `android/app/src/main/java/com/openfuel/app/ui/screens/SettingsScreen.kt`

## Top 10 issues and opportunities (ranked)

| Rank | Item | Why it matters | Evidence | Impact | Effort | Recommendation |
|---|---|---|---|---|---|---|
| 1 | Repeat logging still relies on Today -> Add Food handoff | Core promise is speed for repeat users | `HomeScreen.kt` | H | M | Add direct known-food quick-log path from Today in Phase 36. |
| 2 | Add Food online expanded state is dense | Slower scan and decision time | `AddFoodScreen.kt` | H | M | Compress secondary diagnostics and trust text hierarchy in Phase 36. |
| 3 | History is low-signal for quick decisions | Users cannot quickly assess prior-day quality | `HistoryScreen.kt` | M | M | Add day summary metadata (kcal + entry count) in Phase 36 or 37. |
| 4 | Foods has no direct relog action | Extra navigation step for common tasks | `FoodLibraryScreen.kt` | M | M | Add optional quick-log action in Foods list item in Phase 36. |
| 5 | Profile effect feedback is subtle post-change | Reduced confidence in reversibility and impact | `SettingsScreen.kt`, `HomeScreen.kt` | M | L | Add explicit “what changed” summary after profile save in Phase 37. |
| 6 | Trust cues concentrated in Add Food only | Inconsistent trust language across surfaces | `AddFoodScreen.kt`, `HistoryScreen.kt`, `FoodLibraryScreen.kt` | M | M | Standardise trust cue pattern across Foods/History in Phase 37/39. |
| 7 | Local and CI emulator profiles differ | Device-profile mismatch can mask reliability issues | `.github/workflows/android-gates.yml`, `docs/verification.md` | M | L | Add explicit note and periodic parity run protocol in Phase 35 follow-up docs. |
| 8 | Cast-heavy multi-flow combine patterns in ViewModels | Maintenance and runtime cast risk as state grows | `HomeViewModel.kt`, `SettingsViewModel.kt` | M | M | Introduce typed combine wrappers in a bounded refactor phase. |
| 9 | Documentation status lags shipped phases | Planning drift and contributor confusion | `README.md`, `CHANGELOG.md` | M | L | Align status and changelog as part of this phase. |
| 10 | Entitlement refresh on settings init may be misread against explicit-action policy | Policy ambiguity risk in future changes | `SettingsViewModel.kt`, `PlayBillingEntitlementService.kt` | M | L | Clarify boundary in docs: provider lookups are explicit-action; billing refresh is entitlement sync. |

## Risk register (top 10)

| Risk | Severity | Likelihood | Trigger or signal | Mitigation | Evidence |
|---|---|---|---|---|---|
| Repeat log flow remains too slow for power users | H | H | Increased taps and context switching | Prioritise fast-path from Today in Phase 36 | `HomeScreen.kt` |
| Information overload in Add Food online state | H | M | Longer decision time, confusion on status | Progressive disclosure and compact trust surfaces | `AddFoodScreen.kt` |
| Goal defaults overwrite user intent | H | L | User-set goals unexpectedly changed | Keep `goalsCustomised` guard + regression tests | `HomeViewModel.kt`, `SettingsViewModel.kt`, `HomeViewModelTest.kt` |
| Explicit-action invariant regresses | H | L | Online calls from typing/passive states | Preserve dedicated gating tests as release blockers | `UnifiedSearchSmokeTest.kt`, `OnlineSearchGatingTest.kt` |
| Analytics privacy guardrails drift | H | L | Sensitive keys accepted in events | Keep allowlist and rejection tests mandatory | `LocalAnalyticsStore.kt`, `LocalAnalyticsStoreTest.kt` |
| Instrumentation flake from scroll semantics | M | M | Intermittent CI failures on UI tests | Prefer stable tags + `performScrollTo` patterns only | `GoalProfileOnboardingTest.kt` |
| Local vs CI emulator profile divergence | M | M | Test passes locally but fails CI | Add parity validation checklist and periodic profile cross-run | `android-gates.yml`, `docs/verification.md` |
| Trust cues become inconsistent across screens | M | M | User confusion about data confidence | Centralise trust language standards in Phase 37/39 | `SearchUserCopy.kt`, `AddFoodScreen.kt` |
| Documentation drift from code truth | M | H | Stale status/roadmap claims | Phase reviews must include docs truth sync | `README.md`, `CHANGELOG.md`, `docs/roadmap.md` |
| Scope creep in phases 36-40 | H | M | Overloaded phase plans with low delivery confidence | Enforce explicit scope-out and acceptance criteria per phase | `docs/roadmap.md` |

## Stop / Start / Continue

### Stop doing
- Stop adding adjacent feature surfaces before reducing repeat-log path friction.
- Stop phase planning without explicit risk and acceptance criteria per phase.
- Stop allowing docs status to lag shipped commits.

### Start doing
- Start measuring “time to first log from Today” as a primary quality KPI.
- Start treating Today, Add Food, and Foods as one logging system in planning and QA.
- Start explicitly documenting local-vs-CI test environment differences in review phases.

### Continue doing
- Continue explicit-action networking enforcement with guard-token patterns.
- Continue deterministic fixture-based provider and mapper testing.
- Continue local-only diagnostics and schema-guarded event handling.

## Rebaseline proposal for phases 36-40

## Phase 36: Foods 10x as one system
- **Goal**: Make known-food logging path consistently fast across Today, Add Food, and Foods.
- **Scope in**:
  - Direct known-food quick-log entry points from Today and Foods.
  - Reduce Add Food decision latency for common repeat tasks.
  - Keep trust cues visible but compact.
- **Scope out**:
  - No provider expansion.
  - No new feature family.
- **Risks**:
  - Triggering unintended online execution via new shortcuts.
  - UI clutter from over-surfaced actions.
- **Acceptance criteria**:
  - Repeat user can start logging from Today in two taps or less.
  - No online execution without explicit action.
  - Existing test tags preserved; additive tags only.
- **Test strategy**:
  - ViewModel regressions for explicit-action boundaries.
  - Instrumentation smoke tests for fast path and no hidden online calls.

## Phase 37: Weekly Review v1 (calm, useful, short)
- **Goal**: Deliver one weekly review ritual with one actionable recommendation.
- **Scope in**:
  - One concise summary card.
  - One recommendation with plain-language rationale.
  - Profile-aware framing in Insights.
- **Scope out**:
  - No coaching system.
  - No punitive mechanics.
- **Risks**:
  - Low perceived utility if recommendation is generic.
- **Acceptance criteria**:
  - Weekly review can be completed in under 60 seconds.
  - Recommendation includes “why” explanation and avoids judgemental language.
- **Test strategy**:
  - Unit tests for insight calculation and recommendation selection.
  - Deterministic UI smoke for visibility and copy structure.

## Phase 38: Habit loops v1 (utility-led controls)
- **Goal**: Improve consistency through optional, calm reminders.
- **Scope in**:
  - Reminder controls clarity (caps, quiet hours, dismissal behaviour).
  - Reminder CTA quality tied to useful next action.
- **Scope out**:
  - No variable-ratio rewards.
  - No urgency timers or shame cues.
- **Risks**:
  - Reminder fatigue or dismissal loops.
- **Acceptance criteria**:
  - Reminder display obeys per-day/per-session caps and cooldown.
  - User can disable reminders and quiet hours from Settings.
- **Test strategy**:
  - Policy unit matrix tests.
  - Deterministic instrumentation assertions for reminder visibility controls.

## Phase 39: Trust and data quality v1
- **Goal**: Standardise trust cues and correction flows across core surfaces.
- **Scope in**:
  - Consistent source/completeness/needs-review language.
  - Explicit correction-safe messaging in relevant screens.
- **Scope out**:
  - No hidden auto-corrections.
  - No clinical claims.
- **Risks**:
  - Overloading users with trust metadata.
- **Acceptance criteria**:
  - Trust cue pattern is consistent in Add Food, Foods, and relevant detail views.
  - Missing/uncertain data is labelled clearly, never silently inferred.
- **Test strategy**:
  - Shared trust-signal unit tests.
  - Snapshot/Compose assertions for trust cue visibility.

## Phase 40: Review B
- **Goal**: Re-evaluate retention, reliability, and delivery risk after phases 36-39.
- **Scope in**:
  - Product + technical evidence audit.
  - Reprioritisation of phases 41-45 based on observed outcomes.
  - Docs truth sync.
- **Scope out**:
  - No net-new feature expansion during review.
- **Risks**:
  - Scope drift if review outcomes are not enforced.
- **Acceptance criteria**:
  - Updated ranked backlog and risk register.
  - Explicit keep/cut decisions for the next planning window.
- **Test strategy**:
  - Gate reliability report.
  - Test flake trend review.

## Invariant check
- **Verified**: Offline-first default remains intact for core logging.  
  Evidence: `docs/architecture.md`, `README.md`, local repositories in `data/repository`.
- **Verified**: Explicit-action networking remains enforced for provider lookups and barcode lookups.  
  Evidence: `AddFoodViewModel.kt`, `ScanBarcodeViewModel.kt`, `UnifiedSearchSmokeTest.kt`, `OnlineSearchGatingTest.kt`.
- **Verified**: Deterministic gate order remains canonical in docs and CI.  
  Evidence: `docs/verification.md`, `.github/workflows/android-gates.yml`.
- **Verified**: No sensitive telemetry payload capture patterns introduced in current event schema.  
  Evidence: `LocalAnalyticsStore.kt`, `LocalAnalyticsStoreTest.kt`.

# Phase 40 Review B: Evidence Audit and Rolling Roadmap Refresh

Mode: IMPLEMENT NOW (DOCS-ONLY)
Guard: No non-.md changes permitted. If any code change is required, stop and ask for explicit approval.

Date: 2026-02-12  
Branch context: `phase-40-review-b` (review branch intent; docs-only checkpoint)

## Evidence sources used
- `README.md`
- `docs/README.md`
- `docs/product-vision.md`
- `docs/roadmap.md`
- `docs/architecture.md`
- `docs/provider_contract.md`
- `docs/threat-model.md`
- `SECURITY.md`
- `docs/verification.md`
- `.github/workflows/android-gates.yml`
- `CHANGELOG.md`
- `docs/reviews/phase35-review-a.md`
- `android/app/src/main/java/com/openfuel/app/ui/screens/HomeScreen.kt`
- `android/app/src/main/java/com/openfuel/app/ui/screens/AddFoodScreen.kt`
- `android/app/src/main/java/com/openfuel/app/ui/screens/FoodLibraryScreen.kt`
- `android/app/src/main/java/com/openfuel/app/ui/screens/InsightsScreen.kt`
- `android/app/src/main/java/com/openfuel/app/ui/screens/SettingsScreen.kt`
- `android/app/src/main/java/com/openfuel/app/ui/screens/WeeklyReviewScreen.kt`
- `android/app/src/main/java/com/openfuel/app/ui/screens/HistoryScreen.kt`
- `android/app/src/main/java/com/openfuel/app/viewmodel/HomeViewModel.kt`
- `android/app/src/main/java/com/openfuel/app/viewmodel/AddFoodViewModel.kt`
- `android/app/src/main/java/com/openfuel/app/viewmodel/FoodLibraryViewModel.kt`
- `android/app/src/main/java/com/openfuel/app/viewmodel/WeeklyReviewViewModel.kt`
- `android/app/src/main/java/com/openfuel/app/domain/retention/RetentionPolicy.kt`
- `android/app/src/main/java/com/openfuel/app/domain/quality/FoodDataQuality.kt`
- `android/app/src/androidTest/java/com/openfuel/app/ui/screens/OnlineSearchGatingTest.kt`
- `android/app/src/androidTest/java/com/openfuel/app/ui/screens/UnifiedSearchSmokeTest.kt`

## What is true now (product and engineering)

### Product surfaces
- **Verified**: Today keeps one visually primary Add Food CTA (`home_add_food_fab`) with secondary reminder links.  
  Evidence: `android/app/src/main/java/com/openfuel/app/ui/screens/HomeScreen.kt`
- **Verified**: Add Food online execution remains explicit-action only with dedicated actions and guard-token flow.  
  Evidence: `android/app/src/main/java/com/openfuel/app/viewmodel/AddFoodViewModel.kt`, `android/app/src/androidTest/java/com/openfuel/app/ui/screens/OnlineSearchGatingTest.kt`
- **Verified**: Foods/Add Food local rows include trust cues and correction affordances using shared quality signals.  
  Evidence: `android/app/src/main/java/com/openfuel/app/ui/screens/FoodLibraryScreen.kt`, `android/app/src/main/java/com/openfuel/app/ui/screens/AddFoodScreen.kt`, `android/app/src/main/java/com/openfuel/app/domain/quality/FoodDataQuality.kt`
- **Verified**: Weekly Review supports one actionable summary with explicit low-confidence/correction path.  
  Evidence: `android/app/src/main/java/com/openfuel/app/ui/screens/WeeklyReviewScreen.kt`, `android/app/src/main/java/com/openfuel/app/viewmodel/WeeklyReviewViewModel.kt`
- **Hypothesis**: Add Food expanded online region is still visually dense and can slow scan speed on smaller devices.  
  Evidence: `android/app/src/main/java/com/openfuel/app/ui/screens/AddFoodScreen.kt`
- **Hypothesis**: Insights first viewport hierarchy still under-emphasises weekly review utility for non-Pro users.  
  Evidence: `android/app/src/main/java/com/openfuel/app/ui/screens/InsightsScreen.kt`

### Engineering health
- **Verified**: Gate order is consistent in docs and CI (`test`, `assembleDebug`, connected tests).  
  Evidence: `docs/verification.md`, `.github/workflows/android-gates.yml`
- **Verified**: Explicit-action networking invariants remain represented in tests.  
  Evidence: `android/app/src/androidTest/java/com/openfuel/app/ui/screens/UnifiedSearchSmokeTest.kt`, `android/app/src/androidTest/java/com/openfuel/app/ui/screens/OnlineSearchGatingTest.kt`
- **Hypothesis**: Large screen/viewmodel files remain merge-conflict hotspots due breadth and cast-heavy state wiring.  
  Evidence: `android/app/src/main/java/com/openfuel/app/ui/screens/HomeScreen.kt`, `android/app/src/main/java/com/openfuel/app/viewmodel/HomeViewModel.kt`, `android/app/src/main/java/com/openfuel/app/ui/screens/AddFoodScreen.kt`

## Ranked top 10 issues and opportunities

| Rank | Item | Why it matters | Evidence | Impact | Effort | Recommendation |
|---|---|---|---|---|---|---|
| 1 | Add Food expanded online density | Slows decision speed in core flow | `android/app/src/main/java/com/openfuel/app/ui/screens/AddFoodScreen.kt` | H | M | Tighten hierarchy and metadata density in Phase 42. |
| 2 | Insights first viewport utility | Weekly review and trust cues should be easier to spot | `android/app/src/main/java/com/openfuel/app/ui/screens/InsightsScreen.kt` | M | M | Raise weekly review and confidence context in Phase 46. |
| 3 | File-size conflict hotspots | Slows delivery and raises merge risk | `HomeScreen.kt`, `AddFoodScreen.kt`, `HomeViewModel.kt` | H | M | Keep slices smaller and extract focused composables in-place only when needed. |
| 4 | Quality language consistency | Trust cues should be identical across surfaces | `FoodLibraryScreen.kt`, `WeeklyReviewScreen.kt`, `AddFoodScreen.kt` | M | L | Standardise cue copy in Phase 44. |
| 5 | Weekly review sparse-data confidence | Needs stronger coverage framing before suggestion | `WeeklyReviewScreen.kt` | M | L | Prioritise clarity pass in Phase 46. |
| 6 | History signal depth | Day rows still low-context for quick retrospective decisions | `android/app/src/main/java/com/openfuel/app/ui/screens/HistoryScreen.kt` | M | M | Add bounded metadata in outlined horizon. |
| 7 | Roadmap drift risk | Roadmap lacked rolling 51-60 outline | `docs/roadmap.md` | M | L | Adopt rolling lock/outline structure in this phase. |
| 8 | README status drift | Front-door status was stale after 36-39 | `README.md` | M | L | Keep release/status updates mandatory in review phases. |
| 9 | Changelog continuity gap | Missing neutral shipped notes for 36-39 | `CHANGELOG.md` | M | L | Add factual entries in Unreleased. |
| 10 | CI vs local profile variance | Emulator profile mismatch can hide edge behaviour | `.github/workflows/android-gates.yml`, `docs/verification.md` | M | L | Maintain parity notes and explicit device profile checks in review phases. |

## Risk register (top 10)

| Risk | Severity | Likelihood | Trigger/signal | Mitigation | Evidence |
|---|---|---|---|---|---|
| Explicit-action networking regression | High | Low | Online execution from typing/passive states | Keep gating tests blocking merges | `AddFoodViewModel.kt`, `OnlineSearchGatingTest.kt` |
| UI density regression in Add Food | High | Medium | Increased time-to-selection | Restrict trust metadata to compact hierarchy | `AddFoodScreen.kt` |
| CTA clutter reintroduced on Today | High | Low | Multiple primary-looking controls | Keep FAB as sole primary, enforce smoke assertions | `HomeScreen.kt`, `TodayScreenSmokeTest.kt` |
| Merge conflicts in large files | Medium | High | Frequent conflict fixes in screen/viewmodel cores | Smaller atomic commits; avoid cross-cutting edits | `HomeScreen.kt`, `AddFoodScreen.kt`, `HomeViewModel.kt` |
| Inconsistent trust cue semantics | Medium | Medium | Different wording across screens | Shared cue terms and tests in Phase 44 | `FoodLibraryScreen.kt`, `WeeklyReviewScreen.kt` |
| Sparse-data over-interpretation | Medium | Medium | Strong suggestions with weak data | Coverage-first messaging and guardrails | `WeeklyReviewViewModel.kt` |
| Accessibility drift | Medium | Medium | Semantics regressions in complex cards | Explicit semantics test focus in Phase 47 | `HomeScreen.kt`, `InsightsScreen.kt` |
| Docs/runtime divergence | Medium | Medium | README/roadmap/changelog stale after shipped work | Review-phase docs sync as mandatory output | `README.md`, `CHANGELOG.md`, `docs/roadmap.md` |
| CI profile mismatch surprises | Medium | Medium | Local pass, CI fail or vice versa | Keep deterministic runner and profile documentation | `.github/workflows/android-gates.yml`, `docs/verification.md` |
| Scope creep in locked phases | High | Medium | New features displacing core-loop work | Keep strict scope-in/scope-out and review checkpoints | `docs/roadmap.md` |

## Stop / Start / Continue

### Stop
- Stop expanding new feature families before core capture clarity is complete.
- Stop shipping review phases without updating README and changelog status.
- Stop broad multi-surface edits in one commit for high-conflict files.

### Start
- Start rolling roadmap discipline: lock next 10, outline following 10 at review checkpoints.
- Start treating Add Food information density as a first-order quality metric.
- Start enforcing quality-gate verdict language (`Ship`, `Revise`, `Don’t ship`) in review artefacts.

### Continue
- Continue explicit-action networking guardrails and deterministic tests.
- Continue offline-first default and local-first data posture.
- Continue additive, semantics-driven test tags instead of brittle text assertions.

## Invariant checks
- **Verified**: Offline-first default remains intact for core logging workflows.  
  Evidence: `docs/architecture.md`, `README.md`, local repositories and viewmodels.
- **Verified**: Explicit-action networking remains in place for provider lookups.  
  Evidence: `docs/provider_contract.md`, `AddFoodViewModel.kt`, `UnifiedSearchSmokeTest.kt`, `OnlineSearchGatingTest.kt`
- **Verified**: Deterministic gates remain canonical in docs and CI workflows.  
  Evidence: `docs/verification.md`, `.github/workflows/android-gates.yml`
- **Verified**: No sensitive telemetry/ad-ID posture remains unchanged in current policy docs.  
  Evidence: `SECURITY.md`, `docs/threat-model.md`

## Rolling roadmap update rationale
- Phases 36-39 are now reflected as shipped outcomes in status docs.
- Phase 40 adopts rolling roadmap mechanics: lock 41-50 and outline 51-60.
- Locked phases prioritise speed-to-log, trust clarity, reliability, accessibility, and release readiness.
- Outlined phases hold future options without prematurely committing implementation detail.
- Wearables remain a future candidate only, explicitly out of scope for 41-50.

## Apple-style Quality Gate

### Today (`android/app/src/main/java/com/openfuel/app/ui/screens/HomeScreen.kt`)
- **Verdict**: Ship
- Why:
  - One primary Add Food control via FAB remains clear.
  - Progressive disclosure reduces clutter for secondary details.
  - Reminder affordances are secondary-link style.

### Add Food (`android/app/src/main/java/com/openfuel/app/ui/screens/AddFoodScreen.kt`)
- **Verdict**: Revise
- Why:
  - Expanded online section remains visually dense.
  - Trust and status cues can compete in scan order.
  - Repeat local-item actions can be buried when expanded.
- Fixes:
  - Reduce duplicated secondary metadata in expanded cards. File: `AddFoodScreen.kt`. AC: one trust line plus optional details expander.
  - Keep summary-first hierarchy above item cards. File: `AddFoodScreen.kt`. AC: summary visible without scroll on common device heights.
  - Keep explicit-action controls visually stable. File: `AddFoodScreen.kt`. AC: `Search online` and `Refresh online` remain visible when section is present.
- Do-not-do:
  - No auto online fetch.
  - No new CTA family.
  - No provider logic changes.

### Foods (`android/app/src/main/java/com/openfuel/app/ui/screens/FoodLibraryScreen.kt`)
- **Verdict**: Revise
- Why:
  - Section transition between blank/non-blank query can feel abrupt.
  - Confidence cue placement needs tighter consistency.
- Fixes:
  - Standardise trust cue position under identity line. Files: `FoodLibraryScreen.kt`, `LocalFoodResultRow.kt`. AC: cue placement consistent across recents, favourites, local.
  - Tighten empty-search helper copy. File: `FoodLibraryScreen.kt`. AC: copy remains factual and action-oriented.
- Do-not-do:
  - No new navigation tabs.
  - No online trigger changes.

### Insights (`android/app/src/main/java/com/openfuel/app/ui/screens/InsightsScreen.kt`)
- **Verdict**: Revise
- Why:
  - Weekly review utility can appear secondary in scan order.
  - Trust interpretation cues are less prominent than core capture surfaces.
- Fixes:
  - Promote weekly review entry placement. File: `InsightsScreen.kt`. AC: entry visible in first viewport on standard emulator profile.
  - Align trust cue terms with shared labels. File: `InsightsScreen.kt`. AC: only `Complete` and `Needs review` terminology.
- Do-not-do:
  - No monetisation changes.
  - No new analytics events.

### Settings (`android/app/src/main/java/com/openfuel/app/ui/screens/SettingsScreen.kt`)
- **Verdict**: Ship
- Why:
  - Grouping and reversible settings remain clear.
  - Reminder controls are explicit and user-controlled.
  - Goal profile editing is understandable.

### Weekly Review (`android/app/src/main/java/com/openfuel/app/ui/screens/WeeklyReviewScreen.kt`)
- **Verdict**: Revise
- Why:
  - Sparse-data and correction-path messaging can be clearer at first glance.
  - Suggestion confidence qualifiers can be stronger.
- Fixes:
  - Prefix summary with data coverage statement. File: `WeeklyReviewScreen.kt`. AC: coverage appears before suggestion block.
  - Keep `Review and fix` visible for low-confidence states. File: `WeeklyReviewScreen.kt`. AC: deterministic visibility when low-confidence flag is true.
- Do-not-do:
  - No coaching expansion.
  - No pressure language.

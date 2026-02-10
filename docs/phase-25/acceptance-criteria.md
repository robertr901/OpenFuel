# Phase 25 Acceptance Criteria (Measurable)

## 1) Core logging flows
| ID | Criterion | Pass condition | Fail condition | Measurement |
|---|---|---|---|---|
| CL-1 | Known-food logging speed | Median completion time <= 20s from Add Food open to saved log entry | Median > 20s | Timed usability run (n>=20 runs) |
| CL-2 | Edit/delete reversibility | Edit and delete actions complete without data loss; undo available where applicable | Any irreversible destructive action without clear recovery | Manual QA + deterministic UI tests |
| CL-3 | Search consistency | Local search results remain stable for same input and dataset | Inconsistent ordering without data change | Deterministic test dataset snapshots |

## 2) Offline behavior and explicit network actions
| ID | Criterion | Pass condition | Fail condition | Measurement |
|---|---|---|---|---|
| OF-1 | Offline-first operation | Core logging, viewing, editing, and local search fully usable in airplane mode | Core flow blocked by missing internet | Airplane-mode smoke test |
| OF-2 | Explicit-action networking | 0 online requests occur without user explicit action (Search online/Refresh online/explicit scan lookup) | Any implicit/background request | Network trace + instrumentation checks |
| OF-3 | Online disabled behavior | When online lookup is disabled, no provider execution occurs and user gets clear message | Silent no-op or hidden request | ViewModel/instrumentation tests |

## 3) Export/import integrity
| ID | Criterion | Pass condition | Fail condition | Measurement |
|---|---|---|---|---|
| EI-1 | Round-trip integrity | Export/import round-trip equality for required fields = 100% | Any required field mismatch after round-trip | Deterministic export/import test harness |
| EI-2 | Explicit export control | Export is always explicit user action with no auto-upload | Automatic upload/share path exists | Flow inspection + UI tests |
| EI-3 | Failure handling | Corrupt/invalid import surfaces clear, non-crashing message | Crash or partial silent import | Error-path tests |

## 4) Performance SLOs
| ID | Criterion | Pass condition | Fail condition | Measurement |
|---|---|---|---|---|
| PF-1 | Cold start latency | p50 <= 2.0s, p95 <= 3.0s on reference mid-range device | Exceeds SLO | Perf benchmark checklist |
| PF-2 | Local search latency | Results visible p95 <= 250ms after debounce | >250ms p95 | Instrumented timing on deterministic dataset |
| PF-3 | Add flow responsiveness | No visible UI jank in core Add Food interaction path | Dropped frames or blocked input in standard flow | Profiled run + QA signoff |

## 5) Premium/Pro UX respectfulness
| ID | Criterion | Pass condition | Fail condition | Measurement |
|---|---|---|---|---|
| PR-1 | Non-manipulative paywall trigger | Paywall appears only at gated surface entry or explicit upgrade action | Surprise timed popups or repetitive nag loops | UI flow tests + manual review |
| PR-2 | Clear value framing | Pro copy is factual and non-coercive | Fear/urgency/shame-based copy | Content checklist |
| PR-3 | Restore flow reliability | Restore action is visible and deterministic | Hidden restore path or inconsistent state updates | Instrumentation tests with billing fakes |

## 6) Cross-platform parity expectations
| ID | Criterion | Pass condition | Fail condition | Measurement |
|---|---|---|---|---|
| XP-1 | Core feature parity baseline | Android and iOS both support core logging + explicit online action + export in MVP scope | Missing parity in committed baseline scope | Phase parity matrix |
| XP-2 | Behavior parity in shared rules | Shared normalization/mapping/business rules produce equivalent outputs across platforms | Divergent outputs for same fixtures | Shared-core parity tests |
| XP-3 | Constraint parity | Offline-first and explicit-network constraints hold on both platforms | Platform-specific hidden networking behavior | Cross-platform network trace checklist |

## Release-floor checks (all phases 26-31)
- `./gradlew test` passes.
- `./gradlew assembleDebug` passes.
- `./gradlew :app:connectedDebugAndroidTest` passes.
- No contradiction with privacy/offline/explicit-action constraints.

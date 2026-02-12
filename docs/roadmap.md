# OpenFuel Roadmap (Rolling)

OpenFuel is Android-first and experience-first, with one promise:

- Log what you ate in under 10 seconds, and know what it means.

Historical plans remain in `docs/archive/2026-02-11/`.

## Planning guardrails
- Offline-first remains the default.
- Online lookup remains explicit-action only.
- No dark patterns.
- Deterministic testing is mandatory.
- No schema changes unless a phase explicitly opens schema work.

## Rolling roadmap cadence
- Next 10 phases are locked and detailed.
- Following 10 phases are outlined and adjustable.
- At review checkpoints (40, 50, 60...), outlined phases are promoted and refreshed.

## Phase status snapshot (32-40)
| Phase | Status | Theme |
|---|---|---|
| 32 | Shipped | Docs reset and roadmap v2 |
| 33 | Shipped | Today 10x |
| 34 | Shipped | Goal profiles v1 |
| 35 | Shipped | Review A |
| 36 | Shipped | Foods 10x as one system |
| 37 | Shipped | Weekly Review v1 |
| 38 | Shipped | Habit loops v1 + Today CTA consolidation |
| 39 | Shipped | Trust and data quality v1 |
| 40 | Current review | Review B (roadmap lock + outline refresh) |

## LOCKED phases 41-50 (detailed)

### 41 — Performance and reliability hardening
- Title: Performance and reliability hardening.
- Goal: Reduce latency and failure ambiguity in core logging flows.
- Scope in:
  - Today, Add Food, and Foods interaction latency hardening.
  - Offline save-path reliability checks.
- Scope out:
  - No new feature families.
- Acceptance criteria:
  - p95 interaction latency reduced for core capture paths.
  - No regression in save-path correctness.
- Risks:
  - Micro-optimisation churn without measurable value.
- Test focus:
  - Deterministic ViewModel regressions and instrumentation responsiveness assertions.

### 42 — Add Food coherence pass
- Title: Add Food coherence pass.
- Goal: Improve known-food capture speed with clearer local/online hierarchy.
- Scope in:
  - Add Food composition and hierarchy only.
  - Trust cue density reductions in expanded online state.
- Scope out:
  - No provider orchestration changes.
- Acceptance criteria:
  - Known-food path requires fewer actions in common repeat cases.
  - Explicit-action online behaviour unchanged.
- Risks:
  - Cue clutter regressing scan speed.
- Test focus:
  - Add Food smoke tests and online gating regressions.

### 43 — Goal profiles v2 clarity
- Title: Goal profiles v2 clarity.
- Goal: Make profile effects and reversibility immediately understandable.
- Scope in:
  - Profile effect summaries and settings explainers.
  - Clearer post-change confirmation language.
- Scope out:
  - No clinical or diagnostic claims.
- Acceptance criteria:
  - Users can explain what profile choice changes after one read.
- Risks:
  - Overlong copy reducing usability.
- Test focus:
  - Deterministic profile/settings instrumentation and unit tests.

### 44 — Trust and data quality v2
- Title: Trust and data quality v2.
- Goal: Unify confidence language and correction affordances across all core surfaces.
- Scope in:
  - Shared `Complete` vs `Needs review` language and placement consistency.
  - Harmonised review/fix affordances where low confidence exists.
- Scope out:
  - No schema changes.
- Acceptance criteria:
  - Trust cues are consistent across Today, Foods, Add Food, and Weekly Review.
- Risks:
  - Over-warning reducing clarity.
- Test focus:
  - Trust cue unit matrix and cross-surface smoke coverage.

### 45 — Review C
- Title: Review C.
- Goal: Run an evidence checkpoint and re-prioritise with delivery-risk controls.
- Scope in:
  - Product and engineering audit.
  - Backlog pruning and roadmap deltas.
- Scope out:
  - No feature implementation.
- Acceptance criteria:
  - Published ranked opportunities and top risk register.
- Risks:
  - Drift if deltas are not enforced in subsequent phases.
- Test focus:
  - Gate reliability trend review and deterministic health checks.

### 46 — Weekly Review v2
- Title: Weekly Review v2.
- Goal: Improve practical usefulness with clearer, confidence-aware weekly adjustment guidance.
- Scope in:
  - Summary clarity and coverage signalling.
  - Explainability improvements for suggested action.
- Scope out:
  - No AI coaching expansion.
- Acceptance criteria:
  - Weekly review remains calm and quick to complete.
- Risks:
  - Low perceived utility if suggestions stay generic.
- Test focus:
  - Weekly review deterministic state and UI-path tests.

### 47 — Accessibility hardening
- Title: Accessibility hardening.
- Goal: Improve accessibility confidence across core flows.
- Scope in:
  - TalkBack semantics and focus order checks.
  - Large-text resilience on core screens.
- Scope out:
  - No broad redesign.
- Acceptance criteria:
  - Core flows pass defined accessibility checks.
- Risks:
  - Visual regressions from spacing/tap target adjustments.
- Test focus:
  - Semantics-based instrumentation assertions.

### 48 — Distribution readiness v1
- Title: Distribution readiness v1.
- Goal: Improve release readiness and distribution hygiene.
- Scope in:
  - Release process docs and quality checklists.
  - Store asset/copy alignment with actual product behaviour.
- Scope out:
  - No monetisation work.
- Acceptance criteria:
  - Release checklist is complete and reproducible.
- Risks:
  - Premature launch pressure.
- Test focus:
  - Build and release pipeline rehearsal.

### 49 — Release readiness
- Title: Release readiness.
- Goal: Final pre-launch reliability and support readiness.
- Scope in:
  - Stability and support playbook hardening.
  - Final blocker triage.
- Scope out:
  - No new major capabilities.
- Acceptance criteria:
  - Launch blocker list reaches zero.
  - Gate pass repeatability is demonstrated.
- Risks:
  - Scope creep under deadline pressure.
- Test focus:
  - Full gate reproducibility and stability checks.

### 50 — Review D
- Title: Review D and launch decision.
- Goal: Decide launch readiness from evidence and refresh the rolling roadmap.
- Scope in:
  - Outcome audit and post-50 roadmap update.
- Scope out:
  - No implementation work.
- Acceptance criteria:
  - Published launch decision memo with supporting evidence.
- Risks:
  - Decision bias without complete evidence.
- Test focus:
  - Quality trend and invariant verification.

## OUTLINED phases 51-60 (high-level, not locked)
- 51 — Data quality automation guardrails
  - Intent: reduce manual correction burden while keeping confidence explicit.
  - Major risks: false-confidence classifications.
- 52 — History usefulness pass
  - Intent: improve retrospective insight quality without adding clutter.
  - Major risks: UI density and navigation friction.
- 53 — Capture ergonomics v2
  - Intent: reduce repetitive logging friction in one-thumb usage.
  - Major risks: control overload and accidental taps.
- 54 — Insights comprehension v2
  - Intent: improve interpretation speed and trust context.
  - Major risks: over-interpretation of sparse/incomplete data.
- 55 — Wearables integration (future candidate)
  - Intent: evaluate opt-in wearable import overlays with strict privacy posture.
  - Major risks: permission complexity and background-sync pressure.
  - Note: out of scope for phases 41-50 unless explicitly promoted by a later review.
  - Constraints: opt-in only, privacy-first, explicit permissioning, local-first, no background sync unless a future phase explicitly opens it.
- 56 — Internationalisation readiness
  - Intent: prepare copy/layout for broader locale support.
  - Major risks: regression in tags/semantics and copy consistency.
- 57 — Reliability scaling pass
  - Intent: improve long-session and edge-case stability.
  - Major risks: hard-to-reproduce failures.
- 58 — Personalisation safety rails
  - Intent: tune defaults safely without manipulative mechanics.
  - Major risks: accidental behaviour drift.
- 59 — Distribution readiness v2
  - Intent: harden support and operational playbooks.
  - Major risks: process overhead and delayed delivery.
- 60 — Review E
  - Intent: promote next rolling lock window and refresh outlined horizon.
  - Major risks: planning debt.

## Delta since Phase 35
1. Phases 36-39 are now marked shipped with factual outcomes.
2. Phase 40 introduces rolling roadmap structure (locked 41-50, outlined 51-60).
3. Trust and data quality depth is now explicit through locked Phase 44.
4. Add Food coherence work is narrowed to composition/trust clarity, not provider orchestration.
5. Accessibility hardening is elevated into locked Phase 47.
6. Distribution readiness is split into phased preparation (48) and final readiness (49).
7. Review cadence remains anchored at 45 and 50.
8. Wearables are explicitly deferred to outlined horizon and constrained by privacy/permission guardrails.
9. Locked 41-50 scope continues to exclude monetisation work.
10. Invariants remain explicit: offline-first, explicit-action networking, deterministic gates.

Business model and monetisation remain out of scope for this roadmap slice.

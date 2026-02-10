# Phase 25 Deterministic Test Strategy (26-31)

## Test strategy goals
- Keep release quality predictable under strict local-first constraints.
- Detect regressions early in parsing, mapping, orchestration, and core UX loops.
- Maintain non-flaky CI that enforces the same gates locally and in PR checks.

## Deterministic test architecture
### 1) Unit tests (JVM-first)
Focus areas:
- query/serving normalization and parsing logic,
- nutrient calculations and unit conversions,
- provider merge/dedupe ranking behavior,
- ViewModel state machines for online/offline actions,
- paywall/entitlement state transitions via fakes.

Rules:
- no network,
- fixed inputs and expected outputs,
- no system clock dependence without injected test clock.

### 2) Provider contract tests (fixture-based)
Coverage:
- OFF, USDA, Nutritionix provider fixtures.
- Mapping invariants: identity stability, non-negative nutrients, serving consistency, deterministic output.

Rules:
- payloads loaded from local fixtures only,
- failures must include fixture name and failing invariant,
- fixture additions require corresponding expected-output assertions.

### 3) Critical-flow UI tests (deterministic instrumentation)
Priority flows:
- Add Food local search and logging,
- explicit Search online / Refresh online behavior,
- barcode explicit lookup state transitions,
- paywall lock/restore deterministic paths,
- quick add text/voice flow with test fakes.

Rules:
- no real network providers,
- no real microphone/system speech UI,
- deterministic test runner overrides required.

### 4) Offline-first tests
Must include airplane-mode scenarios:
- core local flows remain usable,
- online actions fail fast with clear user-safe messages,
- no crash/no spinner dead-ends.

Rules:
- assert final states (not transient timing assumptions),
- verify no hidden online side effects.

## Anti-flake policy (hard rules)
- No real network calls in tests.
- No live provider dependencies.
- No fixed sleeps (`Thread.sleep`) in UI tests.
- Use idling/resource synchronization and stable state assertions.
- No real mic/speech/camera/system UI dependence where fakes are available.
- Control all nondeterminism:
  - injectable clock,
  - fixed seeds for random data,
  - deterministic fixture inputs.
- Emulator pinning:
  - use a single known AVD for connected tests,
  - avoid concurrent multi-emulator execution for same AVD profile.

## CI enforcement
Required gates in strict order:
1. `./gradlew test`
2. `./gradlew assembleDebug`
3. `./gradlew :app:connectedDebugAndroidTest`

Policy:
- fail fast on first failing gate,
- triage and fix minimal root cause,
- rerun failing gate, then rerun full sequence before merge.

## Failure triage protocol
1. Classify failure as deterministic regression vs environment instability.
2. If environment issue (e.g., missing/duplicate emulator), stabilize environment first.
3. If code regression, add/adjust smallest deterministic test that reproduces issue.
4. Fix minimally and re-run gate sequence.
5. Record recurring flakes in a dedicated anti-flake backlog.

## Phase-by-phase regression matrix (26-31)
| Phase | New test focus | Required regression suite |
|---|---|---|
| 26 | Reliability/copy state coherence; accessibility regressions | Full JVM suite + Add Food/Settings instrumentation smoke + offline checks |
| 27 | Provenance/completeness messaging and serving consistency | Provider contract tests + golden corpora + Add Food regression smoke |
| 28 | Shared-core extraction parity | Shared JVM parity suite + Android behavior parity checks |
| 29 | iOS MVP parity on core flows | Shared-core parity tests + iOS core flow smoke + Android full regression |
| 30 | Wear OS utility deterministic behavior | Queue/retry deterministic integration tests + no-background-network assertions |
| 31 | Cross-platform stabilization | Full regression sweep + anti-flake audit + manual QA checklist completion |

## Minimum release evidence package
For each phase merge:
- gate outputs recorded,
- changed-file list,
- explicit note of new test tags (if any),
- manual QA checklist run result,
- statement confirming offline-first and explicit-network constraints remain intact.

# OpenFuel
Privacy-first, local-first nutrition tracker.
Status: bootstrapping.

## Changelog
- 2026-02-08: Completed Phase 8 provider execution architecture (multi-provider executor, deterministic stub provider path for tests, Room cache with TTL, local-only diagnostics).
- 2026-02-08: Completed Phase 9 provider reliability and UX polish (structured provider error statuses, cache versioning/invalidation, explicit refresh control, provenance surfacing, save/log hardening, deterministic refresh instrumentation coverage).

## Deterministic Android Tests
Run from `/Users/robertross/Projects/OpenFuel/android`:

```bash
./gradlew test
./gradlew assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

`connectedDebugAndroidTest` uses `OpenFuelAndroidTestRunner`, which overrides the app container with deterministic providers only (`forceDeterministicProvidersOnly = true`). This keeps instrumentation tests offline and reproducible by disabling live Open Food Facts network execution in androidTest runs.

## Phase 9 Manual QA Checklist
1. In Settings, keep Online food lookup enabled and open Add Food.
2. Enter `oat`, tap `Search online`, and confirm deterministic online results render with provenance label.
3. Tap `Refresh online` and confirm results refresh without hidden background behavior.
4. Turn Online food lookup off in Settings, return to Add Food, tap `Search online`, and confirm no spinner appears and a clear disabled message is shown.
5. Open an online result preview with partial/unknown nutrition and verify Save + Save and log both succeed with friendly messaging.
6. In debug builds, verify provider diagnostics show execution count and cache-hit information locally only.

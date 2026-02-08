# OpenFuel
Privacy-first, local-first nutrition tracker.
Status: bootstrapping.

## Changelog
- 2026-02-08: Completed Phase 8 provider execution architecture (multi-provider executor, deterministic stub provider path for tests, Room cache with TTL, local-only diagnostics).
- 2026-02-08: Completed Phase 9 provider reliability and UX polish (structured provider error statuses, cache versioning/invalidation, explicit refresh control, provenance surfacing, save/log hardening, deterministic refresh instrumentation coverage).
- 2026-02-08: Completed Phase 10 intelligence seam and Quick add text helper (local-only rule-based parsing, explicit query prefill, deterministic offline tests).
- 2026-02-08: Completed Phase 11 voice quick add (explicit-action `RecognizerIntent` seam, local-only flow, deterministic fake transcriber in androidTest).

## Deterministic Android Tests
Run from `/Users/robertross/Projects/OpenFuel/android`:

```bash
./gradlew test
./gradlew assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

`connectedDebugAndroidTest` uses `OpenFuelAndroidTestRunner`, which overrides the app container with deterministic providers and fake voice transcription. This keeps instrumentation tests offline and reproducible by disabling live Open Food Facts execution and avoiding system microphone/speech UI dependencies.

## Quick add (text) helper
- Available from Add Food as `Quick add (text)`.
- Input examples: `2 eggs and banana`, `200g chicken`, `1.5 cups milk`.
- The helper only parses text and prefills the existing unified search query when you tap a preview item.
- It does not auto-log, auto-save, or auto-run online search.
- Voice input in the quick-add dialog is explicit action only (`Voice` button), and recognized text must still be reviewed and selected from preview.
- Some devices may require offline speech language packs for offline recognition support.

## Phase 9 Manual QA Checklist
1. In Settings, keep Online food lookup enabled and open Add Food.
2. Enter `oat`, tap `Search online`, and confirm deterministic online results render with provenance label.
3. Tap `Refresh online` and confirm results refresh without hidden background behavior.
4. Turn Online food lookup off in Settings, return to Add Food, tap `Search online`, and confirm no spinner appears and a clear disabled message is shown.
5. Open an online result preview with partial/unknown nutrition and verify Save + Save and log both succeed with friendly messaging.
6. In debug builds, verify provider diagnostics show execution count and cache-hit information locally only.

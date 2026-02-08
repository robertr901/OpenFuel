# OpenFuel
Privacy-first, local-first nutrition tracker.
Status: bootstrapping.

## Current Delivery Status
- 2026-02-08: Completed Phase 8 provider execution architecture (multi-provider executor, deterministic stub provider path for tests, Room cache with TTL, local-only diagnostics).
- 2026-02-08: Completed Phase 9 provider reliability and UX polish (structured provider error statuses, cache versioning/invalidation, explicit refresh control, provenance surfacing, save/log hardening, deterministic refresh instrumentation coverage).
- 2026-02-08: Completed Phase 10 intelligence seam and quick-add helper (local-only rule-based parsing, explicit query prefill, deterministic offline tests).
- 2026-02-08: Completed Phase 11 voice quick add (explicit-action `RecognizerIntent` seam, local-only flow, deterministic fake transcriber in androidTest).
- 2026-02-08: Completed Phase 12a Add Food UX/a11y polish, Phase 12b intelligence seam hardening, and Phase 12c unified quick-add + voice EUX refinement.
- 2026-02-08: Completed Phase 13 security/docs release hardening (security evidence docs, deterministic verification guidance, documentation consistency checks).
- 2026-02-08: Completed Phase 14 monetisation foundations (release Play Billing entitlements, calm paywall UX with restore, Pro advanced export JSON/CSV with optional redaction).

## Deterministic Android Verification
Run from repo root:

```bash
cd android
./gradlew test
./gradlew assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

`./gradlew :app:connectedDebugAndroidTest` is part of the primary release gate. It uses `OpenFuelAndroidTestRunner`, which overrides the app container with deterministic providers and fake voice transcription. This keeps instrumentation tests offline and reproducible by disabling live Open Food Facts execution and avoiding system microphone/speech UI dependencies.

## Pro entitlements and advanced export
- Pro unlock is backed by Play Billing in release builds through the entitlement service seam.
- Gated surfaces (Insights and Advanced Export) present a deterministic paywall with visible purchase and restore actions.
- Advanced export is explicit user action only and supports:
  - JSON export
  - CSV export
  - Optional redaction mode (for example, brand redaction before sharing)
- No telemetry, no background polling, and no automatic uploads.

## Quick add helper
- Available from Add Food as `Quick add`.
- Input examples: `2 eggs and banana`, `200g chicken`, `1.5 cups milk`.
- The helper only parses text and prefills the existing unified search query when you tap a preview item.
- It does not auto-log, auto-save, or auto-run online search.
- Voice input in the quick-add dialog is explicit action only (mic action in the text field), and recognized text must still be reviewed and selected from preview.
- Some devices may require offline speech language packs for offline recognition support.

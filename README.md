# OpenFuel
Privacy-first, local-first nutrition tracker.
Status: active development with completed delivery through Phase 21.

## Vision and principles
- Product vision and non-goals: `docs/product-vision.md`
- Architecture boundaries and change guide: `docs/architecture.md`
- Delivery roadmap (acceptance-first): `docs/roadmap.md`

## Product direction (Phase 25)
- Canonical direction reset: `docs/phase-25/phase-25-plan.md`
- Product positioning and non-goals: `docs/product-vision.md`
- Phase 25 locks the next delivery sequence to phases 26-31 with offline-first, explicit-action networking, deterministic testing, and no telemetry/ads/trackers/accounts.

## Current Delivery Status
### Completed phases (historical)
- 2026-02-08: Completed Phase 8 provider execution architecture (multi-provider executor, deterministic stub provider path for tests, Room cache with TTL, local-only diagnostics).
- 2026-02-08: Completed Phase 9 provider reliability and UX polish (structured provider error statuses, cache versioning/invalidation, explicit refresh control, provenance surfacing, save/log hardening, deterministic refresh instrumentation coverage).
- 2026-02-08: Completed Phase 10 intelligence seam and quick-add helper (local-only rule-based parsing, explicit query prefill, deterministic offline tests).
- 2026-02-08: Completed Phase 11 voice quick add (explicit-action `RecognizerIntent` seam, local-only flow, deterministic fake transcriber in androidTest).
- 2026-02-08: Completed Phase 12a Add Food UX/a11y polish, Phase 12b intelligence seam hardening, and Phase 12c unified quick-add + voice EUX refinement.
- 2026-02-08: Completed Phase 13 security/docs release hardening (security evidence docs, deterministic verification guidance, documentation consistency checks).
- 2026-02-08: Completed Phase 14 monetisation foundations (release Play Billing entitlements, calm paywall UX with restore, Pro advanced export JSON/CSV with optional redaction).
- 2026-02-09: Completed Phase 16 unified search + first real USDA provider integration (explicit-action online only, deterministic offline-friendly tests).
- 2026-02-09: Completed Phase 17 multi-provider online search orchestration (deterministic provider runs, per-provider status disclosure, isolated provider failures, explicit-action online only).
- 2026-02-09: Completed Phase 18 provider integration hardening (Nutritionix real integration, deterministic multi-provider merge improvements, explicit-action online safeguards unchanged).
- 2026-02-09: Completed Phase 19 online provider reliability hardening (Open Food Facts timeout/retry resilience, clearer provider setup/status UX, deterministic merge refinements, explicit-action networking unchanged).
- 2026-02-09: Completed Phase 20 release review and quality hardening (high-leverage UX clarity fix, CI gate enforcement for test/assemble/connected tests, docs/runtime alignment, evidence-based next roadmap proposal).
- 2026-02-10: Completed Phase 21 provider contract conformance pack (versioned OFF/USDA/Nutritionix fixture corpus + deterministic JVM contract harness to lock provider mapping behavior).

### Next phases (26-31 direction)
- Phase 26: reliability and UX clarity baseline.
- Phase 27: data trust layer v1 (provenance and serving/unit trust).
- Phase 28: shared core for cross-platform parity (KMP recommendation).
- Phase 29: iOS MVP with offline-first parity and explicit online actions.
- Phase 30: Wear OS utility slice with explicit-action only networking.
- Phase 31: cross-platform stabilization and release readiness.
- Reference roadmap summaries: `docs/roadmap.md`
- Canonical scope, acceptance, and constraints: `docs/phase-25/phase-25-plan.md`

## Deterministic Android Verification
Run from repo root:

```bash
cd android
./gradlew test
./gradlew assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

If no emulator/device is attached for connected tests, start the reference AVD first:

```bash
emulator -avd Medium_Phone_API_35 -no-window -no-audio
```

`./gradlew :app:connectedDebugAndroidTest` is part of the primary release gate. It uses `OpenFuelAndroidTestRunner`, which overrides the app container with deterministic providers and fake voice transcription. This keeps instrumentation tests offline and reproducible by disabling live Open Food Facts execution and avoiding system microphone/speech UI dependencies.

PR CI runs the same three gates via `.github/workflows/android-gates.yml`.
Verification source of truth: `docs/verification.md`.

## Online provider setup (local development)
OpenFuel includes multi-provider online search behind explicit-action controls.

1. Open `android/local.properties`.
2. Configure providers and credentials:

```properties
ONLINE_PROVIDER_OPEN_FOOD_FACTS_ENABLED=true
ONLINE_PROVIDER_USDA_ENABLED=true
ONLINE_PROVIDER_NUTRITIONIX_ENABLED=true
USDA_API_KEY=your_usda_key_here
NUTRITIONIX_APP_ID=your_nutritionix_app_id
NUTRITIONIX_API_KEY=your_nutritionix_api_key
```

3. Rebuild:

```bash
cd android
./gradlew assembleDebug
```

If `USDA_API_KEY` or Nutritionix credentials are missing, the app stays stable and shows a clear provider `needs setup` message in `Online sources`. No keys are committed to the repository.

Settings now includes an `Online provider setup` section that shows local status for each provider (`Configured`, `Needs setup`, `Disabled`) without revealing secret values.

## Online privacy boundary (explicit action only)
- Data leaves the device only when the user explicitly taps online actions:
  - `Search online`
  - `Refresh online`
  - barcode scan lookup action
- Zero online requests occur without explicit user action.
- Add Food shows an `Online sources` summary for each explicit run so users can see which providers succeeded, failed, were disabled, or need setup.
- There are no background provider refresh jobs, no telemetry, and no analytics SDKs.

## Open Food Facts timeout troubleshooting
- Ensure the request was explicitly triggered with `Search online` or `Refresh online`.
- Check device connectivity (airplane mode / captive portal / DNS reachability).
- If Open Food Facts is slow, use `Refresh online` to retry explicitly.
- Use `Online sources` and provider diagnostics to verify whether OFF returned `Timed out (check connection).`, `No connection.`, or `Service error.`.

## Pro entitlements and advanced export
- Pro unlock is backed by Play Billing in release builds through the entitlement service seam.
- Gated surfaces (Insights and Advanced Export) present a deterministic paywall with visible purchase and restore actions.
- Paywall entry is limited to gated surface access or explicit upgrade action.
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

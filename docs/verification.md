# Verification

## Release gate order (run from `android/`)
1. `./gradlew test`
2. `./gradlew assembleDebug`
3. `./gradlew :app:connectedDebugAndroidTest`

If connected tests need an emulator:
- `emulator -avd Medium_Phone_API_35 -no-window -no-audio`

## Deterministic testing rules
- No live network dependency in tests.
- No fixed sleeps.
- Prefer stable test tags and deterministic fakes.
- Keep explicit-action networking assertions in regression coverage.

## What must be true before merge
- Unit tests pass.
- Debug build assembles.
- Instrumentation suite passes on the pinned emulator profile.
- Offline-first flows remain usable.
- No online provider execution without explicit user action.

## Provider verification checks
- Disabled provider states are clear and non-crashing.
- Missing provider setup paths are explicit and user-safe.
- Offline mode keeps local logging usable.

## Documentation checks (Phase 32 docs slices)
- Internal links resolve.
- Active docs are listed in `docs/README.md`.
- Superseded plans are archived and labelled historical.
- No runtime claims are added without code evidence.

## Reference documents
- Product vision: `docs/product-vision.md`
- Roadmap: `docs/roadmap.md`
- Architecture: `docs/architecture.md`
- Threat model: `docs/threat-model.md`
- Security policy: `SECURITY.md`

# Verification

## Quick local check (run from `android`)
1) Build debug APK:
   - `./gradlew assembleDebug`
2) Run unit tests:
   - `./gradlew test`

## Full gate (required before PR merge)
3) Run deterministic instrumentation tests:
   - `./gradlew :app:connectedDebugAndroidTest`

## Expected results
- All commands complete with no errors.
- Unit tests pass.
- Intelligence golden corpus parser tests pass as part of `./gradlew test`.
- Instrumentation tests pass on the pinned emulator profile.

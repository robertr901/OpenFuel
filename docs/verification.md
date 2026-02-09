# Verification

## Full release gate (run from `android`)
1) Run unit tests:
   - `./gradlew test`
2) Build debug APK:
   - `./gradlew assembleDebug`
3) Run deterministic instrumentation tests:
   - `./gradlew :app:connectedDebugAndroidTest`

## Expected results
- All commands complete with no errors.
- Unit tests pass.
- Debug APK assembles successfully.
- Intelligence golden corpus parser tests pass as part of `./gradlew test`.
- Instrumentation tests pass on the pinned emulator profile.
- Deterministic androidTest coverage still validates:
  - Add Food unified search and quick-add flows without live network dependencies.
  - Pro-gated paywall lock and restore flows using fake/debug entitlement wiring in tests.

## Provider verification checklist (manual, release-oriented)
- With `android/local.properties` missing `USDA_API_KEY`:
  - Add Food online search remains stable.
  - App shows a clear configuration message (no crash, no stack trace).
- With valid `USDA_API_KEY` configured:
  - Tapping `Search online` from Add Food can return USDA-backed results.
  - Tapping `Refresh online` performs a second explicit online execution (no silent background refresh).
- Airplane mode:
  - Explicit online action returns friendly failure copy.
  - UI remains responsive and local search still works.
- Confirm no online request is started unless the user taps an explicit online action.

## UI consistency checklist
- Use design tokens from `ui/design/OpenFuelDesignTokens.kt` and `ui/theme/Dimens.kt`.
- Prefer shared components from `ui/components/OpenFuelComponents.kt` for cards, rows, buttons, and pills.
- Keep a single primary action per surface; demote diagnostics/advanced controls with expanders when possible.
- Preserve existing test tags and accessibility semantics when refactoring UI structure.

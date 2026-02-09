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

## UI consistency checklist
- Use design tokens from `ui/design/OpenFuelDesignTokens.kt` and `ui/theme/Dimens.kt`.
- Prefer shared components from `ui/components/OpenFuelComponents.kt` for cards, rows, buttons, and pills.
- Keep a single primary action per surface; demote diagnostics/advanced controls with expanders when possible.
- Preserve existing test tags and accessibility semantics when refactoring UI structure.

# Verification

## Full release gate (run from `android`)
1) Run unit tests:
   - `./gradlew test`
2) Build debug APK:
   - `./gradlew assembleDebug`
3) Run deterministic instrumentation tests:
   - `./gradlew :app:connectedDebugAndroidTest`

## CI gate (pull requests)
- GitHub Actions workflow: `.github/workflows/android-gates.yml`
- PRs to `main` run the same three gates in order:
  1) `./gradlew test`
  2) `./gradlew assembleDebug`
  3) `./gradlew :app:connectedDebugAndroidTest`

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
- With provider defaults enabled and missing `USDA_API_KEY` and/or Nutritionix credentials:
  - Tapping `Search online` remains stable.
  - `Online sources` shows missing-config providers as `needs setup` with clear messaging.
  - Settings `Online provider setup` shows which provider is `Configured`, `Needs setup`, or `Disabled`.
- With valid `USDA_API_KEY`, `NUTRITIONIX_APP_ID`, and `NUTRITIONIX_API_KEY` configured:
  - Tapping `Search online` can return USDA-backed candidates.
  - Tapping `Search online` can return Nutritionix-backed candidates.
  - Tapping `Refresh online` performs a second explicit online execution (no silent background refresh).
- With `ONLINE_PROVIDER_OPEN_FOOD_FACTS_ENABLED=false`, `ONLINE_PROVIDER_USDA_ENABLED=false`, or `ONLINE_PROVIDER_NUTRITIONIX_ENABLED=false`:
  - `Online sources` shows the disabled provider as `disabled`.
  - Other enabled providers still run and render results/failures independently.
- Airplane mode:
  - Explicit online action returns friendly failure copy per provider status.
  - UI remains responsive and local search still works.
- Confirm no online request is started unless the user taps an explicit online action.

## Open Food Facts timeout troubleshooting (manual)
- Trigger online search only via explicit button tap (`Search online` or `Refresh online`).
- Validate `Online sources` status for OFF:
  - `Timed out (check connection).`
  - `No connection.`
  - `Service error.`
- Confirm retry behavior is explicit-action only:
  - tap `Refresh online` to retry
  - no automatic background retries/polling should occur.

## UI consistency checklist
- Use design tokens from `ui/design/OpenFuelDesignTokens.kt` and `ui/theme/Dimens.kt`.
- Prefer shared components from `ui/components/OpenFuelComponents.kt` for cards, rows, buttons, and pills.
- Keep a single primary action per surface; demote diagnostics/advanced controls with expanders when possible.
- Preserve existing test tags and accessibility semantics when refactoring UI structure.

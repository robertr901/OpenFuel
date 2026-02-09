# Changelog

All notable changes to OpenFuel are documented here.

## Unreleased

### Phase 19: Online provider reliability
- Hardened Open Food Facts runtime reliability for explicit online actions:
  - explicit connect/read/write/call timeout configuration on shared online HTTP client
  - bounded provider execution timeout policy tuned for real-device reliability
  - single explicit retry path for idempotent Open Food Facts GET calls (no background retry loops).
- Refined provider error messaging surfaced to users:
  - timeout: `Timed out (check connection).`
  - offline/network unavailable: `No connection.`
  - HTTP/parsing failures: `Service error.`
- Added Settings `Online provider setup` surface with non-secret provider status (`Configured`, `Needs setup`, `Disabled`).
- Reduced online status message duplication in Add Food:
  - one top-level summary/error message
  - per-provider details remain under `Online sources`.
- Improved deterministic multi-provider merge behavior:
  - stronger identity keying with serving-size-aware text matching
  - stable, deterministic winner selection with richer payload preference
  - conservative dedupe to avoid hiding distinct items.
- Added deterministic unit/androidTest coverage for:
  - Open Food Facts retry behavior
  - provider setup visibility in Settings
  - Add Food online summary messaging
  - multi-provider merge/dedupe edge cases.

### Phase 18: Provider integrations hardening
- Added a real Nutritionix provider integration behind existing provider seams:
  - `NutritionixRemoteFoodDataSource` with explicit guard-token enforcement
  - `NutritionixCatalogProvider` for text search and barcode lookup delegation
  - deterministic mapping tests for defensive parsing and nutrient normalization.
- Added local configuration support for Nutritionix:
  - `ONLINE_PROVIDER_NUTRITIONIX_ENABLED`
  - `NUTRITIONIX_APP_ID`
  - `NUTRITIONIX_API_KEY`
- Wired Nutritionix availability into provider registry selection with explicit `needs setup` messaging when credentials are missing.
- Hardened deterministic online merge behavior:
  - dedupe key precedence: barcode, then exact name(+brand), then fuzzy fallback
  - richer nutrition payload wins duplicate collisions
  - deterministic tie-breakers remain stable.
- Added Nutritionix provenance labeling in Add Food online result cards.
- No changes to provider cache schema/migrations, online guard token enforcement semantics, telemetry/analytics posture, or background networking behavior.

### Phase 17: Multi-provider online search orchestration
- Added deterministic online search orchestration for Add Food explicit online actions:
  - provider runs execute in stable priority order
  - per-provider run status/result metadata are captured
  - provider failures are isolated (one failing provider does not blank successful provider results).
- Added provider availability policy wiring for local development configuration:
  - `ONLINE_PROVIDER_OPEN_FOOD_FACTS_ENABLED`
  - `ONLINE_PROVIDER_USDA_ENABLED`
  - `USDA_API_KEY` (required for USDA execution)
- Added Add Food `Online sources` disclosure row to surface provider outcomes (`ok`, `empty`, `failed`, `disabled`, `needs setup`) with user-safe messaging.
- Added deterministic unit and instrumentation coverage for orchestration and explicit-action online flows.
- No changes to persistence schema/migrations, local search behavior, background networking, telemetry/analytics, or online guard token enforcement.

### Phase 16: Unified search + USDA provider integration
- Introduced shared unified search sectioning model for Local and Online result blocks in Add Food (single query field, single list).
- Added a real USDA FoodData Central provider implementation behind the existing provider seam:
  - Retrofit-based USDA client wrapper
  - deterministic response mapping into canonical `RemoteFoodCandidate`
  - barcode lookup path via explicit action only.
- Added local API key configuration support via `android/local.properties` (`USDA_API_KEY`) and `BuildConfig.USDA_API_KEY`.
- Added graceful missing-key handling with clear user-visible messaging and no crashes.
- Added deterministic unit coverage for USDA mapping/provider delegation and online failure/configuration message flows.
- Added deterministic instrumentation coverage to verify no online provider execution occurs before explicit user action.
- No changes to provider cache schema/migrations, online guard token enforcement semantics, telemetry posture, or background networking behavior.

### Phase 14: Entitlements/paywall/advanced export foundations
- Replaced release entitlement placeholder with Play Billing-backed entitlement wiring behind the existing `EntitlementService` seam.
- Added deterministic paywall UX on Pro-gated surfaces with explicit `Upgrade`, `Restore purchases`, and dismiss actions.
- Added entitlement refresh on app foreground entry plus explicit restore action (no background polling).
- Added Pro advanced export with:
  - JSON export (existing versioned payload path)
  - CSV export
  - optional redaction mode for privacy-conscious sharing.
- Added deterministic instrumentation coverage for paywall lock and restore flows using fakes (no real billing UI/network in tests).
- No provider executor changes, no provider registry changes, no cache schema/migration changes, and no online guard token behavior changes.

### Phase 13: Security/docs/release hardening
- Refreshed core documentation to remove local absolute paths and standardize repo-relative verification instructions.
- Elevated deterministic instrumentation (`:app:connectedDebugAndroidTest`) as a first-class release gate in docs.
- Added explicit release hygiene and security/privacy evidence documentation artifacts.
- No provider execution behavior changes, no cache schema changes, no online guard behavior changes, and no telemetry additions.

### Phase 12c: Unified quick add + voice EUX refinement
- Replaced split quick-add controls with a single `Quick add` entry in Add Food.
- Kept text parsing primary and moved manual macro logging behind a collapsed `Manual details` expander.
- Integrated voice capture as an explicit trailing mic action in quick-add input.
- Kept guardrails: no auto-search, no auto-save/log from parsed or voice text.

### Phase 12b: Intelligence seam hardening
- Added deterministic golden corpus tests for parser/normalizer behavior.
- Hardened rule-based parsing edge cases (comma decimals, uppercase units, leading noise tokens).
- Clarified deterministic, non-throwing contract expectations for intelligence parsing.

### Phase 12a: Add Food UX and accessibility polish
- Reworked Add Food hierarchy and section clarity with calmer composition.
- Kept online actions explicit and visually subordinate.
- Improved keyboard-first behavior and TalkBack semantics on quick-add and voice flows.

## Release Checklist

Run from `android/` before release or PR merge:

1. Build/Test gates:
   - `./gradlew test`
   - `./gradlew assembleDebug`
   - `./gradlew :app:connectedDebugAndroidTest`
2. Quick manual QA:
  - Verify Add Food local search, quick add, and barcode navigation flows.
  - Verify online lookup remains explicit action only (`Search online` / `Refresh online`).
  - Verify missing `USDA_API_KEY` shows a clear configuration message and does not crash.
  - Verify airplane mode returns friendly online failure copy while local search remains usable.
  - Verify online-disabled setting blocks network actions with clear user feedback.
  - Verify Pro-gated surfaces show paywall when locked and `Restore purchases` updates unlocked state.
   - Verify advanced export JSON/CSV plus optional redaction output before sharing.
3. Privacy/permissions checks:
   - Confirm no telemetry/analytics/crash-reporting SDKs were added.
   - Confirm no new dangerous permissions were added unintentionally.
   - Confirm voice capture remains explicit action and no background capture/service is introduced.
4. Documentation consistency:
   - Ensure README, architecture, roadmap, verification, and security docs match runtime behavior.
   - Ensure instructions are repo-relative and do not include local absolute filesystem paths.

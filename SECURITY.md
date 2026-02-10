# Security Policy

OpenFuel is a privacy-first, local-first nutrition tracker.

## Security and Privacy Baseline
- Local-first by default: personal meal logs stay on-device (Room + DataStore).
- Online lookups are explicit-action only and guarded.
- Billing actions are explicit-action only from user-visible paywall controls.
- No telemetry, no ads, no trackers, no crash-reporting SDKs.
- No secrets should be committed to this repository.

## Phase 25 alignment note
- Direction and constraints baseline: `docs/phase-25/phase-25-plan.md`.
- Threat and mitigation detail: `docs/threat-model.md`.
- Verification and deterministic gate order: `docs/verification.md`.

## Threat Model Summary
Threat model details are documented in `docs/threat-model.md`.

High-level controls:
- User-initiated guardrails on online requests (`UserInitiatedNetworkGuard`).
- Explicit-action capture for barcode and voice flows.
- Defensive handling of untrusted provider payloads and graceful UI failure states.
- No background network sync of personal logs.
- Deterministic release gates are enforced in CI (`.github/workflows/android-gates.yml`).

Provider payload drift controls:
- Deterministic provider contract fixtures and JVM contract tests lock OFF/USDA/Nutritionix mapping behavior.
- Canonical fixtures live under `android/app/src/test/resources/provider_fixtures/`.
- Contract harness lives under `android/app/src/test/java/com/openfuel/app/provider/contracts/`.

## Permissions Inventory (AndroidManifest)
Declared in `android/app/src/main/AndroidManifest.xml`:

1. `android.permission.INTERNET`
   - Used only for user-triggered online food lookups.
2. `android.permission.CAMERA`
   - Used only for explicit barcode scanning flow.

Not declared:
- `android.permission.RECORD_AUDIO`

## Network Behavior Summary
- Online provider requests are initiated from explicit UI actions:
  - Add Food online search/refresh (`AddFoodViewModel.searchOnline`, `AddFoodViewModel.refreshOnline`).
  - Barcode lookup (`ScanBarcodeViewModel.onBarcodeDetected`, `ScanBarcodeViewModel.retryLookup`).
- Add Food online search uses deterministic multi-provider orchestration through the provider executor seam.
- Provider runs execute in stable priority order and expose per-provider status in UI (`Online sources`) for explicit user transparency.
- Open Food Facts reliability hardening uses bounded client timeouts and at-most-one retry for idempotent GET requests, still only within explicit user-triggered online actions.
- Active real providers:
  - Open Food Facts (`ONLINE_PROVIDER_OPEN_FOOD_FACTS_ENABLED=true`)
  - USDA FoodData Central (`ONLINE_PROVIDER_USDA_ENABLED=true` and local `USDA_API_KEY` configured)
  - Nutritionix (`ONLINE_PROVIDER_NUTRITIONIX_ENABLED=true` and local `NUTRITIONIX_APP_ID` + `NUTRITIONIX_API_KEY` configured)
- `UserInitiatedNetworkGuard` issues and validates short-lived tokens before provider calls.
- Online lookup setting gates execution; disabled setting returns local-safe UI states and blocks provider calls.
- Online lookup default is currently `enabled = true` when no stored setting exists (`SettingsRepositoryImpl`), but requests still require explicit user action.
- Release-floor expectation: zero online requests without explicit user action.
- There is no background provider polling and no silent online refresh path.
- Missing USDA key degrades gracefully to user-visible `needs setup` guidance; no crashes and no secret fallback.
- Missing Nutritionix credentials degrade gracefully to user-visible `needs setup` guidance; no crashes and no secret fallback.
- Play Billing calls are limited to:
  - explicit user actions (`Upgrade` and `Restore purchases`) from paywall surfaces
  - explicit app foreground entitlement refresh (`MainActivity.onStart`)
- No periodic billing polling/background jobs are used.

## Billing and Paywall Behavior Summary
- Release entitlement implementation:
  - `PlayBillingEntitlementService`
  - `PlayBillingGateway`
- Billing is isolated behind the `EntitlementService` seam for testability and deterministic fake injection in androidTest.
- Locked Pro surfaces open a local paywall with explicit user controls; unlock state is reflected through entitlement state updates.
- Paywall presentation is limited to gated surface entry or explicit upgrade action, with a visible restore path.
- Offline/error states degrade gracefully with user-safe messages; no hidden retries or background network loops.

## Voice Behavior Summary
- Voice input uses the `VoiceTranscriber` seam and `RecognizerIntentVoiceTranscriber`.
- Voice capture is explicit action only from Quick add UI.
- No always-listening mode, no background capture service, no audio persistence.
- Recognizer requests prefer offline recognition (`RecognizerIntent.EXTRA_PREFER_OFFLINE = true`).
- Device/OEM recognizer behavior may vary; offline language packs may be required on some devices.
- Threat boundary reference: `docs/threat-model.md`.

## Data Storage Summary
- Room database: `android/app/src/main/java/com/openfuel/app/data/db/OpenFuelDatabase.kt`
  - Stores foods, meal entries, daily goals, and provider search cache metadata/payload.
- DataStore settings: `android/app/src/main/java/com/openfuel/app/data/datastore/SettingsDataStore.kt`
  - Stores app settings such as online lookup enablement, goals, and local entitlement state.
- Export is explicit user action via Settings UI; data is serialized to user-shared JSON/CSV output.
- Advanced export includes optional redaction controls to reduce sensitive-context sharing risk before user-initiated share.
- USDA API key is read from local build config (`android/local.properties` -> `BuildConfig.USDA_API_KEY`) and is not stored in user content databases.
- Nutritionix credentials are read from local build config (`android/local.properties` -> `BuildConfig.NUTRITIONIX_APP_ID` / `BuildConfig.NUTRITIONIX_API_KEY`) and are not stored in user content databases.
- Architecture boundary reference: `docs/architecture.md`.

## Logging Policy and Current Footprint
- Policy:
  - Do not log meal logs, queries, export contents, or other sensitive user content.
  - Keep diagnostics local-only and minimal.
- Current footprint:
  - Main app source avoids `android.util.Log`/Timber logging calls.
  - Provider diagnostics are local in-memory diagnostics state for debug surfaces.

## Responsible Disclosure
To report a security issue, contact: `security@openfuel.invalid` (placeholder).

When reporting:
- Include app version and reproduction steps.
- Do not include sensitive personal data in reports.
- Use coordinated disclosure and avoid public issue disclosure until a fix is available.

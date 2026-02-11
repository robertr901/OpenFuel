# Security Policy

OpenFuel is an Android app with an offline-first default and explicit-action networking.

## Security and privacy baseline
- Personal meal logs are local-first.
- Online calls are user-triggered and visible.
- No sale of user data.
- No ad IDs.
- No sensitive payload capture.
- No third-party tracking SDKs.
- No secrets committed to the repository.

## Canonical references
- Threat model: `docs/threat-model.md`
- Architecture boundaries: `docs/architecture.md`
- Verification gates: `docs/verification.md`
- Provider execution contract: `docs/provider_contract.md`

## Permissions inventory
Declared in `android/app/src/main/AndroidManifest.xml`:
1. `android.permission.INTERNET`
   - Used for explicit online lookup actions.
2. `android.permission.CAMERA`
   - Used for explicit barcode scanning.

Not declared:
- `android.permission.RECORD_AUDIO`

## Network behaviour summary
- Online lookups are explicit-action only and guarded.
- Online lookup runs only from explicit user actions (for example `Search online`, `Refresh online`, barcode lookup actions).
- Typing in search fields and passive screen states must not trigger provider execution.
- There are no background provider polling loops.
- Online behaviour remains settings-gated and user-visible.
- Online lookup default is currently `enabled = true`.

## Data storage summary
- Room stores local foods and meal history.
- DataStore stores settings and small preference state.
- Export is explicit user action.
- Local diagnostics must remain minimal and should not include sensitive payloads.

## Logging policy
- Do not log meal history, query text, barcode values, notes, or export contents.
- Keep diagnostics local and redacted.

## Reporting a security issue
Contact: `security@openfuel.invalid` (placeholder).

When reporting:
- Include app version and clear reproduction steps.
- Do not include personal sensitive data.

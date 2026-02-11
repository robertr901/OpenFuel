# OpenFuel Threat Model

## Scope
OpenFuel is an Android nutrition logger with an offline-first default.

This model covers:
- local storage (Room + DataStore),
- explicit export and share actions,
- optional explicit online lookup,
- barcode and quick-add capture surfaces.

Out of scope for this model iteration:
- cloud sync,
- account systems,
- third-party tracking services.

## Assets
- Meal history and nutrition logs.
- User goals and preferences.
- Exported user data files.
- Provider configuration values.

## Trust boundaries
- Device storage boundary.
- OS service boundary (camera, file picker, speech services where supported).
- Network boundary crossed only on explicit user action.
- Third-party food provider payload boundary (untrusted input).

## Data egress boundaries
- No ad IDs or tracking egress.
- No sensitive free-text payload capture.
- Online provider requests must be explicitly user-triggered.
- Export/share is explicit and user-controlled.
- No background provider calls from passive screens.

## Main threats and mitigations

### 1. Unintended online requests
Threat:
- Network calls occur without explicit user intent.

Mitigations:
- User-action guardrails in viewmodel and provider execution paths.
- Deterministic tests that assert typing and passive states do not invoke online provider calls.

### 2. Untrusted provider payloads
Threat:
- Malformed or inconsistent provider payloads degrade data quality or crash mapping.

Mitigations:
- Defensive mapping and bounded parsing.
- Deterministic fixture-based provider contract tests.

### 3. Sensitive data leakage via logging
Threat:
- Sensitive user content appears in debug or runtime logs.

Mitigations:
- Strict no-sensitive-logging policy.
- Local diagnostics only, with redaction requirements.

### 4. Export misuse
Threat:
- User shares sensitive data unintentionally.

Mitigations:
- Export remains explicit user action.
- Clear UX warnings and controllable export steps.

### 5. Device compromise
Threat:
- Data at rest exposed by malware or physical compromise.

Mitigations:
- App-private storage boundaries.
- Minimal persistence of non-essential sensitive state.

## Residual risks
- Device-level compromise cannot be fully mitigated by app code.
- Provider payload drift remains a recurring risk and requires continued fixture maintenance.

## Verification links
- `docs/verification.md`
- `SECURITY.md`
- `docs/provider_contract.md`

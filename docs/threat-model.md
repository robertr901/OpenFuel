# OpenFuel Threat Model (v0)

## Scope
OpenFuel is a local-first nutrition tracking app. By default, all logs, foods, goals, and calculations are stored on-device. Optional online lookups exist only when explicitly triggered by the user (e.g., searching a public food database or scanning a barcode).

This threat model covers:
- Local storage (Room + DataStore)
- Export (local file generation)
- Optional online lookups (explicit user action only)
- Quick add helpers (text/voice) as local-only input features

Out of scope (for now):
- Cloud sync
- Accounts/authentication
- Telemetry/analytics
- Payments/billing

## Assets to protect
- Meal logs (personal diet history)
- User-entered foods and favourites
- Goals and preferences
- Exported data files
- Device identifiers and metadata (avoid collecting)

## Trust boundaries
- Device storage (trusted, but vulnerable to physical compromise / malware)
- OS services (voice recognition, camera, file picker)
- Network boundary (only crossed on explicit user action)
- Third-party food providers (untrusted data source)

## Threats and mitigations

### 1) Unintended network calls (privacy leak)
**Threat:** App makes background or implicit network calls.
**Mitigations:**
- No network calls by default.
- All online lookups must be initiated by explicit user action (button tap).
- Guardrails enforce “user-initiated token” requirement in online execution path.
- Online lookup can be disabled in Settings and must block online actions cleanly.
- Deterministic instrumentation tests verify online gating behavior.

### 2) Data exfiltration via export
**Threat:** User exports sensitive data and shares it accidentally.
**Mitigations:**
- Export is always explicit user action.
- Export format is documented, inspectable JSON with schemaVersion/appVersion.
- UI should warn users that export contains their full local history (future hardening: optional redacted export).

### 3) Local data compromise
**Threat:** Other apps, malware, or physical attacker accesses local database/files.
**Mitigations:**
- Use app-private storage for DB/DataStore.
- Use Android OS sandbox.
- Avoid writing sensitive data to logs.
- Consider optional encrypted export (future).

### 4) Provider data integrity and poisoning
**Threat:** Online provider returns malformed or misleading nutrition data.
**Mitigations:**
- Treat provider data as untrusted.
- Defensive parsing (missing fields tolerated, nutrients bounded/sanitised).
- Provenance labels shown in UI.
- User must explicitly Save or Save+Log, no auto-log from online results.

### 5) Voice privacy and persistence
**Threat:** Voice capture records audio, stores it, or sends it off-device.
**Mitigations:**
- Voice is explicit action only.
- No audio persistence, only transcribed text is used.
- Prefer offline recognition where possible; handle “offline unavailable” gracefully.
- No always-listening, no background capture.

### 6) Camera/barcode permissions abuse
**Threat:** Camera used outside explicit user intent.
**Mitigations:**
- Barcode scanning is an explicit screen/action.
- Clear permission rationale.
- No background camera use.

## Residual risks / open questions
- Offline speech recognition availability varies by device/language packs.
- INTERNET permission is broad when optional online features exist; no TLS pinning currently.
- Export sharing risk remains user-driven; consider adding redaction options and stronger warnings.

## Verification strategy
- Unit tests for domain logic and parsers.
- Deterministic instrumentation tests for:
  - Online gating and explicit-action enforcement
  - Unified search controls flow
  - Quick add text and voice helper flows (no mic/system UI dependency in tests)

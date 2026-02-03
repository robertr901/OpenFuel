# Threat Model

## Top threats and mitigations

### 1) Privacy leaks via logs or analytics
**Threat**: Meal logs or exports appear in Logcat or third-party analytics.
**Mitigations**
- No analytics/telemetry libraries.
- Avoid logging sensitive data.
- Keep export generation silent; no payload logging.
- Disable automatic cloud backups by default.

### 2) Accidental data exfiltration through network calls
**Threat**: App sends food data over the network without consent.
**Mitigations**
- No network layer in MVP.
- Online lookup is off by default and requires explicit opt-in.
- Any future network calls must be documented and user-enabled.

### 3) Export misuse or accidental sharing
**Threat**: User shares exported data unintentionally.
**Mitigations**
- Export uses system share sheet so the user controls recipients.
- Export file stored in app-private cache and shared via FileProvider.
- Clear privacy note in Settings.

### 4) Malicious or malformed input
**Threat**: Large or malformed values cause crashes or incorrect totals.
**Mitigations**
- Input validation in UI and domain logic.
- Safe defaults for unit conversions.
- Tests for edge cases (empty days, decimals).

### 5) Local data corruption
**Threat**: Database corruption or partial writes lose entries.
**Mitigations**
- Room transactions for write operations.
- Structured migrations from v1 onward.
- Local backups via export.

### 6) Supply chain risk
**Threat**: Dependency compromise introduces tracking or data leaks.
**Mitigations**
- Minimal dependencies.
- Prefer stable, well-known AndroidX libraries.
- Review new dependencies before adding.

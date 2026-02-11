# OpenFuel Engineering Rules (AGENTS.md)

OpenFuel is an Android nutrition logging app.

Primary product promise:
- Log what you ate in under 10 seconds, and know what it means.

## Priorities (in order)
Security and privacy > reliability and data integrity > performance > usability and accessibility > maintainability.

## Hard constraints
- Local-first by default.
- Explicit-action networking for online lookups.
- No ad IDs, no sale of user data, no sensitive payload capture.
- No secrets in repository.
- Avoid binary files in PRs unless explicitly requested.
- Never invent command output.

## Documentation-first discipline
Before implementation work:
1. Read `docs/README.md`.
2. Read `docs/roadmap.md`.
3. Confirm the phase scope and non-goals.
4. If docs and runtime disagree, document the mismatch and propose a bounded fix.

Use these as active sources of truth:
- `docs/product-vision.md`
- `docs/architecture.md`
- `docs/provider_contract.md`
- `docs/threat-model.md`
- `docs/verification.md`
- `SECURITY.md`

Superseded planning docs are in `docs/archive/` and are historical only.

## Git and PR discipline
- Keep changes small and reviewable.
- One clear goal per commit.
- Before risky history operations (`rebase`, `reset`, `force push`), show:
  - `git status`
  - `git branch -vv`
  - `git log -n 10`

## Android implementation rules
- Keep heavy work off the main thread.
- Compose state should be stable and predictable.
- Use Room and DataStore responsibly.
- Do not log sensitive user content.
- Keep permissions minimal.
- Preserve accessibility semantics and test tags.

## Observability
- Local diagnostics only.
- Redact sensitive fields.
- No third-party tracking tooling.

## Verification gates
After meaningful changes, run from `android/`:
1. `./gradlew test`
2. `./gradlew assembleDebug`
3. `./gradlew :app:connectedDebugAndroidTest` for UI/instrumentation-impacting slices.

If connected tests need a device, start `Medium_Phone_API_35`.

## Review checklist before merge
- Scope matches active roadmap phase.
- No hidden online triggers added.
- Deterministic tests added or updated where behaviour changed.
- Docs and runtime claims remain aligned.

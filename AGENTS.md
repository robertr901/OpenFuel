# OpenFuel Engineering Rules (AGENTS.md)

OpenFuel is a privacy-first, local-first nutrition tracker.
Default stance: security and privacy are features.

## Priorities (in order)
Security > Privacy > Reliability/Data Integrity > Performance > Usability/Accessibility > Maintainability > Future-proofing

## Hard constraints
- Local-first by default. Personal meal logs must not be stored on the server.
- Network calls must be explicit (user-enabled) and resilient to offline.
- No secrets in repo. Use env vars and `.env.example`.
- Avoid binary files in PRs unless explicitly requested:
  .png/.jpg/.webp/.jar/.keystore/.jks/.zip/.gz/.apk/.aab/.pdf
  If a binary is needed (e.g. Gradle wrapper jar), document manual steps instead of adding it.
- Never invent command output. Run commands or ask the human to run them.

## Git/PR discipline
- Small, reviewable diffs. One goal per PR.
- Before rebase/reset/force push: show `git status`, `git branch -vv`, `git log -n 10`.
- Conflict rule: if add/add baseline duplicates, prefer main’s version; preserve unique feature work.

## Android (front-end) rules
- No heavy work on the main thread. Use coroutines properly.
- Compose: stable state, avoid unnecessary recompositions.
- Use Room + DataStore; migrations must be safe.
- No sensitive logging (meal logs, exports) to Logcat.
- Least-privilege permissions; use FileProvider for exports.
- Accessibility: labels/content descriptions, large text, sensible error states.

## Backend rules (when added)
- Validate all inputs (schema). Parameterised SQL only.
- Rate limiting, payload limits, and timeouts by default.
- Safe CORS/headers by default; do not expose debug endpoints.

## Observability (no creepiness)
- Local-only diagnostics by default; sharing logs must be opt-in and redacted.

## Verification (don’t skip)
- After meaningful changes, provide commands to verify:
  - Android: `./gradlew assembleDebug` (or equivalent)
  - Backend: `npm test` (when present)

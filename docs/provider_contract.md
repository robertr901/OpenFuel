# Provider Contract

## Purpose
This document defines the contract for remote food providers used by Add Food online lookup.

## Contract boundaries
- Local search remains local-first.
- Provider execution is explicit-action only.
- Typing, debounce, and passive lifecycle states must not trigger provider calls.

## Required provider metadata
- `providerId`: stable unique key.
- `displayName`: user-facing provider label.
- `priority`: deterministic ordering key.
- `capabilities`: one or both of:
  - `TEXT_SEARCH`
  - `BARCODE_LOOKUP`

## Request contract
- Calls require a valid user-initiated network token.
- Request types:
  - text search with non-blank query,
  - barcode lookup with non-blank barcode.
- Source filters and settings determine whether online providers can run.

## Result contract
Each provider run returns structured output:
- `providerId`
- `capability`
- `status`
- mapped `items`
- `elapsedMs`
- optional diagnostics (local-only)
- cache marker (`fromCache`)

Provider execution must not throw UI-facing exceptions.

## Deterministic merge and dedupe
- Primary key precedence:
  - barcode when present,
  - otherwise normalised name and brand context,
  - fallback identity key when needed.
- Deterministic tie-breaks use stable ordering and consistent richness rules.
- Provenance is preserved for user trust cues.

## Error model
- Use user-safe error states.
- No stack traces in UI.
- No sensitive payload logging.
- Typical statuses include:
  - disabled by settings,
  - misconfigured,
  - unsupported capability,
  - guard rejected,
  - network unavailable,
  - timeout,
  - parsing or service error.

## Caching policy
- Cache key includes normalised input, request type, and provider id.
- Cache stores public nutrition candidate fields only.
- Cache reads are allowed for explicit fast-path responses.
- No silent background refresh.

## Test and quality requirements
- Provider mapping should be locked with fixture-based contract tests.
- Regression coverage should remain deterministic and local.
- Changes to this contract must update tests and docs in the same slice.

# Provider Contract (Phase 8)

## Purpose
This document defines the canonical contract for remote nutrition catalog providers used by Unified Add Food.

## Required provider metadata
- `providerId`: stable unique key (for diagnostics, cache keys, provenance).
- `displayName`: user-facing provider name.
- `priority`: lower number executes first for deterministic merge tie-breaks.
- `capabilities`: one or both of:
  - `TEXT_SEARCH`
  - `BARCODE_LOOKUP`

## Request contract
- All online lookups must be explicitly user-initiated and provide a valid `UserInitiatedNetworkToken`.
- Provider requests are typed:
  - `TEXT_SEARCH` with non-blank `query`
  - `BARCODE_LOOKUP` with non-blank `barcode`
- `SearchSourceFilter` controls whether online providers are eligible to run.

## Canonical result contract
Each provider execution returns:
- `providerId`
- `capability`
- `status` (success/empty/disabled/error class)
- `items` (`RemoteFoodCandidate` list)
- timing (`elapsedMs`)
- optional diagnostics (local-only)
- cache source marker (`fromCache`)

Provider execution must never throw to UI layers. Failures are represented as structured statuses.

## Normalization and dedupe rules
- Primary dedupe key:
  - barcode (if present, trimmed non-blank)
  - else normalized `name + brand + servingSize`
- Text normalization:
  - trim
  - collapse repeated whitespace
  - lowercase with `Locale.ROOT`

## Error model
- Friendly UX errors only.
- No stack traces in UI.
- No sensitive payload logging.
- Common statuses: `DISABLED_BY_SETTINGS`, `DISABLED_BY_SOURCE_FILTER`, `MISCONFIGURED`, `UNSUPPORTED_CAPABILITY`, `RATE_LIMITED`, `TIMEOUT`, `ERROR`.

## Provenance
- Merged candidates retain provider provenance via `providerKey`.
- UI-level stable ids include provenance to avoid collisions across providers.

## Caching policy
- Cache key includes normalized input + request type + provider id.
- Cache stores only public nutrition candidate fields.
- Default TTL: 24h.
- Cache reads are allowed for fast-path results.
- No silent background network refresh; refresh requires explicit user action.

## Observability (local-only)
- Track per-provider elapsed time, status, and counts.
- Track overall elapsed time and cache hit/miss counters.
- Diagnostics are local debug information only and are never transmitted.

# Provider Contract (Phase 9)

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
- Typing, local query updates, and app start events must not trigger provider execution.

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
  - fallback to source-scoped identity when brand/serving context is missing
- Text normalization:
  - trim
  - collapse repeated whitespace
  - lowercase with `Locale.ROOT`

## Error model
- Friendly UX errors only.
- No stack traces in UI.
- No sensitive payload logging.
- Common statuses:
  - `DISABLED_BY_SETTINGS`
  - `DISABLED_BY_SOURCE_FILTER`
  - `MISCONFIGURED`
  - `UNSUPPORTED_CAPABILITY`
  - `GUARD_REJECTED`
  - `NETWORK_UNAVAILABLE`
  - `HTTP_ERROR`
  - `PARSING_ERROR`
  - `RATE_LIMITED`
  - `TIMEOUT`
  - `ERROR`

## Provenance
- Merged candidates retain provider provenance via `providerKey`.
- UI-level stable ids include provenance to avoid collisions across providers.

## Trust and provenance direction (Phase 27 roadmap)
- Phase 27 direction focuses on clearer provenance and completeness messaging in the UI.
- This is roadmap direction only and does not change current provider execution behaviour.
- Reference: `docs/phase-25/phase-25-plan.md` (Phase 27 - Data Trust Layer v1).

## Caching policy
- Cache key includes normalized input + request type + provider id.
- Cache stores only public nutrition candidate fields.
- Default TTL: 24h.
- Cache row contract includes `cacheVersion`; current version is `1`.
- Version bump policy: bump `cacheVersion` when serialized payload shape changes incompatibly.
- Cache reads are allowed for fast-path results.
- Version mismatch rows are purged and treated as cache misses.
- Corrupted payload JSON is treated as cache miss and overwritten on next successful fetch.
- No silent background network refresh; refresh requires explicit user action.

## Observability (local-only)
- Track per-provider elapsed time, status, and counts.
- Track overall elapsed time and cache hit/miss counters.
- Diagnostics are local debug information only and are never transmitted.

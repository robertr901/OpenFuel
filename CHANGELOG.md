# Changelog

All notable changes to OpenFuel are documented here.

## Unreleased

### Phase 13: Security/docs/release hardening
- Refreshed core documentation to remove local absolute paths and standardize repo-relative verification instructions.
- Elevated deterministic instrumentation (`:app:connectedDebugAndroidTest`) as a first-class release gate in docs.
- Added explicit release hygiene and security/privacy evidence documentation artifacts.
- No provider execution behavior changes, no cache schema changes, no online guard behavior changes, and no telemetry additions.

### Phase 12c: Unified quick add + voice EUX refinement
- Replaced split quick-add controls with a single `Quick add` entry in Add Food.
- Kept text parsing primary and moved manual macro logging behind a collapsed `Manual details` expander.
- Integrated voice capture as an explicit trailing mic action in quick-add input.
- Kept guardrails: no auto-search, no auto-save/log from parsed or voice text.

### Phase 12b: Intelligence seam hardening
- Added deterministic golden corpus tests for parser/normalizer behavior.
- Hardened rule-based parsing edge cases (comma decimals, uppercase units, leading noise tokens).
- Clarified deterministic, non-throwing contract expectations for intelligence parsing.

### Phase 12a: Add Food UX and accessibility polish
- Reworked Add Food hierarchy and section clarity with calmer composition.
- Kept online actions explicit and visually subordinate.
- Improved keyboard-first behavior and TalkBack semantics on quick-add and voice flows.

## Release Checklist

Run from `android/` before release or PR merge:

1. Build/Test gates:
   - `./gradlew test`
   - `./gradlew assembleDebug`
   - `./gradlew :app:connectedDebugAndroidTest`
2. Quick manual QA:
   - Verify Add Food local search, quick add, and barcode navigation flows.
   - Verify online lookup remains explicit action only (`Search online` / `Refresh online`).
   - Verify online-disabled setting blocks network actions with clear user feedback.
3. Privacy/permissions checks:
   - Confirm no telemetry/analytics/crash-reporting SDKs were added.
   - Confirm no new dangerous permissions were added unintentionally.
   - Confirm voice capture remains explicit action and no background capture/service is introduced.
4. Documentation consistency:
   - Ensure README, architecture, roadmap, verification, and security docs match runtime behavior.
   - Ensure instructions are repo-relative and do not include local absolute filesystem paths.

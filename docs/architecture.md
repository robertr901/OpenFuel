# Architecture

## Purpose
This document describes the current Android architecture and the boundaries that must remain stable while the product evolves.

## System map
```text
Compose UI
  -> ViewModel intent handlers
    -> domain services and repositories
      -> local persistence (Room/DataStore) and explicit online provider execution
        -> mapped domain models
          -> immutable UI state
```

## Invariants
- Offline-first defaults.
- Explicit-action networking model.
- No background provider execution from passive UI states.
- Deterministic behaviour and deterministic tests.
- Local diagnostics only.

## Layer boundaries
- `ui`: Compose screens, UI components, navigation.
- `viewmodel`: state machines and intent handling.
- `domain`: pure models, rules, and repository contracts.
- `data`: Room/DataStore, provider clients, mapping, and repository implementations.
- `shared-core`: pure Kotlin shared rules used by Android module (normalisation and selection logic).

## Core data paths
1. Logging and daily summaries
- User actions flow from screen to ViewModel to local repository.
- Room/DataStore remain the source of truth for history and settings.

2. Add Food local search
- Query updates run local search via deterministic debounce and local matching.
- Query typing must not trigger online execution.

3. Explicit online search
- Online provider execution only runs from explicit user actions.
- ViewModel issues guarded requests into provider orchestration.
- Orchestrator merges provider outputs deterministically and returns stable UI models.

4. Barcode flow
- Barcode lookup remains explicit action through scan or retry handlers.
- Stale responses are ignored by state machine policy.

## Provider architecture
- Registry resolves available providers by settings and local configuration.
- Orchestrator runs providers with deterministic ordering and bounded execution.
- Provider failures are isolated so one provider failure does not erase successful results.
- Provider mapping is treated as untrusted input and parsed defensively.

## Trust surfaces
- Source and completeness cues are presented in Add Food online results.
- Serving and unit uncertainty can be flagged as review-needed states.
- Selection reasoning remains deterministic and explainable.

## Persistence
- Room stores foods, meal logs, and related local entities.
- DataStore stores settings and lightweight user preferences.
- Export is user-initiated and local.

## Testing architecture
- Unit tests cover domain rules, mapping, and state transitions.
- Provider contract tests use local fixtures only.
- Instrumentation tests run on a pinned emulator profile with deterministic test runner overrides.
- Canonical release gates are defined in `docs/verification.md`.

## Where to change what
- Add Food screen composition and tags:
  - `android/app/src/main/java/com/openfuel/app/ui/screens/AddFoodScreen.kt`
- Today screen composition:
  - `android/app/src/main/java/com/openfuel/app/ui/screens/HomeScreen.kt`
- Settings screen composition:
  - `android/app/src/main/java/com/openfuel/app/ui/screens/SettingsScreen.kt`
- Add Food state machine and explicit online actions:
  - `android/app/src/main/java/com/openfuel/app/viewmodel/AddFoodViewModel.kt`
- Barcode state machine:
  - `android/app/src/main/java/com/openfuel/app/viewmodel/ScanBarcodeViewModel.kt`
- Provider orchestration:
  - `android/app/src/main/java/com/openfuel/app/data/remote/ProviderExecutorOnlineSearchOrchestrator.kt`
- Provider contracts and fixtures:
  - `android/app/src/test/java/com/openfuel/app/provider/contracts/`
  - `android/app/src/test/resources/provider_fixtures/`

## Related docs
- Product vision: `docs/product-vision.md`
- Roadmap: `docs/roadmap.md`
- Provider contract: `docs/provider_contract.md`
- Threat model: `docs/threat-model.md`
- Verification: `docs/verification.md`
- Security policy: `SECURITY.md`

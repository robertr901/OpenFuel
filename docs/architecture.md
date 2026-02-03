# Architecture

## Layered structure
```
ui (Compose screens, navigation)
  ↓
ui-state + viewmodels (StateFlow)
  ↓
repositories (interfaces in domain, implementations in data)
  ↓
data (Room, DataStore, export serializer)
  ↓
domain (pure logic, calculations, unit helpers)
```

## Package layout (Android app module)
- `com.openfuel.app.ui`: screens, navigation, design system
- `com.openfuel.app.viewmodel`: ViewModels and UI state models
- `com.openfuel.app.domain`: pure domain logic, repository interfaces, models
- `com.openfuel.app.data`: Room entities/DAO/db, repository implementations, DataStore
- `com.openfuel.app.export`: export serialization + file writing

## Data flow
1. UI events (e.g., log meal) call ViewModel intent methods.
2. ViewModel updates StateFlow UI state and invokes repositories.
3. Repositories persist to Room/DataStore.
4. DAO emits flows, which ViewModel collects and maps to UI state.

## Offline-first strategy
- All logs stored in Room locally.
- Settings stored in DataStore locally.
- Online lookup is disabled by default and gated by a setting.
- Export uses local-only FileProvider sharing.

## Resilience
- Writes are transactional where needed (Room).
- UI state is immutable, derived from stable flows.
- Domain logic is pure and tested (no Android dependencies).

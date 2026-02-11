# OpenFuel

Log what you ate in under 10 seconds, and know what it means.

OpenFuel is an Android nutrition logging app focused on speed, clarity, and trust.

## Who this is for
- People who want fast daily logging without clutter.
- People who want clear interpretation of progress.
- People who need confidence in source and completeness cues.

## First-minute experience
OpenFuel aims to make this sequence simple:
1. Choose a goal profile in 20 seconds or less.
2. Log a first meal in 30 seconds or less.
3. See an immediate, readable daily summary.
4. Understand trust cues for imported data.

## Principles
- Utility first: reduce work in every core action.
- Calm design: no pressure mechanics or manipulative prompts.
- Offline-first default: core logging works without internet.
- Explicit-action networking: online calls run only when the user asks.
- Deterministic quality: stable behaviour and repeatable tests.

## Android quick start
From repository root:

```bash
cd android
./gradlew test
./gradlew assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

If you need to start the reference emulator first:

```bash
emulator -avd Medium_Phone_API_35 -no-window -no-audio
```

Install debug build on a connected device/emulator:

```bash
cd android
./gradlew installDebug
```

## Verification gates
Canonical gate order is:
1. `./gradlew test`
2. `./gradlew assembleDebug`
3. `./gradlew :app:connectedDebugAndroidTest`

See `docs/verification.md` for deterministic test guidance.

## Product status
- Current direction: Phase 32 experience-first docs reset and roadmap alignment.
- Forward roadmap: `docs/roadmap.md`
- Change history: `CHANGELOG.md`

## Documentation map
- Docs index: `docs/README.md`
- Product vision: `docs/product-vision.md`
- Architecture: `docs/architecture.md`
- Provider contract: `docs/provider_contract.md`
- Threat model: `docs/threat-model.md`
- Security policy: `SECURITY.md`

## Networking stance
- Core usage is local and offline-first.
- Online food lookup remains explicit action only.
- No background provider polling from passive screens.

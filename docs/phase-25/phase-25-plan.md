# Phase 25 Product Review and Direction Reset Plan (2026)

## Executive summary
OpenFuel is a privacy-first, local-first nutrition tracker for people who want fast, trustworthy food logging without surveillance.

OpenFuel serves users who value:
- reliable daily logging with low friction,
- accurate and explainable nutrition data,
- a calm, respectful product experience,
- full ownership of their personal health history.

OpenFuel will not become:
- an ad-supported attention product,
- a social network,
- a background-sync tracker,
- a black-box AI coach,
- an account-gated ecosystem.

## Product positioning
### Category definition
OpenFuel is a **private nutrition ledger**: a local-first food logging and nutrition insight tool where online lookup is optional and always explicit.

### JTBD we will excel at (and trade-offs)
| Priority JTBD | What success looks like | Trade-off we accept |
|---|---|---|
| Fast daily logging | User logs known foods in under 20 seconds median | We prioritize speed and clarity over novelty features |
| Trustworthy nutrition decisions | Source provenance and data completeness are visible; users can verify key numbers | We avoid broad crowdsourced/social input loops that reduce data quality |
| Calm private trend review | Weekly/monthly insights are local, explainable, and non-judgmental | We reject engagement gimmicks (streak pressure, shame copy, fear prompts) |

### Why these JTBDs
These priorities are directly aligned with the research findings:
- dominant user pain = logging friction,
- biggest trust gaps = bad database quality and unexplained numbers,
- biggest retention risks = guilt-driven UX and noisy upsell patterns.

## Competitive landscape synthesis
| Copy (table stakes) | Refuse | Uniquely win |
|---|---|---|
| Fast search, barcode scan, favorites/recents, macro totals, export, Pro tier | Ads, telemetry/tracking SDKs, account requirement, social-feed pressure loops, hidden background networking | Local-first + explicit-action networking + deterministic quality + provenance-first UX |
| Multi-provider online lookup as optional enhancement | Auto-running online requests on typing/app launch | Clear online-source status with "what ran / what failed / why" |
| Basic insights (trends, summaries) | Black-box "AI coaching" with opaque logic and medical-style claims | Rule-based, explainable insights with safety language and user control |
| Purchase + restore flows for premium features | Manipulative paywall urgency and nagging | Respectful paywall with clear value and easy dismissal |

## "Bentley UX" principles with measurable standards
| Principle | Product standard | Measurable guardrail |
|---|---|---|
| Speed as craftsmanship | Core flows feel immediate and stable | Local search results visible p95 <= 250ms after debounce; known-food log median <= 20s |
| Calm language | Neutral, factual copy with no guilt framing | No punitive phrasing in core flow copy; UX review check in every phase |
| Reversible actions | Users can recover quickly from mistakes | Edit/delete/undo available for log actions; no irreversible one-tap destructive flow |
| One primary action per surface | Reduced cognitive load | Each primary screen has one dominant CTA; secondary actions are visually subordinate |
| No nagging | Respectful premium posture | No forced upsell modal loops; paywall shown only on gated entry or explicit upgrade action |
| Predictable networking | Zero surprise data egress | 0 online requests without explicit user action; explicit online-source disclosure on every run |

## Phase roadmap (26-31)
| Phase | Goal | Why now | Deliverables | Non-goals | Dependencies | Risks | Acceptance criteria | Test approach |
|---|---|---|---|---|---|---|---|---|
| 26 - Reliability and UX Clarity Baseline | Remove highest-friction reliability/clarity issues without feature expansion | Trust is easiest to lose and hardest to recover; stabilize before expansion | Error-copy normalization, online status deduping, local latency instrumentation, Add Food/Settings accessibility pass | No new providers, no schema changes, no new permissions | Current Add Food/ViewModel/provider status seams | Scope creep into redesign; regressions in messaging states | Fewer duplicate/conflicting online messages; no behavior regressions; all gates green | ViewModel state tests, deterministic UI smoke tests, offline/airplane scenarios |
| 27 - Data Trust Layer v1 | Strengthen provenance, serving/unit consistency, and "why" transparency | Core JTBD includes trustworthy data and explainability | Provenance presentation standards, confidence/completeness messaging, correction-safe UX patterns | No cloud validation service, no AI inference | Provider mapping seams + contract fixture corpus | Overloading UI with advanced data details | Provenance shown for imported candidates; completeness messaging is deterministic and non-blocking | Golden corpora tests, provider contract suite, regression snapshots |
| 28 - Shared Core for Cross-Platform (KMP) | Extract shared domain rules and mapping logic for Android+iOS parity | Reduces duplication before iOS build-out | Shared module boundaries, parity tests, platform adapter contracts | No full iOS release in this phase, no schema changes | Stable domain/model boundaries and existing deterministic tests | Cross-platform build complexity; timeline slip | Shared-core tests pass; Android behavior unchanged | Shared JVM tests, adapter contract tests, deterministic integration checks |
| 29 - iOS MVP (offline-first parity) | Ship iOS MVP for core logging and explicit online lookup | Cross-platform expectation is now strategic, not optional | Today/Add Food/Settings baseline, export, explicit online actions, no account requirement | No cloud sync, no advanced wearable parity | Phase 28 shared-core readiness | iOS parity drift; App Store policy interpretation risk | Core logging/export/offline work on iOS; explicit online action behavior preserved | Shared-core parity suite, iOS flow smoke tests, offline checks |
| 30 - Wear OS Utility Slice | Deliver minimal high-value wrist workflows | Improves capture speed for committed users without expanding scope | Quick add recents, daily progress glance, explicit sync-now action | No background sync loops, no broad health-platform ingestion | Stable Android core + explicit-action orchestration | Wear scope creep; battery/performance issues | Useful watch actions work without hidden networking; no autonomous online fetch | Deterministic integration tests with fakes, queue/retry tests |
| 31 - Cross-Platform Stabilization and Release Readiness | Harden Android+iOS+Wear quality and parity before further expansion | Prevent compounding complexity and release risk | Parity matrix closure, performance hardening, docs truth audit, release checklist | No new headline features, no schema changes unless explicitly opened | Phases 26-30 complete and testable | Quality debt accumulation; release fatigue | Release gates stable, parity checklist met, manual QA checklist fully passable | Full regression sweep, anti-flake audit, manual QA checklist validation |

## Scope control
### Stop doing
- Adding providers without a clear quality/coverage gap and ownership plan.
- Shipping features with flaky tests or unresolved deterministic failures.
- Using engagement gimmicks (streak pressure, shame copy, dark-pattern upsells).
- Expanding platform/device support without explicit acceptance criteria.
- Introducing "AI" features that cannot be explained, tested, and bounded.

### Kill criteria (de-scope/delay triggers)
| Trigger evidence | Action |
|---|---|
| Any candidate phase item requires background networking to feel usable | De-scope item; redesign to explicit-action model |
| Any item requires schema/migration work but phase does not explicitly include schema change | Delay item to dedicated schema phase |
| New feature cannot be covered by deterministic unit/UI tests | Delay until deterministic test path is defined |
| User value is unclear and measurable outcome is missing | De-prioritize to backlog |
| UX introduces guilt/shame or manipulative urgency | Reject copy/flow and rework before merge |
| Cross-platform work causes sustained parity regressions in core logging | Freeze net-new scope and enter stabilization mode |

## Cross-platform strategy decision
### Recommendation: Kotlin Multiplatform (KMP) for shared domain/services/tests
**Why**
- Best fit for current Android-first codebase.
- Preserves existing Kotlin domain logic, contract tests, and provider parsing rigor.
- Enables deterministic shared test harnesses across Android+iOS logic.
- Minimizes rewrite risk for core rules (normalization, mapping, guard logic).

### Alternative evaluated: Flutter
**Trade-offs**
| Dimension | KMP (Recommended) | Flutter (Alternative) |
|---|---|---|
| Dev effort from current state | Lower for shared logic migration; keep Android UI stack | Higher due to broader UI and platform-layer rewrite |
| Test reuse | High reuse of existing JVM/domain tests | Lower direct reuse; more adaptation required |
| Performance and platform feel | Native UI per platform; predictable platform behavior | Good performance but shared UI may require platform-specific polish work |
| iOS trajectory | Strong for shared business logic with native iOS UI freedom | Faster single-codebase UI potential but larger architecture pivot |
| Constraint fit (determinism/local-first) | Strong alignment with current seams | Possible but with higher transition risk |

## Wearables strategy decision
### Wear OS feature subset (Phase 30 target)
- Quick add recent foods.
- Daily macro/progress glance.
- Explicit sync-now action.
- Optional manual barcode result review continuation from phone.

### Why this subset
- High utility with minimal complexity.
- Preserves explicit-action networking.
- Avoids background sync behavior that conflicts with privacy and battery constraints.

### Apple Watch entry criteria
- iOS core logging parity established first.
- Shared-domain contracts stable.
- Clear no-background-network architecture for watch flows.
- Deterministic watch flow tests/fakes available before shipping.

## Local-first insights strategy (rule-based first)
### Strategy
- Use rule-based, explainable insight generation only in this horizon.
- No AI coach, no photo calorie estimation as core loop.
- Every insight includes:
  1) **What was observed**,
  2) **Why it matters**, 
  3) **How to act safely**.

### Example insight cards (with safety language)
1. **Protein distribution is back-loaded**
   - Why: Last 14 days show breakfast protein median below target while dinner is above target.
   - Safe action: "Consider adding a small protein source earlier in the day if it fits your goals. This is general guidance, not medical advice."

2. **High sodium variance on weekdays**
   - Why: Weekday sodium average exceeds weekend average by a meaningful margin.
   - Safe action: "If blood pressure is a concern, review high-sodium items with a clinician. OpenFuel does not provide medical diagnosis."

3. **Fiber consistency improved week-over-week**
   - Why: 7-day rolling average increased and stayed above baseline.
   - Safe action: "Sustained changes are typically more useful than one-day spikes. Keep changes gradual and sustainable."

### Safety constraints
- No moralized food labels ("good/bad").
- No rapid-weight-loss encouragement language.
- No medical claims, diagnoses, or medication guidance.

## Final constraint compliance matrix
| Phase | Offline-first | Explicit online only | No telemetry/ads/trackers | Deterministic tests required | Schema change policy respected |
|---|---|---|---|---|---|
| 26 | Yes | Yes | Yes | Yes | Yes (no schema changes) |
| 27 | Yes | Yes | Yes | Yes | Yes (no schema changes) |
| 28 | Yes | Yes | Yes | Yes | Yes (no schema changes) |
| 29 | Yes | Yes | Yes | Yes | Yes (no schema changes) |
| 30 | Yes | Yes | Yes | Yes | Yes (no schema changes) |
| 31 | Yes | Yes | Yes | Yes | Yes (no schema changes unless explicitly opened) |

## Evidence references (source anchors)
- Deep research synthesis (`A`-`G`) in: `/Users/robertross/Documents/OpenFuel - Deep Research Output.rtf`
- Runtime and architecture truth:
  - `/Users/robertross/Projects/OpenFuel/docs/architecture.md`
  - `/Users/robertross/Projects/OpenFuel/docs/roadmap.md`
  - `/Users/robertross/Projects/OpenFuel/docs/verification.md`
  - `/Users/robertross/Projects/OpenFuel/docs/threat-model.md`
  - `/Users/robertross/Projects/OpenFuel/SECURITY.md`
  - `/Users/robertross/Projects/OpenFuel/docs/reviews/phase20-release-review.md`

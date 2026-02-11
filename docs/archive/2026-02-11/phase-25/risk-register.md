# Phase 25 Risk Register

| # | Risk | Category | Likelihood | Impact | Detection signals | Mitigation | Owner |
|---|---|---|---|---|---|---|---|
| 1 | Provider payload drift breaks mapping quality | Engineering | M | H | Contract fixture failures; support reports of odd nutrients/servings | Maintain fixture corpus, contract tests, and strict parser fallbacks | Engineering |
| 2 | Hidden/implicit online calls break offline trust | Privacy/Security | L | H | Network traces show requests without user action; user complaints | Keep explicit-action guard invariant; add regression checks for online triggers | Engineering |
| 3 | Paywall resentment reduces trust and conversion | Product | M | M | Increased paywall dismissals, negative review language around nagging | Keep respectful paywall triggers and plain value communication | Product |
| 4 | Instrumentation flakiness slows delivery | Engineering | M | H | Intermittent CI failures not reproducible locally | Anti-flake policy, emulator pinning, no sleeps, fail-fast triage | Engineering |
| 5 | iOS parity slips due to underestimated scope | Maintenance/Solo-dev | M | H | Milestone rollovers, unresolved parity matrix gaps | Narrow iOS MVP scope; ship parity baseline first | Product + Engineering |
| 6 | Wearable scope creep creates low-value complexity | Product | M | M | New wearable backlog items exceed defined subset | Lock Wear OS subset, add kill criteria for low-usage features | Product |
| 7 | Monetization conversion misses sustainability threshold | Product | M | H | Low Pro conversion after release windows | Improve Pro anchor clarity; preserve respectful upsell; reassess packaging | Product |
| 8 | Ambiguous health wording triggers store/compliance issues | Store/Compliance | M | H | App review feedback; user reports of "medical advice" framing | Safety language standards, no diagnosis claims, review copy checklist | Product + Compliance |
| 9 | Local data backup expectations conflict with strict privacy messaging | Product | M | M | User confusion around restore expectations and device migration | Clear export/import guidance and recovery docs | Product |
| 10 | Cross-platform architecture choice (KMP) under-delivers velocity | Engineering | M | M | Persistent blocked tasks in shared-core extraction | Timebox KMP checkpoints; keep Flutter fallback documented | Engineering |
| 11 | Performance regressions in core logging loop | UX/Accessibility | M | H | Increased logging latency and QA complaints | Set SLOs, add perf checks to release checklist | Engineering |
| 12 | Accessibility regressions from UI polish work | UX/Accessibility | M | H | TalkBack/focus issues; test failures at large text | Include a11y checks in every phase gate and QA checklist | Engineering + UX |
| 13 | Provider setup complexity causes user confusion | Product | M | M | High rate of "needs setup" confusion in support feedback | Improve setup diagnostics copy and verification docs | Product |
| 14 | Over-optimization for niche users hurts onboarding clarity | UX/Accessibility | M | M | New users drop off before first successful log | Keep one primary action per surface and simplify first-run flows | UX |
| 15 | Security documentation drifts from runtime behavior | Privacy/Security | M | M | Contradictions found during release review | Keep docs-truth audit in stabilization phase, docs consistency checks | Engineering + Compliance |
| 16 | Solo-dev bandwidth constraints cause phase overruns | Maintenance/Solo-dev | H | M | Repeated deferments and growing WIP | Strict scope control, kill criteria, stabilization buffer in roadmap | Product |
| 17 | App store policy changes impact billing/wearable/privacy flows | Store/Compliance | L | M | New policy notices; review rejections | Track policy deltas per release; keep claims factual and minimal | Compliance |
| 18 | Data quality confidence messaging overwhelms casual users | UX/Accessibility | M | M | Usability sessions show confusion or ignored controls | Progressive disclosure for advanced diagnostics | UX |

## Risk ownership model
- Product: prioritization, monetization, UX tone.
- Engineering: reliability, determinism, architecture execution.
- Compliance/Security: policy and privacy posture alignment.
- UX: clarity, accessibility, cognitive load.

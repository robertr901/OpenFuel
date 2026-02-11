# Phase 25 Decision Log (ADR-lite)

| # | Decision | Status | Rationale | Alternatives considered | Constraint impact |
|---|---|---|---|---|---|
| 1 | Position OpenFuel as a privacy-first, local-first nutrition ledger | Accepted | Most defensible niche versus mass-market incumbents; aligns with current architecture and user trust goals | Broad "all-in-one wellness super app" | Reinforces offline-first, no-account, no-telemetry posture |
| 2 | Prioritize JTBD: fast logging, trustworthy data, calm insights | Accepted | These three jobs map to strongest user value and retention signal in research | Add social/community JTBD now | Preserves focus and avoids scope creep |
| 3 | Reject social feed, leaderboards, and challenge mechanics | Accepted | High complexity and trust/mental-health downside; weak fit for product identity | "Opt-in social lite" | Prevents account pressure and behavioral dark patterns |
| 4 | Reject AI coach and photo-calorie as core loops in this horizon | Accepted | Accuracy/safety risk too high for trust-first product; low deterministic testability | Limited experimental AI pilot | Preserves explainability and deterministic quality |
| 5 | Keep all networking explicit-action only | Accepted | Core privacy promise and current runtime architecture depend on this invariant | Auto-refresh on typing/app open | Prevents hidden network calls and trust breakage |
| 6 | Maintain no telemetry/ads/third-party tracking policy | Accepted | Trust moat and product principle; avoids surveillance incentives | Minimal analytics SDK | Requires stronger local evidence and manual research loop |
| 7 | Pro value must be respectful and non-manipulative | Accepted | Monetization should not undermine premium calm UX | Aggressive timed paywall tactics | Keeps premium posture aligned with trust principles |
| 8 | Recommend KMP shared core for cross-platform | Accepted | Best leverage from existing Kotlin domain/services/tests and lower rewrite risk | Flutter | Preserves deterministic shared business logic tests |
| 9 | Evaluate Flutter as fallback alternative only | Accepted | Keeps option open if KMP velocity or team constraints change | React Native | Limits architecture churn while documenting trade-offs |
| 10 | Wear OS-first wearable rollout with narrow utility scope | Accepted | Android-first sequencing and lower expansion risk | Build Apple Watch first | Preserves explicit-action and reliability constraints |
| 11 | Apple Watch follows only after iOS parity baseline | Accepted | Avoids triple-platform fragmentation before core parity | Parallel Watch+iOS launch | Reduces maintenance/QA overhead for small team |
| 12 | No schema/migration work in phases 26-31 by default | Accepted | Roadmap focus is reliability, trust, parity, and polish | Opportunistic schema tweaks during feature work | Enforces blast-radius control |
| 13 | Add a stabilization-first phase (Phase 26) before expansion | Accepted | Quality debt now costs less than cross-platform rework later | Start with iOS immediately | Lowers regression risk and preserves velocity |
| 14 | Use rule-based insights with "why" explanations and safety language | Accepted | Explainability and safety are core to trust and ED-safe UX | Opaque ML insight generator | Keeps insights auditable and bounded |
| 15 | Enforce deterministic three-gate CI as release floor | Accepted | Prevents regressions and flaky release cadence | Best-effort local testing only | Strengthens reliability and review confidence |

## Notes
- Decisions are locked for Phase 25 planning output and should be treated as baseline unless superseded by a formal follow-up ADR.
- Any future exception requires explicit documentation of constraint impact.

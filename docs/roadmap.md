# OpenFuel Roadmap (Phase 32 to Phase 50)

This roadmap is the canonical forward plan.

OpenFuel is Android-first and experience-first, with one product promise:

- Log what you ate in under 10 seconds, and know what it means.

Historical phase plans are preserved under `docs/archive/2026-02-11/`.

## Planning guardrails
- Offline-first remains the default.
- Explicit-action networking remains the operational model.
- No dark patterns: no guilt copy, no urgency timers, no variable-ratio rewards.
- Deterministic testing remains mandatory.
- No schema changes or migrations unless a specific future phase explicitly opens schema work.

## First-minute experience target
The product should support this sequence for first-time users:
1. Choose a goal profile in 20 seconds or less.
2. Log a first meal in 30 seconds or less.
3. See a calm, understandable summary immediately.
4. Understand why shown data is trustworthy through clear source and completeness cues.

## How we prioritise
- North star metric: users who log on at least 3 days per week.
- Supporting metrics:
  - first meal logged in 30 seconds or less from Add Food entry,
  - first-minute completion rate (goal profile + first log + summary viewed),
  - day-7 retention for first-week users,
  - trust-comprehension score from moderated usability checks.
- Review cadence: full direction reviews at phases 35, 40, 45, and 50.

## Roadmap v2
| Phase | Theme | Outcomes (2-4 bullets) | Not doing (1 bullet) | Review phase? |
|---|---|---|---|---|
| 32 | Docs reset and roadmap v2 | - Unify product docs around one promise.<br>- Establish one canonical roadmap and archive superseded plans.<br>- Align verification, security, and architecture language. | No production code changes. | No |
| 33 | Today 10x: clarity and speed | - Make Today the fastest path to the next log action.<br>- Reduce clutter and improve scanability for one-thumb use.<br>- Improve first-action clarity for repeat users. | No new feature families. | No |
| 34 | Goal profiles v1 | - Introduce clear profile selection flows (for example fat loss, muscle gain, maintenance, blood sugar awareness, low-FODMAP overlays).<br>- Apply profile-aware defaults to wording and targets.<br>- Keep profile edits reversible and understandable. | No diagnostic or clinical claims. | No |
| 35 | Review A | - Product and technical truth audit completed.<br>- Ranked top issues and risks recorded with evidence.<br>- Rebaseline proposed for phases 36-40.<br>- See `docs/reviews/phase35-review-a.md` for evidence and rationale. | No net-new expansion during review. | Yes |
| 36 | Foods 10x as one system | - Reduce repeat-log path to two taps or less from Today for common known-food cases.<br>- Unify Today, Add Food, and Foods into one fast logging system.<br>- Keep trust cues visible while reducing decision friction in core capture flows. | No provider expansion or new feature families. | No |
| 37 | Weekly Review v1 | - Ship one calm weekly review ritual with one actionable recommendation.<br>- Keep recommendation wording plain-language, explainable, and non-judgemental.<br>- Keep flow short enough to complete in under a minute. | No coaching engine or punitive feedback mechanics. | No |
| 38 | Habit loops v1 (utility-led) | - Improve reminder usefulness with explicit controls, caps, and quiet hours.<br>- Keep reminder CTA tied to immediate logging actions, not pressure loops.<br>- Maintain reversible user controls and calm interaction language. | No variable-ratio rewards, urgency timers, or shame-based copy. | No |
| 39 | Trust and data quality v1 | - Standardise source/completeness/review-needed cues across core surfaces.<br>- Clarify uncertain data states with explicit, correction-safe language.<br>- Keep all user-facing trust decisions explainable and deterministic. | No hidden auto-corrections or clinical claims. | No |
| 40 | Review B | - Re-audit retention movement, reliability, and friction drivers from phases 36-39.<br>- Reprioritise phases 41-45 using measured outcomes and risk trend data.<br>- Refresh docs and acceptance criteria from evidence. | No scope inflation during review. | Yes |
| 41 | Performance and reliability | - Tighten interaction latency in core flows.<br>- Reduce failure ambiguity in capture and save paths.<br>- Harden deterministic behaviour under offline constraints. | No broad visual redesign. | No |
| 42 | Recipes v1 (cheap MVP path) | - Allow simple recipe import from open/public structured sources where feasible.<br>- Support cook-mode style step-through logging for recipes.<br>- Enable log-from-recipe in a predictable flow. | No opaque recipe inference engine. | No |
| 43 | Coaching v1 (rules-based) | - Provide explainable, rules-based guidance tied to user profile and logs.<br>- Keep wording safe, calm, and actionable.<br>- Surface clear rationale for each recommendation. | No black-box coaching system. | No |
| 44 | Trust and workflow depth | - Expand reporting and workflow support for consistency-oriented users.<br>- Improve export clarity and interpretation aids.<br>- Strengthen long-horizon review usability. | No feature sprawl without evidence. | No |
| 45 | Review C | - Mid-horizon review of utility and quality outcomes.<br>- Remove low-impact work and rebalance scope.<br>- Confirm readiness for broader distribution work. | No new headline features. | Yes |
| 46 | Computer vision capture v1 (review-before-save) | - Ship a narrow label-scan MVP where inputs are always reviewable before save.<br>- Keep confidence and source cues explicit.<br>- Ensure fallback to manual flow stays first-class. | No auto-save from vision capture. | No |
| 47 | Health constraints v2 overlays | - Improve profile overlays for dietary and health constraints in a user-safe way.<br>- Strengthen warnings and interpretation boundaries.<br>- Keep recommendations transparent and editable. | No clinical treatment claims. | No |
| 48 | Growth loops v2 (opt-in only) | - Improve shareable progress surfaces with explicit user consent.<br>- Prepare store assets and launch messaging from real product utility.<br>- Keep referral/share mechanics optional and transparent. | No forced sharing mechanics. | No |
| 49 | Release readiness | - Finalise compliance and support playbooks.<br>- Lock quality bars for launch readiness.<br>- Validate onboarding and first-week reliability. | No new major capabilities. | No |
| 50 | Review D and launch decision | - Decide launch readiness from measured outcomes.<br>- Produce post-50 roadmap from evidence.<br>- Capture lessons learned and close open risks. | No deadline-driven quality bypass. | Yes |

Business model and monetisation are intentionally out of scope until retention milestones are met.

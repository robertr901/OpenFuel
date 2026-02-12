# Phase 40 Review B Plan

Mode: IMPLEMENT NOW (DOCS-ONLY)
Guard: No non-.md changes permitted. If any code change is required, stop and ask for explicit approval.

## Objective
Complete the Phase 40 checkpoint as a docs-only slice:
- publish a canonical Phase 40 review artefact,
- refresh roadmap with locked 41-50 and outlined 51-60,
- align README and changelog with shipped phases 36-39,
- keep docs index current.

## Docs-only guards
Run these before each commit:

```bash
git diff --name-only | rg -v '\.md$' && echo "ERROR: non-doc files changed" && exit 1 || true
rg -n "<<<<<""<<|====""===|>>>>"">>>" -S . && echo "ERROR: conflict markers" && exit 1 || true
rg -n "\./gradlew|:app:connectedDebugAndroidTest" docs/verification.md .github/workflows || true
```

## Deliverables
- `docs/reviews/phase40-review-b.md`
- `docs/roadmap.md`
- `README.md`
- `CHANGELOG.md`
- `docs/README.md`

## Commit sequence
1. `docs(phase40): add review-b plan and canonical review artefact`
2. `docs(roadmap): lock phases 41-50 and outline 51-60 with phase40 deltas`
3. `docs(status): align readme, changelog, and docs index with phase40 review`

## Final validation
```bash
git diff --name-only main...HEAD | rg -v '\.md$' && echo "ERROR: non-doc files changed" && exit 1 || true
rg -n "<<<<<""<<|====""===|>>>>"">>>" -S . && echo "ERROR: conflict markers" && exit 1 || true
rg -n "Phase 40|41|51|Wearables" README.md CHANGELOG.md docs/roadmap.md docs/reviews/phase40-review-b.md docs/README.md
```

## PR commands
```bash
git push -u origin phase-40-review-b
gh pr create --base main --head phase-40-review-b --title "docs(phase40): review B roadmap lock 41-50 and outline 51-60" --body "Phase 40 Review B docs-only update: canonical review artefact, rolling roadmap refresh, README/CHANGELOG/docs index alignment, and docs-only guards."
gh pr checks --watch
gh pr merge --merge --delete-branch=false
```

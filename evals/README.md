# Seeded-bug evals (ADR-0004)

Each `seeded/<id>/case.json` is a real defect the agent once let through. `scripts/sdlc/evolve.mjs` runs the code reviewer
(`worker-reviewer`) on the case's diff with the `.claude/` rules of a given git ref and grades the verdict block:
**caught** = a `[B]` finding on one of `expect.files` whose text matches one of `expect.patterns` (case-insensitive).

```bash
node scripts/sdlc/evolve.mjs eval --case <id> --rules main --runs 3          # how today's rules do on one case
node scripts/sdlc/evolve.mjs gate --base main --head evolve/<x> --case <id>  # the Evolve gate (ADR-0004 D3)
```

| Field | Meaning |
|-------|---------|
| `signal` | The agent-metrics signal that produced the case (`defect:<fix sha>`); Evolve skips signals that already have a case |
| `run` | The autopilot run that introduced the defect |
| `base`, `head` | Commit range of the defect; the reviewer sees `git diff base head` with the tree checked out at `head` |
| `fix` | The commit that fixed it later |
| `expect.files` | Path suffixes where a good review puts the blocking finding |
| `expect.patterns` | Regexes for ways of describing this defect |

Rules: cases are added, never edited or deleted (the workflow rejects it). Each eval run costs about $0.15 (Sonnet).

| Case | From | Rules at creation | With the Evolve rule |
|------|------|-------------------|----------------------|

Add one row per case Evolve adds. The empty table is expected on a fresh install: cases come from this repo's own escaped defects.

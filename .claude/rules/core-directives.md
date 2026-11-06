# Core Directives

How work is done in this repository. Other rules and skills cite these by number.

## Principles

1. **Understand first.** Search for what exists before adding anything. Check library APIs in current docs rather than memory. Re-read a file before you edit it if it may have changed.
2. **Prove it works.** Write or update tests with the change. A bug fix comes with a test that fails without the fix. Run the gates (`code-quality.md`) before each commit.
3. **Keep it safe.** `security.md` wins over every other rule. Flag a vulnerability the moment you see it.
4. **Keep it simple.** Solve the problem in front of you. No speculative abstraction, no dead code left behind.
5. **One source of truth.** Look in `.claude/skills/` before writing an ad-hoc procedure, and link to a rule instead of restating it.
6. **Ship it, with consent.** Work counts as done once it is pushed and CI is green. Every `git push` needs the owner's go-ahead for that push; an earlier yes does not carry over. The only exceptions are the opt-in autopilot gates (ADR-0003, ADR-0005).
7. **Leave a trail.** Decisions go in ADRs, status in the tracking files named in `sdlc.config.json`, scratch work in `./scratchpad/`.

## Planning Flow

Idea → PR-FAQ → PRD → ADR → spec (+ design spec for UI) → sequence diagrams → plan → code.

Match the ceremony to the change:

| Change | Artifacts |
|--------|-----------|
| Trivial (typo, config value) | none |
| Small fix or feature, one sentence to describe | a plan at most |
| Feature with user-facing scope | PRD, sequence diagrams, plan |
| Large or cross-team | PR-FAQ first, then the full chain |

An ADR depends on the kind of decision, not the size: write one for anything hard to undo (data model, public API, a new dependency or service, security model). `swarm-plan` holds the detailed sizing table.

## Where Things Go

| Artifact | Path |
|----------|------|
| ADR | `docs/adr/NNNN-<topic>.md` |
| PR-FAQ, PRD | `docs/prd/PR-FAQ-<feature>.md`, `docs/prd/PRD-<feature>.md` |
| Spec, design spec | `docs/specs/spec-<feature>.md`, `docs/specs/design-spec-<component>.md` |
| Sequence diagrams | `docs/sequence/sequence-<feature>.md` |
| Plan, roadmap | `docs/plans/plan-<feature>.md`, `docs/plans/roadmap-<name>.md` |
| Reports, postmortems | `docs/reports/<kind>-<YYYY-MM-DD>[-<topic>].md` |
| Throwaway notes | `./scratchpad/` (git-ignored) |

Templates live in `templates/artifacts/`. If a skill names a different path, this table wins.

## When Rules Disagree

Security → `tech-strategy.md` → this file → a skill's own conventions → your judgment. A higher source wins; your judgment fills gaps only.

## Corrections

When the owner corrects you and the correction contradicts a written rule or skill, add a line to `scratchpad/corrections.log`: `YYYY-MM-DD | what they said | file it contradicts`. Ordinary task instructions are not corrections. `land-the-plane` reviews the log and proposes a durable fix.

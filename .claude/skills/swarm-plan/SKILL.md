---
name: swarm-plan
description: Turns a feature, fix or spec into a task plan with acceptance criteria and file:line facts, using parallel explorer workers to map the code first. Use before implementing anything bigger than a trivial change.
argument-hint: "<request, issue, or path to a PRD/spec>"
disable-model-invocation: false
---

# Swarm Plan

Input: `$ARGUMENTS`. Output: `docs/plans/plan-<slug>.md` from `templates/artifacts/plan.template.md`, plus a section in the tracking file (`tracking.tasks` in `sdlc.config.json`, default `TASKS.md`).

## Artifact Requirements

Size the request first; it decides which documents must exist before the plan.

| Size | Signs | Needs before the plan |
|------|-------|-----------------------|
| Trivial | One file, obvious change, no behavior question | Nothing; skip the plan too |
| Small | Describable in one sentence, < ~100 changed lines, one component | Nothing |
| Medium | New user-visible capability, several components, or a new endpoint | PRD (`/scope`), spec + sequence diagrams (`/spec`) |
| Large | Several teams or services, new product area, migration of data | PR-FAQ, then PRD, spec, sequence diagrams |

Independent of size, a decision that is hard to reverse needs an ADR (`architecture/writing-adrs`): data model, public API shape, a new dependency or service, auth or security model, a change to `tech-strategy.md`.

When a required document is missing, stop and name the command that makes it. Do not plan around a gap.

## Steps

1. **Map.** Start two to four `worker-explorer` workers in parallel, each with one question: where the change lands, what calls it, existing patterns to copy, tests that cover it today. Each returns `path:line` evidence.
2. **Facts.** Write what the code does now, every line backed by `path:line` from the workers or your own reading. No fact without a location.
3. **Tasks.** Split the work into tasks a builder can finish in one sitting:
   - each has the files it touches, acceptance criteria a test can check, and its dependencies;
   - tests are part of the task that adds the behavior, not a separate task at the end;
   - order tasks so the tree builds and tests pass after each one.
4. **Risks.** What could break, and the test or check that would notice.
5. **Write** the plan file and the tracking section (`[ ]` per task with its ID). Set the plan's Status to Draft.
6. **Self-check** before handing over: every task has acceptance criteria; every fact has `path:line`; the scope matches the request and nothing more; nothing contradicts `tech-strategy.md` or `security.md`.

## Hand-off

Reply with the plan path, the task table and any open question. Implementation goes to `/swarm-execute` (or `/autopilot` for small work).

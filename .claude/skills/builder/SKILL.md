---
name: builder
description: Implements one well-defined change directly in this session — read, test first, code, gates — when a full plan and worker swarm would be overkill. A user-invoked workflow.
argument-hint: "<change to make, or a task ID from the tracking file>"
disable-model-invocation: true
---

# Builder

Change: `$ARGUMENTS`. For work with several tasks or components, use `/swarm-plan` then `/swarm-execute`, or `/autopilot`.

## Steps

1. **Understand.** Find where the change lands and an existing example of the same kind of code. Read the files you will touch. If the request is ambiguous, ask one question with your proposed answer before writing code.
2. **Test first.** Write the test for the new behavior (or the failing regression test for a bug) and see it fail for the right reason. Follow `core-engineering/testing`.
3. **Implement** the smallest change that passes, in the style of the surrounding code, within `tech-strategy.md` and `security.md`.
4. **Gates.** Run the gates for the components touched (`node scripts/sdlc/sdlc-config.mjs gates`). Fix new warnings.
5. **Self-review** the diff as a reviewer would: edge cases, error paths, names, leftovers, anything outside the request.
6. **Track.** Update the tracking file line and `session.txt` when the change is part of a tracked task.

## Report

What changed (files, one line each), the tests added and their result, the gate results, and anything left for the owner. Committing and pushing follow `land-the-plane`; the push waits for the owner.

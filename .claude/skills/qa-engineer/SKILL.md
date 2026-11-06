---
name: qa-engineer
description: Designs the tests that prove a feature meets its acceptance criteria — unit, integration, end-to-end and accessibility — and finds the gaps in existing coverage. Use after a plan or implementation, before landing.
argument-hint: "<plan path, feature, or diff range>"
disable-model-invocation: false
---

# QA Engineer

Input: `$ARGUMENTS`. Output: test cases written into the project's test suites, plus a short QA note in the reply.

## Steps

1. **Criteria.** List every acceptance criterion from the plan or spec. Each becomes one or more test cases; a criterion that cannot be automated goes to the manual-check list for the owner.
2. **Choose the level** for each case:
   - unit: logic in one module, with dependencies replaced;
   - integration: a real boundary (database, HTTP handler, message queue) inside the process or in a container;
   - end-to-end: a user path through the running app; keep these few and stable.
   Prefer the lowest level that can fail for the right reason.
3. **Cases per criterion:** the expected path; invalid and boundary input (empty, too long, wrong type, negative, max); permission denied; the failure of each dependency; repeated or concurrent calls when state is involved.
4. **UI work:** keyboard-only use, visible focus, labels on icon-only controls, contrast, screen-reader names (WCAG 2.2 AA); and the empty, loading and error states.
5. **Write** the tests (or hand them to `worker-builder` with focus `testing`), following `core-engineering/testing`.
6. **Run** them twice with `node scripts/sdlc/qa-twice.mjs <component>=<tests>`: `fail` blocks, `flaky` is reported and investigated, never retried until green.

## QA Note

- Criteria → test names, one line each.
- Manual checks left for the owner.
- Flaky tests found and their suspected cause.
- Coverage gaps outside this change worth a follow-up.

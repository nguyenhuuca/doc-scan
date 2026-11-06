---
name: scope
description: Turns a rough idea into a PRD (and a PR-FAQ first for large bets) by asking the questions that matter, sizing it, and writing the documents. A user-invoked product workflow.
argument-hint: "<idea in a sentence or two>"
disable-model-invocation: true
---

# Scope

Idea: `$ARGUMENTS`. Output: `docs/prd/PRD-<feature>.md` (and `docs/prd/PR-FAQ-<feature>.md` when large).

## 1. Understand

Read what exists: related code (`worker-explorer`), earlier PRDs and ADRs, open issues. Then ask the owner only what you cannot find, at most five questions in one round, each with your suggested answer:

- Who is it for, and what do they do today instead?
- What outcome shows it worked (a number, a behavior)?
- What is explicitly out of scope?
- Constraints: deadline, compliance, platforms, budget.
- Must it work with something that already exists?

## 2. Size

Use `swarm-plan`'s Artifact Requirements. Small → no PRD; say so and point to `/autopilot` or `/swarm-plan`. Medium → PRD. Large → PR-FAQ first, reviewed by the owner, then the PRD.

## 3. Write

From `templates/artifacts/prd.template.md` (and `pr_faq.template.md`):

- the problem in the user's terms, with the evidence you found;
- goals with measures and targets; non-goals;
- requirements numbered R1, R2… each with a Given/When/Then acceptance line;
- risks with mitigations; open questions with an owner.

Mark guesses as assumptions. No requirement without acceptance.

## 4. Hand-off

Status Draft. Show the owner the goals, the requirements table and the open questions. Once approved, the next step is `/spec` (spec + sequence diagrams), and an ADR for any hard-to-reverse choice the PRD implies.

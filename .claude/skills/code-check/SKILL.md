---
name: code-check
description: A quick quality pass over a path or the current diff — gates, obvious bugs, security smells, dead code and rule violations — without a full multi-reviewer review. A user-invoked check.
argument-hint: "[path | diff range]"
disable-model-invocation: true
---

# Code Check

Target: `$ARGUMENTS`, or the uncommitted diff when empty. Read-only: report, do not fix.

## Steps

1. **Gates.** Run the gates for the components the target touches (`node scripts/sdlc/sdlc-config.mjs gates`). Record each as pass, fail with the first error, or not run with the reason.
2. **Scan** the target files for:
   - bugs: unhandled errors, null or empty input, off-by-one, resources not closed, async work not awaited;
   - security: input reaching a query, shell, path or HTML; secrets; missing authorization checks (`.claude/rules/security.md`);
   - tests: new branches with no test;
   - code health: dead code, duplicated business logic, functions doing several jobs, names that mislead;
   - rules: anything against `tech-strategy.md` or `code-quality.md`.
3. **Rank** each finding Critical, High, Medium or Low (`swarm-review` severity table).

## Report

A table of gates, then findings by severity with `file:line`, the problem and a suggested fix. End with one line: ready to commit, or what must be fixed first. For a deeper review use `/swarm-review`.

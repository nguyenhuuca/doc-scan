---
name: swarm-review
description: Reviews a diff, branch, PR or plan from several angles at once (correctness, security, tests, architecture), traces each real problem to its root cause, and ends with a verdict block. Use before merging or after a plan is drafted.
argument-hint: "[diff range | PR number | plan path] [--focus security,quality,tests,architecture]"
disable-model-invocation: false
---

# Swarm Review

Target: `$ARGUMENTS`; with nothing given, the uncommitted diff (`git diff` plus untracked files).

## Steps

1. **Collect.** Resolve the target to a file list and a diff. For a plan, the plan file and the code it cites.
2. **Fan out.** Start `worker-reviewer` workers in parallel, one per angle, each with the same target and one focus:
   - `quality`: correctness, edge cases, error handling, design;
   - `security`: `.claude/rules/security.md`, input to sinks, auth checks, secrets;
   - `tests`: missing, weak or flaky tests, untested branches;
   - `architecture`: boundaries, `tech-strategy.md`, consistency with nearby code (only for changes over ~200 lines or across components).
   Small changes may use one reviewer covering quality, security and tests.
3. **Merge.** Combine the findings, drop duplicates, and keep the higher severity when two reviewers rate the same issue differently. Discard a finding that does not hold up when you read the cited line yourself.
4. **Root cause** for each Critical or High finding (below).
5. **Verdict.** End with the block from `.claude/skills/autopilot/resources/verdict.md`.

## Severity

| Severity | Examples | Block |
|----------|----------|-------|
| Critical | Exploitable injection, auth bypass, data loss, secret committed | `[B]` |
| High | Broken requirement, unhandled failure path that loses work, new logic without tests, violated rule or ADR, breaking change without migration | `[B]` |
| Medium | Performance risk, unclear design, missing edge-case test | `[A]` |
| Low | Naming, small cleanups, style a linter does not catch | `[A]` |

Plans: a task without acceptance criteria, a claim without `file:line`, or scope beyond the request is High.

## Root Cause Analysis

Ask "why" until you reach a cause in the process, not only in the code (often five times):

1. Why does the defect exist? (the code does X)
2. Why was it written that way? (the author assumed Y)
3. Why was Y assumed? (nothing in the spec or code said otherwise)
4. Why did nothing catch it? (no test for the case; the reviewer had no check for it)
5. Which stage should have caught it, and what one change to that stage would? (plan review, code review, a test rule, a CI check or hook)

Write the chain under the finding. The last answer is a candidate rule or check; `/evolve` uses the same chain for escaped defects.

## Output

Findings grouped by severity, each with `file:line`, the issue, why it matters and the fix; root-cause chains under Critical and High; then the verdict block. Never print a secret you found, only where it is.

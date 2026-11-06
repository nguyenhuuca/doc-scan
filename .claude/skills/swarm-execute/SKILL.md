---
name: swarm-execute
description: Implements an approved plan task by task with builder workers, running tests and gates after each task and reviewing before commit. Use when a plan in docs/plans/ is ready to build.
argument-hint: "<docs/plans/plan-*.md> [task IDs]"
disable-model-invocation: false
---

# Swarm Execute

Input: a plan path and optionally the task IDs to run. Without IDs, every open task (`[ ]` in the tracking file) in plan order.

## Before Starting

- Read the plan and its tracking section. Stop if the plan is not approved, a task is `[~]` (someone else has it), or a task has no acceptance criteria.
- Write `session.txt` (`.claude/rules/session-file.md`): task, plan path, the task list in `next`.

## Per Task

1. Mark it `[~]` in the tracking file.
2. **Build.** Give one `worker-builder` (focus `implementation`) the task row, its acceptance criteria, the files and the relevant plan facts. Tasks with no dependency on each other and no shared files may run in parallel, each worker in its own worktree.
3. **Check.** Run the task's tests and the gates of the components it touched (`node scripts/sdlc/sdlc-config.mjs gates`). Red → back to the builder with the failure, at most three times, then stop and report.
4. **Mark** `[x]` once green; update `session.txt` (`done`, `progress`, `next`).

## After All Tasks

1. **Review** the whole diff with `swarm-review`. Blocking findings go back to a builder; re-review after the fix. Three rounds without approval: stop and report.
2. **Commit.** One commit per task or one per plan, as the plan says; explicit pathspecs, never `git add -A` on a tree with unrelated changes. Put the commit hash on each tracking line.
3. **Push.** Show the commits, gate results and review verdict, and ask the owner before pushing. After a yes: `git pull --rebase`, push, watch CI, run the `smoke` command if one is configured. An earlier yes does not cover this push.
4. Set the plan's Status to Done when every task is `[x]`, and update the status file (`tracking.status`).

## Rules

- The orchestrator alone starts workers, edits tracking files and commits.
- A worker that hits a question outside its task stops and reports; the orchestrator decides or asks the owner.
- Never weaken or delete a test to make a gate pass.

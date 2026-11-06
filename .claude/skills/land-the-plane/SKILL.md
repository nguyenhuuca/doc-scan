---
name: land-the-plane
description: Finishes in-flight work safely — gates, a clean commit, the owner's push approval, push, CI and smoke checks, tracking updates, and a short retro. Use when work is ready to ship or a session is ending.
argument-hint: "[--mode A|B]"
disable-model-invocation: false
---

# Land the Plane

Work is done when it is pushed, CI is green and the next person (or session) can see where things stand.

## Modes

- **Mode A** (default, a person is at the keyboard): all steps below; the push waits for the owner's answer.
- **Mode B** (inside `/autopilot`): steps 1–3 and 6–7 run during the Land stage; steps 4–5 are autopilot's Gate 2 and its CI step, under the rules in that skill.

## Steps

1. **Inventory.** `git status` and `git diff --stat`. Separate the change from unrelated edits; the unrelated ones stay out of the commit.
2. **Gates.** Run the gates of every component touched (`git diff --name-only <base> | node scripts/sdlc/sdlc-config.mjs gates`). A red gate stops the landing; fix it or report it. A gate that could not run is reported as not run.
3. **Commit.** Explicit pathspecs. Message: a conventional type and scope, one subject line under ~72 characters, a body that says why. Add the tracking updates (hash on each `[x]` line, plan status, `session.txt`) to the same commit or a `chore(session)` commit.
4. **Push, with consent.** Show the commits and gate results and ask the owner. On a yes: `git pull --rebase`, resolve conflicts by keeping both sides' intent, `git push`, then confirm `git status` says in sync. Never force-push.
5. **Verify.** Watch the CI runs for the pushed commit to the end. Run the `smoke` command from `sdlc.config.json` after the deploy when one is set. A red run is reported with its failing step; a fix is a new commit, again with consent.
6. **Hand-off.** Update `session.txt`: `status: done` and `progress: 100` when the task is finished, otherwise the precise `next`. Note follow-ups in the tracking file.
7. **Retro.** Read `scratchpad/corrections.log` if it exists. For each correction from this session, propose where it belongs (a rule line, a skill step, a hook or a CI check) and ask the owner before writing it. Keep it to what happened, not general advice.

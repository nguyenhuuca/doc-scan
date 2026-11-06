---
name: welcome
description: First-session onboarding after agentic-sdlc is installed — shows the stack the installer detected, finds what it could not, asks the owner to confirm or choose, and writes the tech strategy (the project's golden path). The owner may skip. Also runs on demand to revisit the tech strategy.
argument-hint: "[--recheck]"
disable-model-invocation: false
---

# Welcome

`.claude/rules/tech-strategy.md` is the golden path every rule, skill and worker follows, so the owner confirms it once instead of trusting a guess. The session-start hook offers this skill while the file holds `<!-- sdlc:status unconfirmed -->`. With `--recheck`, run it on a confirmed file too.

## 0. Greet and Offer

**Interactive sessions only.** When `AskUserQuestion` is not available (a headless `claude -p` run, CI), do not onboard and never hold up the task: do the task, and end the reply with one line saying the tech strategy is unconfirmed and `/welcome` checks it.

Greet the owner in one or two lines (their language): agentic-sdlc is installed, and the tech strategy needs a two-minute check. If their first message was a task, say you will come back to it right after, or now if they skip.

Ask with `AskUserQuestion` (single question): **Check it now (Recommended) / Skip for now / Don't ask again**.
- **Skip for now:** change nothing; the next session offers it again. Go to their task.
- **Don't ask again:** set the marker to `<!-- sdlc:status skipped YYYY-MM-DD -->`, tell them `/welcome` is there any time, and go to their task.

## 1. Collect

Read `.claude/rules/tech-strategy.md` (the rows the installer filled from its stack packs) and `sdlc.config.json`. Then look for what build files do not say, with `Glob`/`Grep` or one `worker-explorer`:

| Area | Where to look |
|------|---------------|
| Database, cache, queue | driver and client dependencies, `docker-compose*.yml`, connection settings |
| Migrations | `migrations/`, `db/changelog/`, Flyway, Liquibase, Alembic, Prisma, Knex folders |
| API style | REST controllers, GraphQL schema, gRPC `.proto` |
| Auth | security config, JWT or OAuth libraries, session middleware |
| Lint and format | ESLint, Prettier, Biome, Ruff, Black, Checkstyle, Spotless, golangci-lint configs |
| Tests beyond unit | Testcontainers, Playwright, Cypress, contract tests |
| Build and deploy | `Dockerfile`, Helm or Kubernetes manifests, Terraform, `.github/workflows/` |
| Observability | logging library, metrics, tracing (OpenTelemetry) |

Every claim needs a file you saw. Mark each area **detected** (with evidence), **guessed** (indirect sign) or **unknown**.

## 2. Show

One table: area · what was found · evidence (`path:line`) · status. Then the gates from `sdlc.config.json`, one line per component, and `smoke` (or "not set").

## 3. Ask

Use `AskUserQuestion`, up to four questions per call, only for what is not plainly detected:

- **Guessed or unknown areas**, most important first: database, migrations, test strategy, deployment. Options: your suggestion first, labelled "(Recommended)", that fits what is detected (for example Java + Spring → PostgreSQL with Flyway or Liquibase, JUnit 5 with Testcontainers; Node + React → Vitest with Testing Library; Python → pytest with Ruff), one or two real alternatives, and **"Not decided yet"**. The owner can always type their own answer.
- **Detected areas**: one question to confirm them all, with "Something is wrong" to correct.
- **Gates**: confirm the commands, or ask for the right ones; offer to set a `smoke` command when a deployed app exists.
- **Not allowed**: offer two or three rules that protect the choices above (for example "no schema changes outside migrations", "no new HTTP client besides the one in use"). Multi-select.

An owner who does not know an answer picks "Not decided yet": write `_Not decided_` in that row and move on. Never block on a question.

## 4. Write

- Fill the `sdlc:stack` table (Area · Choice, with versions when known) and the `sdlc:prohibited` list. Keep text outside the markers.
- Set the marker to `<!-- sdlc:status confirmed YYYY-MM-DD -->`.
- Apply gate or smoke changes to `sdlc.config.json`; run each changed gate once and report pass or fail.
- Show the diff and suggest a commit (`chore: confirm tech strategy`). Committing follows the usual rules; pushing waits for the owner.

Then return to the owner's first task, if there was one.

## Rules

- Suggest, never decide: the owner's answer goes in even when you suggested otherwise. If an answer conflicts with what the code uses, say so once.
- Rows only for what the project uses or has chosen; no aspirational stack.
- Keep `tech-strategy.md` short: it loads into every session.

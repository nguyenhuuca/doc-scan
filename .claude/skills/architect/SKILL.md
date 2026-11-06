---
name: architect
description: Works through a system design question with the owner — requirements, options, trade-offs — and records the outcome as an ADR or a system design document. A user-invoked workflow.
argument-hint: "<design question or component>"
disable-model-invocation: true
---

# Architect

Question: `$ARGUMENTS`.

## Steps

1. **Requirements.** Functional needs, expected load and growth, latency and availability targets, data sensitivity, team and budget limits. Ask the owner for any that the docs and code do not answer.
2. **Current state.** Map the components involved and their dependencies (`worker-explorer`), with `path:line` and links to existing ADRs and `docs/architecture/`.
3. **Options.** Two or three designs that meet the requirements. For each: a Mermaid diagram, how it handles the load and failure cases, operating cost, security boundaries, migration path from today, and how hard it is to back out.
4. **Recommend** one, with the deciding trade-off stated plainly. Stay within `tech-strategy.md` unless the owner agrees to change it, which itself needs an ADR.
5. **Record.**
   - A decision → ADR via `architecture/writing-adrs` (or hand it to `worker-architect` for a deep trade-off).
   - A component's overall shape → `docs/architecture/system-design-<component>.md` from `templates/artifacts/system_design.template.md`.
6. **Next steps.** The tasks this implies, ready for `/swarm-plan`.

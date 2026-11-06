---
name: writing-adrs
description: When a decision needs an Architecture Decision Record and how to write one — context, real options, the choice and its consequences. Use when a plan or review hits a hard-to-reverse choice.
---

# Writing ADRs

## When One Is Needed

Write an ADR when the decision is expensive to undo or shapes later work, whatever the size of the change:

- a data model or schema that other code will depend on;
- the shape of a public or cross-team API;
- adding a dependency, service, datastore or vendor;
- authentication, authorization, encryption or data retention;
- anything that changes `tech-strategy.md` or a prohibited pattern;
- a deliberate exception to a rule.

Not needed: a choice local to one module that a later refactor can change cheaply.

## How

1. Next number: highest `docs/adr/NNNN-*.md` plus one. File `docs/adr/NNNN-<topic>.md` from `templates/artifacts/adr.template.md`.
2. **Context**: the forces, in facts with `path:line` and links to earlier ADRs. Someone who was not there should understand why this came up.
3. **Options**: two or more that were really considered. For each: how it would work here, cost, risk, and reversibility. Leave out straw men.
4. **Decision**: the choice and the deciding reason, and what is being given up.
5. **Consequences**: what changes in code, operations, security and tests; follow-up tasks.
6. Status `Proposed` until the owner accepts. Accepted ADRs are not rewritten: a later change gets a changelog line, a larger one a new ADR that supersedes it.

Hard architectural trade-offs can go to `worker-architect`.

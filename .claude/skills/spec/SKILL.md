---
name: spec
description: Turns an approved PRD (and its ADRs) into a technical spec, a design spec for UI work, and Mermaid sequence diagrams, ready for planning. A user-invoked workflow.
argument-hint: "<docs/prd/PRD-*.md>"
disable-model-invocation: true
---

# Spec

Input: an approved PRD. Output, with the same `<feature>` slug as the PRD:

- `docs/specs/spec-<feature>.md` (`templates/artifacts/spec.template.md`);
- `docs/specs/design-spec-<component>.md` for each new or changed screen (`design_spec.template.md`);
- `docs/sequence/sequence-<feature>.md` (`sequence.template.md`): required for every feature with a PRD.

## Steps

1. **Read** the PRD, its ADRs, and the code that the feature touches (`worker-explorer` for locations). Stop if the PRD is not approved, or if a decision it needs has no ADR; name what is missing.
2. **Spec.** Data model and migrations; every interface with input, output, errors and auth; behavior rules numbered B1, B2… and mapped to the PRD's R-numbers; limits and non-functional targets; rollout and rollback.
3. **Design spec** for UI: layout per breakpoint, every state (empty, loading, error, populated), interactions, copy with translation keys, accessibility.
4. **Sequence diagrams.** One diagram for the main path and one per important alternative or failure (auth failure, validation error, a dependency down). Participants are real components named as in the code. Each arrow shows the call and its key data; responses show status.
5. **Trace.** A table R → B → diagram, so every requirement is covered and every behavior has a reason.
6. **Review** with `swarm-review` on the spec files; fix blocking findings.

## Hand-off

Status Draft until the owner approves. Then `/swarm-plan <spec path>`.

# Code Quality

## Gates Before Every Commit

Run the `gates` of each component you touched in `sdlc.config.json`:

```bash
git diff --name-only HEAD | node scripts/sdlc/sdlc-config.mjs gates
```

Each printed line is a command; all must pass. A gate you cannot run (missing tool, no network) is reported as not run, never as passed. Fix new warnings rather than silencing them.

## Design

- One reason to change per module. If a description needs "and", consider splitting it.
- Depend on interfaces at boundaries (storage, network, clock) so tests can swap them.
- Duplication of a business rule must be removed; duplication of a few similar lines may stay until a third copy appears.
- Prefer clear names over comments; comment the *why* that the code cannot show.
- Use the type system: strict mode, no `any`-style escapes, narrow unions over flags.

## Two Hats

Refactoring changes structure and leaves behavior alone: the existing tests pass untouched. Optimizing changes speed and needs a measurement before and after. Do not mix either with a feature in one commit.

## Performance Checks for Review

- A query or remote call inside a loop.
- Blocking I/O on a request or event-loop path.
- Unbounded result sets with no paging.
- Quadratic work over input that can grow.
- The same expensive value computed twice on a hot path.

## Logging

Use the project's structured logger, not print statements. Log errors with enough context to act, include a request or correlation id, and never log secrets, tokens or personal data.

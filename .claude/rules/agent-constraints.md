# Agent Constraints

Rules for the orchestrator and the workers in `.claude/agents/`.

- **Focused prompts.** A worker gets the task, the files or paths it needs and the expected output. Never paste the whole conversation or codebase into it.
- **One coordinator.** The orchestrator owns the task list and any shared state. Workers report back; they do not edit each other's work or the tracking files.
- **No nesting.** Workers do not start workers. Each worker's `tools` list leaves out `Agent`, which is what enforces it.
- **Bounded runs.** Every worker sets `maxTurns` in its frontmatter.
- **Model by job.** The model of each worker is set in its frontmatter and nowhere else: reading and search on the cheapest model, code and review on the middle one, architecture only on the strongest.
- **Summaries, not dumps.** A worker returns what changed, what it found and its verdict, not file contents the orchestrator can read itself.
- **Pushing is never a worker's call.** See `core-directives.md` principle 6.

# Debugging Protocol

## Hypotheses Before Edits

Before changing code to fix a failure, write down three candidate causes. Each must name something you can check: a log line, a value at a breakpoint, a request, a commit that changed behavior. Check the cheapest one first. If a change does not fix it, go back to the list instead of improvising.

## Fix the Cause

A fix is done when you can explain the chain from cause to symptom. Retries, swallowed exceptions, wider types and extra null checks may buy time, but they are labelled as mitigation, not presented as the fix. The regression test should fail on the old code; if you cannot make it fail, the cause is not isolated yet.

## Stale Context Check

Memory of a file decays during a long session.

- Re-read a file before editing it when about twenty tool calls have passed since the last read, or after the context was compacted.
- After a compaction or a resume, re-read the plan and the task list as well, not only the next file.
- Put any plan longer than a few steps in a file before starting, so there is something to re-read.
- Hand wide searches to a worker, so the orchestrator's context stays small.

## Warning Signs

Stop and rethink when you notice: a fix that works for reasons you cannot state; a second speculative change stacked on an unverified first one; editing from recollection; an error being silenced.

## Escalate After Three

Three failed fix attempts on one problem means stop. Write down what you tried, what each attempt showed and which causes are ruled out, then ask the owner or open a task with those notes.

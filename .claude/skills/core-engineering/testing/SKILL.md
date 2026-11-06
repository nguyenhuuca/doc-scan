---
name: testing
description: How to write tests in this repository — test-first for logic, a failing regression test for every bug fix, deterministic tests, and the right level for each check. Use whenever adding or changing tests.
---

# Testing

## Order of Work

1. Write the test for the behavior you are about to add, and watch it fail for the expected reason.
2. Write the smallest code that makes it pass.
3. Clean up with the test green.

For a bug: reproduce it in a test first. The test must fail on the current code; then fix. If you cannot make it fail, you have not found the cause yet (`debugging-protocol.md`).

## What a Good Test Looks Like

- One behavior per test, named after it: `rejects_expired_token`, not `test2`.
- Arrange, act, assert, in that order and visibly separated.
- Asserts on outcomes the user or caller sees, not on private details or call counts, unless the call is the behavior (sending an email, charging a card).
- Independent: no order between tests, no shared mutable state, each cleans up what it creates.
- Deterministic: inject the clock, random source and ids; no real network; no sleeps, wait on a condition instead.
- Fast: unit tests in milliseconds. Slow tests are tagged and run in their own job.

## Doubles

Replace what is slow, flaky or external (network, payment, time). Keep real what is cheap and part of the behavior (pure functions, in-memory structures, the database in an integration test when the project runs one in a container). A test that mocks the thing it is testing proves nothing.

## Coverage

Use coverage to find untested branches, not as the goal. Never lower a threshold to pass a gate; add the missing tests or explain the exception to the owner.

## Flaky Tests

A test that fails one run in many is a bug. Run suspects twice with `node scripts/sdlc/qa-twice.mjs`, find the cause (time, order, shared state, async without waiting), and fix it. Do not retry until green, skip it silently or delete it.

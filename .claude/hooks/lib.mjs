// Shared helpers for the hooks in this folder. Claude Code passes the event as JSON on stdin and reads JSON on stdout.
// A hook that cannot read its input exits 0 without output (fails open): hooks are guardrails, not the security
// boundary; permissions.deny and CI are (.claude/rules/security.md, "What Enforces a Rule").
import { readFileSync } from 'node:fs'

export function readEvent(raw) {
  try { return JSON.parse(raw ?? readFileSync(0, 'utf8')) } catch { return null }
}

// decision: 'deny' | 'ask'. The reason is shown to the user (ask) or to the model (deny).
export function preToolUse(decision, reason) {
  return { hookSpecificOutput: { hookEventName: 'PreToolUse', permissionDecision: decision, permissionDecisionReason: reason } }
}

// Writes the hook's JSON reply (if any) and ends with exit 0, the "no error" path Claude Code expects with JSON.
export function reply(out) {
  if (out) process.stdout.write(JSON.stringify(out))
  process.exit(0)
}

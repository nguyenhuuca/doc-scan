#!/usr/bin/env node
// PreToolUse (Bash): denies commands that destroy data outside the repo or rewrite shared history, and asks before
// risky ones. permissions.deny in settings.json blocks the plain forms; this hook also catches them inside a chain,
// after sudo or behind an env assignment, where a prefix rule does not match.
import { fileURLToPath } from 'node:url'
import { preToolUse, readEvent, reply } from './lib.mjs'

// [decision, why, pattern]; the first match wins, so deny rules come first
export const RULES = [
  ['deny', 'deletes the filesystem root, the home directory or a parent of the repo',
    /\brm\s+(?:-[a-zA-Z]*[rR][a-zA-Z]*f?|-[a-zA-Z]*f[a-zA-Z]*[rR])[a-zA-Z]*\s+(?:--\s+)?(?:\/(?:\s|$|\*)|~\/?(?:\s|$)|\$HOME\/?(?:\s|$)|\.\.(?:\/|\s|$))/],
  ['deny', 'writes to a raw disk device', /\b(?:dd\b[^|;&]*\bof=\/dev\/(?:sd|nvme|disk|hd)|mkfs(?:\.\w+)?\s)/],
  ['deny', 'rewrites the history of a shared branch (force push)', /\bgit\s+push\b[^;&|]*\s(?:--force(?!-with-lease)\b|-f\b|\+[\w/.-]+)/],
  ['deny', 'a fork bomb', /:\(\)\s*\{\s*:\s*\|\s*:\s*&\s*\}\s*;\s*:/],
  ['ask', 'force push with lease still rewrites a remote branch', /\bgit\s+push\b[^;&|]*--force-with-lease\b/],
  ['ask', 'discards uncommitted work', /\bgit\s+(?:reset\s+--hard|clean\s+-[a-zA-Z]*f|checkout\s+--\s+\.|restore\s+(?:--staged\s+)?\.(?:\s|$)|stash\s+(?:drop|clear))/],
  ['ask', 'deletes files recursively', /\brm\s+-[a-zA-Z]*[rR]/],
  ['ask', 'runs a script downloaded from the network', /\b(?:curl|wget)\b[^|;&]*\|\s*(?:sudo\s+)?(?:ba|z|da)?sh\b/],
  ['ask', 'drops or empties database objects', /\b(?:drop\s+(?:database|schema|table)|truncate\s+table)\b/i],
  ['ask', 'opens permissions to everyone', /\bchmod\s+(?:-R\s+)?(?:0?777|a\+rwx)\b/],
  ['ask', 'runs with root privileges', /(?:^|[;&|]\s*)sudo\s/],
  ['ask', 'prints a secrets file', /\b(?:cat|less|more|head|tail|type|bat)\s+[^|;&]*(?:^|[\s/\\])\.env(?:\.(?!example|sample|template)[\w.-]+)?(?:\s|$)/],
  ['ask', 'deletes cloud or container resources', /\b(?:terraform\s+destroy|kubectl\s+delete\s+(?:ns|namespace)|docker\s+system\s+prune|helm\s+uninstall)\b/],
]

export function check(event) {
  if (event?.tool_name !== 'Bash') return null
  const cmd = String(event.tool_input?.command ?? '')
  for (const [decision, why, re] of RULES) {
    if (re.test(cmd)) {
      const reason = decision === 'deny'
        ? `Blocked: this command ${why}. Do it by hand outside Claude Code if it is really needed.`
        : `This command ${why}. Confirm before it runs.`
      return preToolUse(decision, reason)
    }
  }
  return null
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) reply(check(readEvent()))

#!/usr/bin/env node
// PreToolUse (Write, Edit, MultiEdit, NotebookEdit, Bash): asks before text that looks like a credential is written to a
// file or passed on a command line, and before any write to a .env file. Asks rather than denies: a test fixture or a
// documented example can be legitimate, and the person at the keyboard decides.
import { fileURLToPath } from 'node:url'
import { preToolUse, readEvent, reply } from './lib.mjs'

export const SECRET_PATTERNS = [
  ['AWS access key id', /\bAKIA[0-9A-Z]{16}\b/],
  ['GitHub token', /\b(?:gh[pousr]_[A-Za-z0-9]{36,}|github_pat_[A-Za-z0-9_]{60,})\b/],
  ['Anthropic API key', /\bsk-ant-[A-Za-z0-9_-]{20,}/],
  ['OpenAI-style API key', /\bsk-(?:proj-)?[A-Za-z0-9]{32,}\b/],
  ['Slack token', /\bxox[abprs]-[A-Za-z0-9-]{10,}/],
  ['Google API key', /\bAIza[0-9A-Za-z_-]{35}\b/],
  ['private key', /-----BEGIN (?:RSA |EC |OPENSSH |DSA |PGP )?PRIVATE KEY( BLOCK)?-----/],
  ['JSON web token', /\beyJ[A-Za-z0-9_-]{10,}\.eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}/],
]

// key = "value" where the value is long and not a placeholder or a reference to the environment
const ASSIGNED = /\b([A-Za-z_]*(?:password|passwd|secret|token|api[_-]?key|access[_-]?key|private[_-]?key)[A-Za-z_]*)\s*[:=]\s*["']([^"'\s]{8,})["']/i
const PLACEHOLDER = /^(?:\$\{?[\w.:-]+\}?|<[^>]+>|%[\w]+%|\{\{[^}]+\}\}|x{6,}|\*{6,}|changeme|change-me|example\w*|placeholder|dummy\w*|test\w*|fake\w*|your[_-]\w+|redacted)$/i

export function findSecret(text) {
  if (!text) return null
  for (const [label, re] of SECRET_PATTERNS) if (re.test(text)) return label
  const m = ASSIGNED.exec(text)
  if (m && !PLACEHOLDER.test(m[2])) return `a literal value for ${m[1]}`
  return null
}

export function contentOf(tool, input = {}) {
  switch (tool) {
    case 'Write': return { path: input.file_path, text: input.content }
    case 'Edit': return { path: input.file_path, text: input.new_string }
    case 'MultiEdit': return { path: input.file_path, text: (input.edits ?? []).map((e) => e.new_string).join('\n') }
    case 'NotebookEdit': return { path: input.notebook_path, text: input.new_source }
    case 'Bash': return { path: null, text: input.command }
    default: return null
  }
}

const isEnvFile = (p) => /(^|[\\/])\.env(\.[\w.-]+)?$/.test(p ?? '') && !/\.env\.(example|sample|template)$/.test(p)

export function check(event) {
  const c = contentOf(event?.tool_name, event?.tool_input)
  if (!c) return null
  if (isEnvFile(c.path)) return preToolUse('ask', `Writing ${c.path}: .env files hold secrets and must stay untracked. Confirm this is intended.`)
  const hit = findSecret(c.text)
  if (hit) {
    const where = c.path ? `the content for ${c.path}` : 'this command'
    return preToolUse('ask', `${where} contains what looks like ${hit}. Secrets belong in environment variables, never in files or chat. Allow only if it is a placeholder or a test fixture.`)
  }
  return null
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) reply(check(readEvent()))

#!/usr/bin/env node
// Stop: a non-blocking reminder when the turn ends with loose ends: commits not pushed (the push still needs the
// owner's yes), or an unfinished task whose session.txt was not updated during a session that changed files.
// It never blocks the stop, so it cannot loop.
import { execFileSync } from 'node:child_process'
import { existsSync, readFileSync, statSync } from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { readEvent, reply } from './lib.mjs'
import { parseSession } from './session-start.mjs'

const STALE_MIN = 30

const git = (root, args) => {
  try { return execFileSync('git', ['-C', root, ...args], { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim() } catch { return null }
}

// facts: { ahead, dirty, sessionStatus, sessionAgeMin }
export function reminders(f) {
  const out = []
  if (f.ahead > 0) out.push(`${f.ahead} local commit(s) not pushed yet. Ask the owner before pushing.`)
  if (f.dirty && f.sessionStatus && f.sessionStatus !== 'done' && f.sessionAgeMin > STALE_MIN) {
    out.push(`session.txt is ${Math.round(f.sessionAgeMin)} min old while files changed. Update progress/done/next (.claude/rules/session-file.md).`)
  }
  return out
}

export function facts(root, now = Date.now()) {
  const ahead = Number(git(root, ['rev-list', '--count', '@{upstream}..HEAD']) ?? 0) || 0
  const dirty = Boolean(git(root, ['status', '--porcelain', '--untracked-files=no', '--', '.', ':!.claude/settings.local.json']))
  const file = join(root, 'session.txt')
  const has = existsSync(file)
  return {
    ahead,
    dirty,
    sessionStatus: has ? parseSession(readFileSync(file, 'utf8')).status : null,
    sessionAgeMin: has ? (now - statSync(file).mtimeMs) / 60000 : 0,
  }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const event = readEvent()
  const root = process.env.CLAUDE_PROJECT_DIR || event?.cwd || process.cwd()
  const r = event ? reminders(facts(root)) : []
  reply(r.length ? { systemMessage: r.join('\n') } : null)
}

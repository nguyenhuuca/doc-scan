#!/usr/bin/env node
// Reads sdlc.config.json at the repo root, so scripts and skills share one place for the project's commands.
// Usage:
//   node scripts/sdlc/sdlc-config.mjs get <dotted.key>   print a value (JSON for objects, empty for missing)
//   node scripts/sdlc/sdlc-config.mjs protected          ERE that matches paths an unattended run may not change
//   node scripts/sdlc/sdlc-config.mjs uncounted          ERE of paths left out of the changed-lines limit
//   node scripts/sdlc/sdlc-config.mjs gates < paths      one gate command per line, for the components the paths touch
import { readFileSync, existsSync } from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

export const DEFAULTS = {
  deployBranch: 'main',
  maxLoc: 400,
  // Paths an unattended run (auto-push, auto-merge) may never change: agent config, CI, secrets
  protectedPaths: ['^\\.github/', '^\\.claude/', '^CLAUDE\\.md$', '^AGENTS\\.md$', '^scripts/sdlc/', '^sdlc\\.config\\.json$', '(^|/)\\.env'],
  // Not counted toward maxLoc
  uncountedPaths: ['(^|/)(test|tests|__tests__)/', '\\.(test|spec)\\.[cm]?[jt]sx?$', '^docs/', '^TASKS\\.md$', '^session\\.txt$'],
  components: [],
  smoke: null,
  buildWorkflow: null,
  tracking: { tasks: 'TASKS.md', status: 'docs/tracking.md' },
}

// SDLC_CONFIG_FILE: an unattended job points this at a copy taken from the deploy branch, so a PR cannot change
// its own limits (the helper itself is copied the same way, see autopilot.yml)
export function load(root = process.cwd(), file = process.env.SDLC_CONFIG_FILE || join(root, 'sdlc.config.json')) {
  const own = existsSync(file) ? JSON.parse(readFileSync(file, 'utf8')) : {}
  const cfg = { ...DEFAULTS, ...own, tracking: { ...DEFAULTS.tracking, ...own.tracking } }
  // Extra protected paths add to the defaults; they never replace them
  cfg.protectedPaths = [...DEFAULTS.protectedPaths, ...(own.extraProtectedPaths ?? [])]
  return cfg
}

export function get(cfg, key) {
  return key.split('.').reduce((v, k) => (v == null ? undefined : v[k]), cfg)
}

export const anyOf = (patterns) => patterns.map((p) => `(${p})`).join('|')

// components: [{ name, path: 'api/', gates: ['mvn -B verify', ...] }]; path '' or '.' matches every file
export function gatesFor(cfg, paths) {
  const touched = (c) => !c.path || c.path === '.' || paths.some((p) => p.startsWith(c.path))
  return cfg.components.filter(touched).flatMap((c) => c.gates ?? [])
}

function main([cmd, arg]) {
  const cfg = load()
  if (cmd === 'get') {
    const v = get(cfg, arg ?? '')
    if (v != null) console.log(typeof v === 'object' ? JSON.stringify(v) : String(v))
  } else if (cmd === 'protected') console.log(anyOf(cfg.protectedPaths))
  else if (cmd === 'uncounted') console.log(anyOf(cfg.uncountedPaths))
  else if (cmd === 'gates') {
    const paths = readFileSync(0, 'utf8').split(/\r?\n/).filter(Boolean)
    for (const g of gatesFor(cfg, paths)) console.log(g)
  } else {
    console.error('usage: sdlc-config.mjs get <key> | protected | uncounted | gates < paths')
    return 2
  }
  return 0
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) process.exit(main(process.argv.slice(2)))

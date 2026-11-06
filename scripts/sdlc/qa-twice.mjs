#!/usr/bin/env node
// Runs the given tests twice and labels each one pass / flaky / fail (used by /autopilot Local QA).
//   pass  = passed both runs
//   flaky = failed one run only  -> reported, does not block
//   fail  = failed both runs     -> blocks
//
// Usage: node scripts/sdlc/qa-twice.mjs <component>=<test>[,<test>...] [<component>=...]
//   The component's "qa" entry in sdlc.config.json says how to run tests and where the JUnit XML lands:
//   { "run": "cd api && mvn -q test -Dtest={tests}", "junit": "api/target/surefire-reports", "sep": "," }
//   {tests} becomes the list joined with "sep" (default " "). Any runner that writes JUnit XML works.
// Exit: 0 = no fail (flaky allowed), 1 = at least one fail, 2 = bad usage or a run that produced no results.
// Local only: no network calls besides what the tests themselves do.
import { execSync } from 'node:child_process'
import { existsSync, mkdirSync, readdirSync, readFileSync, rmSync, statSync, cpSync } from 'node:fs'
import { isAbsolute, join, normalize, sep as pathSep } from 'node:path'
import { fileURLToPath } from 'node:url'
import { load } from './sdlc-config.mjs'

const TEST_ID = /^[\w./#:*@-]+$/  // class names, file paths, globs; nothing a shell would expand into a command

export function parseArgs(argv) {
  const out = []
  for (const a of argv) {
    const m = a.match(/^([\w-]+)=(.+)$/)
    if (!m) throw new Error(`expected <component>=<tests>, got "${a}"`)
    const tests = m[2].split(',').filter(Boolean)
    const bad = tests.find((t) => !TEST_ID.test(t))
    if (bad) throw new Error(`test id "${bad}" has characters outside [A-Za-z0-9_./#:*@-]`)
    out.push({ component: m[1], tests })
  }
  return out
}

// A report path is removed before each run, so it must stay inside the repo
export function safeReportPath(p) {
  const n = normalize(p)
  if (!p || isAbsolute(n) || n === '.' || n.startsWith('..') || n.split(pathSep).includes('..')) {
    throw new Error(`qa.junit must be a relative path inside the repo, got "${p}"`)
  }
  return n
}

// <testcase> with a <failure> or <error> child failed; one with <skipped> is left out
export function junitResults(xml, prefix) {
  const results = new Map()
  for (const m of xml.matchAll(/<testcase\b([^>]*?)(\/>|>([\s\S]*?)<\/testcase>)/g)) {
    const attr = (name) => (m[1].match(new RegExp(`\\b${name}="([^"]*)"`)) || [])[1] || ''
    const body = m[3] || ''
    if (/<skipped\b/.test(body)) continue
    results.set(`${prefix} ${attr('classname') || attr('file')}#${attr('name')}`, !/<(failure|error)\b/.test(body))
  }
  return results
}

function readJunit(path, prefix) {
  const files = statSync(path).isDirectory()
    ? readdirSync(path, { recursive: true }).filter((f) => String(f).endsWith('.xml')).map((f) => join(path, String(f)))
    : [path]
  const all = new Map()
  for (const f of files) for (const [k, v] of junitResults(readFileSync(f, 'utf8'), prefix)) all.set(k, v)
  return all
}

export function classify(run1, run2) {
  const rows = []
  for (const key of new Set([...run1.keys(), ...run2.keys()])) {
    const a = run1.get(key) ?? false
    const b = run2.get(key) ?? false
    rows.push({ key, status: a && b ? 'pass' : a || b ? 'flaky' : 'fail' })
  }
  return rows
}

export function run(argv, { root = process.cwd(), log = console.log } = {}) {
  let targets
  try {
    targets = parseArgs(argv)
    if (!targets.length) throw new Error('no tests given')
  } catch (e) {
    log(`usage: qa-twice.mjs <component>=<test>[,<test>...] ...\n${e.message}`)
    return 2
  }
  const cfg = load(root)
  const out = join(root, 'scratchpad', 'qa-twice')
  rmSync(out, { recursive: true, force: true })
  mkdirSync(out, { recursive: true })
  const rows = []
  for (const { component, tests } of targets) {
    const qa = cfg.components.find((c) => c.name === component)?.qa
    if (!qa?.run || !qa?.junit) {
      log(`component "${component}" has no qa.run / qa.junit in sdlc.config.json`)
      return 2
    }
    const report = join(root, safeReportPath(qa.junit))
    const cmd = qa.run.replaceAll('{tests}', tests.join(qa.sep ?? ' '))
    const runs = []
    for (const n of [1, 2]) {
      log(`== ${component} run ${n}: ${tests.join(' ')}`)
      rmSync(report, { recursive: true, force: true })
      try { execSync(cmd, { cwd: root, stdio: 'ignore', shell: true }) } catch { /* failures show in the report */ }
      if (!existsSync(report)) {
        log(`${component} run ${n} wrote no JUnit XML at ${qa.junit} (compile error?)`)
        return 2
      }
      cpSync(report, join(out, `${component}-${n}`), { recursive: true })
      runs.push(readJunit(report, component))
    }
    rows.push(...classify(runs[0], runs[1]))
  }
  const count = (s) => rows.filter((r) => r.status === s).length
  for (const r of rows.filter((r) => r.status !== 'pass')) log(`${r.status.toUpperCase().padEnd(5)}  ${r.key}`)
  log(`QA-TWICE: ${rows.length} tests | pass ${count('pass')} | flaky ${count('flaky')} | fail ${count('fail')}`)
  if (rows.length === 0) return 2
  return count('fail') > 0 ? 1 : 0
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) process.exit(run(process.argv.slice(2)))

#!/usr/bin/env node
// The autopilot run ledger: one row per stage run, written by command so a row cannot be skipped by forgetting to
// edit a table. Works on the local ledger (scratchpad/autopilot/*.md) and on a headless run report
// (docs/reports/autopilot-*.md): both have a "## Runs" table, and columns are matched by their header names.
//
//   start --slug <slug> --request "<one line>" [--plan <path>]   new local ledger; prints its path
//   add --stage <stage> --result <result> [--round n] [--cycle n] [--path skill|fallback|inline]
//       [--blocking n] [--advisory n] [--turns n] [--min n] [--tokens n] [--model m]
//   note "<text>"                                               a line under "## Notes"
//   check                                                       exit 1 and list the stages missing before the landing commit
//   total                                                       write and print the "## Total" line
// Every command except start takes [--file <ledger or report>]; default: the ledger `start` made last.
import { existsSync, mkdirSync, readFileSync, readdirSync, statSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

const DIR = 'scratchpad/autopilot'
const CURRENT = join(DIR, 'CURRENT')

export const STAGES = ['plan', 'plan-review', 'gate1', 'implement', 'code-review', 'qa-specs', 'qa', 'fix', 'land', 'gate2', 'ci']
// Stages every run passes before its landing commit, where `check` runs. Gate 2 comes after that commit, so it
// cannot be required here (a test run found `check` failing on it every time).
export const REQUIRED = ['plan', 'plan-review', 'gate1', 'implement', 'code-review', 'qa']
const RESULTS = {
  'plan-review': /^(approve|revise|blocked)$/,
  'code-review': /^(approve|revise|blocked)$/,
  gate1: /^(approved|auto|revise|stopped)$/,
  gate2: /^(push|auto|no push|stopped)$/,
  qa: /^(pass|flaky \d+|fail \d+|broken)$/,
}
const NUM = ['round', 'cycle', 'blocking', 'advisory', 'turns', 'min', 'tokens']

const pad = (n) => String(n).padStart(2, '0')
const stamp = (d) => `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}-${pad(d.getHours())}${pad(d.getMinutes())}`
const human = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`

export function parseArgs(argv) {
  const out = { _: [] }
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i]
    if (a.startsWith('--')) {
      const v = argv[i + 1]
      if (v === undefined || v.startsWith('--')) throw new Error(`${a} needs a value`)
      out[a.slice(2)] = v
      i++
    } else out._.push(a)
  }
  return out
}

const cells = (line) => line.trim().replace(/^\||\|$/g, '').split('|').map((c) => c.trim())
const isRow = (line) => /^\s*\|/.test(line)

// The "## Runs" table: header columns, the index of its last line, and its data rows as { column: value }
export function readTable(text) {
  const lines = text.split(/\r?\n/)
  const h = lines.findIndex((l) => /^## Runs\s*$/.test(l))
  if (h < 0) throw new Error('no "## Runs" section')
  const head = lines.findIndex((l, i) => i > h && isRow(l))
  if (head < 0) throw new Error('no table under "## Runs"')
  const columns = cells(lines[head])
  let end = head + 1
  while (end + 1 < lines.length && isRow(lines[end + 1])) end++
  const rows = lines.slice(head + 2, end + 1).map((l) => Object.fromEntries(cells(l).map((c, i) => [columns[i], c])))
  return { lines, columns, head, end, rows }
}

// Template rows still holding <placeholders> are dropped when the first real row arrives
const isPlaceholder = (row) => Object.values(row).some((v) => /<[^>]+>/.test(v))

export function addRow(text, opts) {
  const stage = opts.stage
  if (!STAGES.includes(stage)) throw new Error(`stage must be one of ${STAGES.join(', ')}`)
  const result = String(opts.result ?? '').trim()
  if (!result) throw new Error('--result is required')
  if (RESULTS[stage] && !RESULTS[stage].test(result)) throw new Error(`${stage} result must match ${RESULTS[stage]}`)
  for (const k of NUM) if (opts[k] !== undefined && !/^\d+(\.\d+)?$/.test(String(opts[k]))) throw new Error(`--${k} must be a number`)
  if (opts.path && !/^(skill|fallback|inline)$/.test(opts.path)) throw new Error('--path must be skill, fallback or inline')

  const t = readTable(text)
  const real = t.rows.filter((r) => !isPlaceholder(r))
  const last = real.at(-1)
  const cycle = opts.cycle ?? last?.cycle ?? '1'
  const round = opts.round ?? String(real.filter((r) => r.stage === stage && r.cycle === String(cycle)).length + 1)
  const values = {
    '#': String(real.length + 1), cycle: String(cycle), stage, round: String(round), path: opts.path ?? '—', result,
    blocking: opts.blocking ?? '—', advisory: opts.advisory ?? '—', turns: opts.turns ?? '—', min: opts.min ?? '—',
    tokens: opts.tokens ?? '—', model: opts.model ?? '—', '≈$': '—',
  }
  const row = `| ${t.columns.map((c) => String(values[c] ?? '—').replace(/\|/g, '/')).join(' | ')} |`
  const body = [...t.lines.slice(t.head + 2, t.end + 1).filter((l) => !isPlaceholder(Object.fromEntries(cells(l).map((c, i) => [t.columns[i], c])))), row]
  const lines = [...t.lines.slice(0, t.head + 2), ...body, ...t.lines.slice(t.end + 1)]
  return { text: lines.join('\n'), row: values }
}

export function missingStages(text) {
  const seen = new Set(readTable(text).rows.filter((r) => !isPlaceholder(r)).map((r) => r.stage))
  return REQUIRED.filter((s) => !seen.has(s))
}

export function totals(text) {
  const rows = readTable(text).rows.filter((r) => !isPlaceholder(r))
  const sum = (k) => rows.reduce((n, r) => n + (Number(r[k]) || 0), 0)
  return { runs: rows.length, min: sum('min'), tokens: sum('tokens') }
}

function setSection(text, heading, body, { append = false } = {}) {
  const lines = text.split(/\r?\n/)
  const h = lines.findIndex((l) => l.trim() === heading)
  if (h < 0) return `${text.replace(/\n*$/, '\n\n')}${heading}\n\n${body}\n`
  let end = h + 1
  while (end < lines.length && !/^## /.test(lines[end])) end++
  const old = lines.slice(h + 1, end).filter((l) => l.trim() && !(append && /<[^>]+>/.test(l)))
  const next = append ? [...old, body] : [body]
  return [...lines.slice(0, h + 1), '', ...next, '', ...lines.slice(end)].join('\n').replace(/\n{3,}/g, '\n\n')
}

export function newLedger({ slug, request, plan, now = new Date() }) {
  return `# Autopilot run: ${slug}

- **Request:** ${request}
- **Plan:** ${plan ?? '- (lite)'}
- **Started:** ${human(now)}

## Runs

| # | cycle | stage | round | path | result | blocking | advisory | turns | min | tokens | ≈$ |
|---|-------|-------|-------|------|--------|----------|----------|-------|-----|--------|----|

## Notes

## Total
`
}

function resolveFile(root, file) {
  if (file) return join(root, file)
  const cur = join(root, CURRENT)
  if (existsSync(cur)) return join(root, readFileSync(cur, 'utf8').trim())
  const dir = join(root, DIR)
  const latest = existsSync(dir) && readdirSync(dir).filter((f) => f.endsWith('.md'))
    .sort((a, b) => statSync(join(dir, b)).mtimeMs - statSync(join(dir, a)).mtimeMs)[0]
  if (!latest) throw new Error('no ledger: run `start` first or pass --file')
  return join(dir, latest)
}

export function run(argv, { root = process.cwd(), now = new Date(), log = console.log } = {}) {
  let a
  try { a = parseArgs(argv) } catch (e) { log(e.message); return 2 }
  const cmd = a._[0]
  try {
    if (cmd === 'start') {
      if (!a.slug || !/^[\w.-]+$/.test(a.slug)) throw new Error('--slug is required: letters, digits, . _ -')
      if (!a.request) throw new Error('--request is required')
      const rel = `${DIR}/${stamp(now)}-${a.slug}.md`
      mkdirSync(join(root, DIR), { recursive: true })
      writeFileSync(join(root, rel), newLedger({ slug: a.slug, request: a.request.replace(/\s+/g, ' '), plan: a.plan, now }))
      writeFileSync(join(root, CURRENT), `${rel}\n`)
      log(rel)
      return 0
    }
    const file = resolveFile(root, a.file)
    const text = readFileSync(file, 'utf8')
    if (cmd === 'add') {
      const { text: next, row } = addRow(text, a)
      writeFileSync(file, next)
      log(`row ${row['#']}: ${row.stage} round ${row.round} → ${row.result}`)
      return 0
    }
    if (cmd === 'note') {
      if (!a._[1]) throw new Error('note needs the text')
      writeFileSync(file, setSection(text, '## Notes', `- ${a._.slice(1).join(' ')}`, { append: true }))
      return 0
    }
    if (cmd === 'check') {
      const missing = missingStages(text)
      if (missing.length) { log(`ledger is missing: ${missing.join(', ')}. Add a row for each stage that ran.`); return 1 }
      log(`ledger complete: ${totals(text).runs} rows`)
      return 0
    }
    if (cmd === 'total') {
      const t = totals(text)
      const line = `${t.runs} runs · ${t.min} min · ${t.tokens} tokens`
      writeFileSync(file, setSection(text, '## Total', line))
      log(line)
      return 0
    }
    throw new Error('command: start | add | note | check | total')
  } catch (e) {
    log(e.message)
    return 2
  }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) process.exit(run(process.argv.slice(2)))

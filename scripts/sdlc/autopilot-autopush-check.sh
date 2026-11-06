#!/usr/bin/env bash
# Local autopilot (ADR-0005): may Gate 2 push without asking? Checks the run's commits (origin/<branch>..HEAD).
# Usage: bash scripts/sdlc/autopilot-autopush-check.sh
# Exit 0 = auto-push allowed; exit 1 = ask the owner, the reason is on stdout.
# Limits and protected paths come from sdlc.config.json (scripts/sdlc/sdlc-config.mjs), shared with autopilot-merge.sh.
set -uo pipefail
# SDLC_HELPER / SDLC_CONFIG_FILE: trusted copies from the deploy branch (headless); default: this checkout
cfg() { node "${SDLC_HELPER:-scripts/sdlc/sdlc-config.mjs}" "$@"; }
deny() { echo "auto-push refused: $1"; exit 1; }

branch=$(cfg get deployBranch)
max_loc=${AUTOPILOT_MAX_LOC:-$(cfg get maxLoc)}
git fetch -q origin "$branch" || deny "git fetch origin $branch failed"
[ -n "$(git rev-list "origin/$branch..HEAD")" ] || deny "no commits ahead of origin/$branch"
# A local edit to settings.local.json (it changes whenever a permission is allowed) does not block
[ -z "$(git status --porcelain --untracked-files=no -- . ':!.claude/settings.local.json')" ] || deny "uncommitted changes to tracked files"

paths=$(git diff --name-only "origin/$branch...HEAD")
bad=$(grep -E "$(cfg protected)" <<<"$paths" | head -5 | tr '\n' ' ')
[ -z "$bad" ] || deny "protected paths changed: $bad"

# numstat prints "-" for binary files; they count as 0 lines
uncounted=$(cfg uncounted)
loc=$(git diff --numstat "origin/$branch...HEAD" | while IFS=$'\t' read -r add del path; do
  grep -qE "$uncounted" <<<"$path" && continue
  echo $(( ${add/-/0} + ${del/-/0} ))
done | awk '{ s += $1 } END { print s + 0 }')
[ "$loc" -le "$max_loc" ] || deny "$loc changed lines outside tests and docs (limit $max_loc)"

echo "auto-push allowed: $(git rev-list --count "origin/$branch..HEAD") commit(s), $loc changed lines outside tests and docs"

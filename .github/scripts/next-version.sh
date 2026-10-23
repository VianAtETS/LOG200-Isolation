#!/usr/bin/env bash
# Calcule la prochaine version sémantique à partir des Conventional Commits
# depuis le dernier tag vX.Y.Z.
#
#   feat!: / type(scope)!: / BREAKING CHANGE:  -> majeure
#   feat:                                      -> mineure
#   fix: / perf:                               -> patch
#   autres (docs, ci, chore, ...)              -> aucune release
#
# Affiche la version (ex. v1.2.3) sur stdout, ou rien s'il n'y a pas de release.
set -euo pipefail

last_tag="$(git describe --tags --abbrev=0 --match 'v[0-9]*' 2>/dev/null || true)"

if [[ -n "$last_tag" ]]; then
    range="${last_tag}..HEAD"
    version="${last_tag#v}"
else
    range="HEAD"
    version="0.0.0"
fi

IFS=. read -r major minor patch <<< "$version"

messages="$(git log "$range" --format='%B')"

bump=""
if grep -qE '^BREAKING[ -]CHANGE:' <<< "$messages" \
    || grep -qE '^[a-z]+(\([^)]*\))?!:' <<< "$messages"; then
    bump="major"
elif grep -qE '^feat(\([^)]*\))?:' <<< "$messages"; then
    bump="minor"
elif grep -qE '^(fix|perf)(\([^)]*\))?:' <<< "$messages"; then
    bump="patch"
fi

case "$bump" in
    major) echo "v$((major + 1)).0.0" ;;
    minor) echo "v${major}.$((minor + 1)).0" ;;
    patch) echo "v${major}.${minor}.$((patch + 1))" ;;
    *) ;;
esac

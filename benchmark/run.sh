#!/usr/bin/env bash
# Fait jouer le client de src/ (branche courante) contre les bots de référence de l'arène.
# Usage : bash benchmark/run.sh [options de l'arène : --games N --time MS --strict ...]
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT

javac -encoding UTF-8 -d "$out/arena" "$root"/benchmark/*.java
if ! javac -encoding UTF-8 -d "$out/client" "$root"/src/*.java; then
    echo "Échec de la compilation de src/." >&2
    exit 1
fi
jar --create --file "$out/client.jar" --main-class Client -C "$out/client" .

label="$(git -C "$root" branch --show-current 2>/dev/null || true)"
status=0
for bot in random greedy; do
    java -cp "$out/arena" Arena --label-a "${label:-courant}" "$@" "$out/client.jar" "$bot" ||
        status=$?
    echo
done
exit "$status"

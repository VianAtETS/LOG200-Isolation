#!/usr/bin/env bash
# Tournoi toutes-rondes entre les clients de plusieurs branches et les bots de référence
# (random, greedy), sans avoir à merger les branches.
# Usage : bash benchmark/compare.sh [branche...]
#   Sans argument : toutes les branches origin/* sauf main.
#   GAMES=N parties par match (défaut 4), TIME=MS temps par coup (défaut 500).
set -euo pipefail

root="$(git rev-parse --show-toplevel)"
games="${GAMES:-4}"
time="${TIME:-500}"

if (($# > 0)); then
    refs=("$@")
else
    mapfile -t refs < <(git -C "$root" for-each-ref --format='%(refname:short)' refs/remotes/origin |
        grep -vE '^origin(/(HEAD|main))?$')
fi

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp/arena" "$root"/benchmark/*.java

players=()
labels=()
errors=()

for ref in "${refs[@]}"; do
    name="${ref#origin/}"
    if ! git -C "$root" rev-parse --verify --quiet "$ref^{commit}" >/dev/null; then
        if git -C "$root" rev-parse --verify --quiet "origin/$ref^{commit}" >/dev/null; then
            ref="origin/$ref"
        else
            errors+=("| $name | branche introuvable |")
            continue
        fi
    fi

    work="$tmp/${name//\//_}"
    mkdir -p "$work/bin"
    if ! git -C "$root" archive "$ref" src | tar -x -C "$work" 2>/dev/null; then
        errors+=("| $name | pas de dossier src/ |")
        continue
    fi
    if ! javac -encoding UTF-8 -d "$work/bin" "$work"/src/*.java 2>"$work/javac.log"; then
        head -n 20 "$work/javac.log"
        errors+=("| $name | compilation échouée |")
        continue
    fi
    jar --create --file "$work/client.jar" --main-class Client -C "$work/bin" .
    players+=("$work/client.jar")
    labels+=("$name")
done

players+=(random greedy)
labels+=(random greedy)

declare -A points
for label in "${labels[@]}"; do points[$label]=0; done
matches=()

for ((i = 0; i < ${#players[@]}; i++)); do
    for ((j = i + 1; j < ${#players[@]}; j++)); do
        a="${labels[i]}"
        b="${labels[j]}"
        [[ "${players[i]}" == random || "${players[i]}" == greedy ]] &&
            [[ "${players[j]}" == random || "${players[j]}" == greedy ]] && continue

        echo
        log="$tmp/match-$i-$j.log"
        if ! java -cp "$tmp/arena" Arena --games "$games" --time "$time" \
            --label-a "$a" --label-b "$b" "${players[i]}" "${players[j]}" | tee "$log"; then
            matches+=("| $a | $b | match échoué | | | | |")
            continue
        fi
        IFS=';' read -r _ _ _ winsA winsB toA toB invA invB crashA crashB avgA avgB _ _ \
            < <(grep '^RESULT;' "$log" | tail -n 1)
        points[$a]=$((points[$a] + winsA))
        points[$b]=$((points[$b] + winsB))
        matches+=("| $a | $b | $winsA – $winsB | $toA / $toB | $invA / $invB | $crashA / $crashB | $avgA / $avgB |")
    done
done

standings="$(
    echo "| Joueur | Victoires |"
    echo "|---|---:|"
    for label in "${labels[@]}"; do echo "${points[$label]} $label"; done |
        sort -rn | while read -r score label; do echo "| $label | $score |"; done
    if ((${#errors[@]} > 0)); then printf "%s\n" "${errors[@]}"; fi
)"
table="$(
    echo "| A | B | Score A – B | Timeouts | Coups invalides | Plantages | ms/coup moyen |"
    echo "|---|---|---|---|---|---|---|"
    printf '%s\n' "${matches[@]}"
)"

echo
echo "$standings"
echo
echo "$table"

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
    {
        echo "## Tournoi Isolation ($games parties par match, $time ms/coup)"
        echo
        echo "$standings"
        echo
        echo "$table"
        echo
        echo "_Les temps sur CI sont bruités : un client proche de la limite peut y dépasser le temps._"
    } >>"$GITHUB_STEP_SUMMARY"
fi

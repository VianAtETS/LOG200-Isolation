#!/usr/bin/env bash
# Installe les extensions VS Code dans le conteneur.
# Contourne le bogue de VS Code sous Linux où la liste "extensions" de
# devcontainer.json n'est pas installée.
set -euo pipefail

EXTENSIONS=(
    vscjava.vscode-java-pack
    redhat.java
    josevseb.google-java-format-for-vs-code
    vscjava.vscode-java-debug
    myriad-dreamin.tinymist
    streetsidesoftware.code-spell-checker
    streetsidesoftware.code-spell-checker-french
    EditorConfig.EditorConfig
    ms-azuretools.vscode-docker
)

# Serveur VS Code installé dans le conteneur (le plus récent)
server=$(ls -td "$HOME"/.vscode-server/bin/*/bin/code-server "$HOME"/.vscode-server/cli/servers/*/server/bin/code-server 2>/dev/null | head -n 1 || true)
if [[ -z "$server" ]]; then
    echo "Serveur VS Code introuvable, extensions non installées" >&2
    exit 0
fi

args=()
for ext in "${EXTENSIONS[@]}"; do
    args+=(--install-extension "$ext")
done
"$server" "${args[@]}"

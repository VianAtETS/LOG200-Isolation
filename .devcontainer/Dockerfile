# JDK 25 (LTS) complet : java, javac et jdb (débogueur en ligne de commande)
FROM mcr.microsoft.com/devcontainers/java:25-bookworm

# Version épinglée de Typst (https://github.com/typst/typst/releases)
ARG TYPST_VERSION=0.15.1

# Polices (accents français) et utilitaires pour Typst et le terminal
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        xz-utils \
        fonts-linuxlibertine \
        fonts-noto-core \
        fonts-dejavu \
        tree \
        less \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Typst CLI (binaire statique musl), selon l'architecture
RUN case "$(dpkg --print-architecture)" in \
        amd64) TYPST_ARCH=x86_64 ;; \
        arm64) TYPST_ARCH=aarch64 ;; \
        *) echo "Architecture non supportée" >&2; exit 1 ;; \
    esac \
    && curl -fsSL "https://github.com/typst/typst/releases/download/v${TYPST_VERSION}/typst-${TYPST_ARCH}-unknown-linux-musl.tar.xz" \
        | tar -xJ -C /tmp \
    && install -m 0755 "/tmp/typst-${TYPST_ARCH}-unknown-linux-musl/typst" /usr/local/bin/typst \
    && rm -rf /tmp/typst-*

# JUnit (lanceur autonome) pour les tests unitaires dans test/, sans outil de build
ARG JUNIT_VERSION=6.1.3
RUN mkdir -p /opt/junit \
    && curl -fsSL -o "/opt/junit/junit-platform-console-standalone-${JUNIT_VERSION}.jar" \
        "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/${JUNIT_VERSION}/junit-platform-console-standalone-${JUNIT_VERSION}.jar"

# google-java-format (style AOSP) installé localement : l'extension VS Code l'utilise
# directement au lieu de le télécharger elle-même. Il appartient à vscode car
# l'extension fait un chmod +x dessus au démarrage
ARG GJF_VERSION=1.28.0
RUN mkdir -p /opt/google-java-format \
    && curl -fsSL -o /opt/google-java-format/google-java-format.jar \
        "https://github.com/google/google-java-format/releases/download/v${GJF_VERSION}/google-java-format-${GJF_VERSION}-all-deps.jar" \
    && printf '%s\n' '#!/bin/sh' \
        'exec java \' \
        '  --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \' \
        '  --add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \' \
        '  --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \' \
        '  --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \' \
        '  --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \' \
        '  --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \' \
        '  -jar /opt/google-java-format/google-java-format.jar "$@"' \
        > /usr/local/bin/google-java-format \
    && chmod 0755 /usr/local/bin/google-java-format \
    && chown -R vscode:vscode /opt/google-java-format /usr/local/bin/google-java-format

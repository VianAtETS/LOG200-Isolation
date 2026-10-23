# LOG200-Isolation

AI client for the modified **Isolation** game (12×12 board) of the LOG200 lab
(statement: [`docs/isolation.pdf`](docs/isolation.pdf)).

## 🚀 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/VianAtETS/LOG200-Isolation.git
   cd LOG200-Isolation
   ```
2. Open the folder in VS Code and choose **Reopen in Container**. Docker
   Compose starts two containers:
   - `dev`: JDK 25 (with `jdb`), the VS Code Java debugger, Typst and Tinymist;
     VS Code attaches to this one;
   - `server`: the official game server (`server/BoardGame.exe`, Windows only)
     running under Wine. Its window is shown in a VS Code tab (noVNC on port
     **6080**, opened automatically; otherwise open the **Ports** view and click
     the globe icon on port 6080).

   Both containers share the same network: the server listens on
   `localhost:8888`, exactly as on Windows.
3. Compile the client (or run the default build task, `Ctrl+Shift+B`):
   ```bash
   javac -d bin src/*.java
   ```
4. In the server window, set the network player (**Réseau**) and the opponent
   (**Ordinateur**, level 1 to 3), click **START**, then run the client:
   ```bash
   java -cp bin Client localhost 8888
   ```
   or debug it with `F5` (**Déboguer Client (serveur local)**). The client that
   connected first plays red. Set **Parties** to play several games in a row
   (batch mode, summary in the status bar).
5. Write unit tests as `test/*Test.java` (JUnit 6, default package). Run them
   with:
   ```bash
   javac -d bin src/*.java
   javac -d bin-test -cp bin:/opt/junit/* test/*.java
   java -jar /opt/junit/*.jar execute -cp bin:bin-test --scan-class-path
   ```
6. Write the report in `remise/rapport.typ` (based on the
   `remise/ets-report.typ` template) and preview it with Tinymist
   (**Typst: Show Preview**) or `typst watch --root . remise/rapport.typ`.

### Game server notes

- Wine comes from the WineHQ **devel** branch (pinned in
  `.devcontainer/server.Dockerfile`): Qt 6.10 needs
  `mscms.WcsCreateIccProfile`, only implemented since Wine 11.15.
- The **Partie terminée** / **Batch terminé** pop-ups are drawn black under
  Wine. Press `Enter` or click their **OK** button area to close them; the
  result is in the move list and the status bar.
- The server is restarted automatically if you click **Quitter**.
- Protocol details observed on the real server that differ from the
  statement: red receives no `3 A8-A8` and must play as soon as it receives the
  `1 <board>` message; moves are sent as `F12 - I9 - D4` (with spaces); a
  destroyed cell "exactly _d_ cells away" from the opponent lies on one of the
  8 directions (like a chess queen), not on a square ring.

## 🏁 Benchmark

`benchmark/` contains a headless **arena** (`Arena.java`) that replaces the GUI
server: it speaks the same protocol, validates the moves with the rules checked
against the official server (`IsolationRules.java`), enforces the time per move
and plays several games on the same connection, alternating colors. Reference
players: `random` and `greedy` (one-ply mobility).

```bash
bash benchmark/run.sh --games 4 --time 500      # current branch vs random and greedy
bash benchmark/compare.sh                       # round-robin between all origin/* branches
GAMES=10 TIME=1000 bash benchmark/compare.sh vianney origin/yacine
```

The client must accept `host port` arguments and play on its own (no console
input). Results: wins per color, timeouts, invalid moves, crashes and time per
move.

## 🔄 CI/CD

- **CI** (`.github/workflows/ci.yml`): on every pull request and push, checks
  the formatting (`google-java-format --aosp`), compiles `src/`, runs the JUnit
  tests in `test/` (skipped if there are none), builds `Client-eqXXX.jar` and
  plays 2 games against `random` in the arena (smoke test, non-blocking until
  the client plays on its own), then compiles the Typst report.
- **Benchmark** (`.github/workflows/benchmark.yml`): on every push, plays the
  branch against the reference bots and runs a round-robin tournament between
  all branches; tables are in the job summary (_Actions_ tab).
- **Release** (`.github/workflows/release.yml`): on every push to `main`, runs
  the CI, then computes the next semantic version from the
  [Conventional Commits](https://www.conventionalcommits.org/) since the last
  tag and publishes a GitHub release with `Client-eqXXX.jar` and a submission
  zip (jar, `src/*.java`, `remise/LISEZMOI.md`, `rapport.pdf`):
  - `feat!:` or `BREAKING CHANGE:` → major, `feat:` → minor,
    `fix:`/`perf:` → patch;
  - other types (`docs`, `ci`, `chore`, …) do not create a release.

  With squash merges, the **PR title** becomes the commit message, so it must
  follow the convention.

Set the team number in `TEAM` (`.github/workflows/ci.yml`) and in the
**Construire le jar** task (`.vscode/tasks.json`). The submission must run with
`java -jar Client-eqXXX.jar`.

## 📁 Project Structure

- `.devcontainer/` - Dev containers (`docker-compose.yml`: `dev` + `server`)
  - `Dockerfile` - Java 25, Typst, JUnit, google-java-format
  - `server.Dockerfile`, `server-entrypoint.sh` - Wine, Xvfb, noVNC
- `.github/` - Issue/PR templates, workflows and release script
- `.vscode/` - Workspace settings, tasks and debug configurations
- `benchmark/` - Arena, reference rules and bots, comparison scripts
- `docs/isolation.pdf` - Lab statement
- `remise/` - Typst report (`rapport.typ`), ÉTS template, `LISEZMOI.md`
- `server/` - Official game server (Windows, Qt 6)
- `src/` - Java client (`Client`, `GameConnection`)
- `test/` - JUnit unit tests

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

// Arène Isolation : remplace le serveur officiel pour faire jouer deux clients l'un contre
// l'autre, sans interface graphique (CI, comparaison de branches).
//
// Implémente le protocole de l'énoncé (codes 0 à 5), valide les coups avec IsolationRules,
// applique le temps maximal par coup et alterne les couleurs. Toutes les parties se jouent sur
// la même connexion, comme l'exige le deuxième point de contrôle.
//
// Un joueur est soit un jar (lancé avec « java -jar X.jar localhost <port> »), soit un bot de
// référence (random, greedy).
//
// Usage : java -cp out Arena [--games N] [--time MS] [--grace MS] [--seed S] [--logs DIR]
//                            [--label-a NOM] [--label-b NOM] [--strict] JOUEUR_A JOUEUR_B
//   --strict : code de sortie 1 si A plante, dépasse le temps ou joue un coup invalide.
class Arena {
    private static final int SIZE = 12;
    private static final int PROTOCOL_VERSION = 2;
    private static final int MAX_INVALID = 3;
    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final String EOF = "\0EOF";

    public static void main(String[] args) throws Exception {
        int games = 4;
        int time = 1000;
        int grace = 200;
        long seed = 42;
        boolean strict = false;
        String labelA = null;
        String labelB = null;
        Path logs = null;
        String[] players = new String[2];
        int count = 0;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--games" -> games = Integer.parseInt(args[++i]);
                case "--time" -> time = Integer.parseInt(args[++i]);
                case "--grace" -> grace = Integer.parseInt(args[++i]);
                case "--seed" -> seed = Long.parseLong(args[++i]);
                case "--logs" -> logs = Path.of(args[++i]);
                case "--label-a" -> labelA = args[++i];
                case "--label-b" -> labelB = args[++i];
                case "--strict" -> strict = true;
                default -> {
                    if (count == 2) usage();
                    players[count++] = args[i];
                }
            }
        }
        if (count != 2) usage();
        if (logs == null) logs = Files.createTempDirectory("arena");
        Files.createDirectories(logs);

        Arena arena = new Arena(time, grace);
        Seat a;
        Seat b;
        try (ServerSocket server = new ServerSocket(0, 2, InetAddress.getLoopbackAddress())) {
            server.setSoTimeout(CONNECT_TIMEOUT_MS);
            String nameA = label(labelA, players[0]);
            String nameB = label(labelB, players[1]);
            if (nameA.equals(nameB)) {
                nameA += " (A)";
                nameB += " (B)";
            }
            a = connect(server, players[0], nameA, seed, logs.resolve("a.log"));
            b = connect(server, players[1], nameB, seed + 1, logs.resolve("b.log"));
        }

        System.out.printf(
                Locale.ROOT,
                "=== Arène Isolation : %s vs %s (%d parties, %d ms/coup) ===%n",
                a.label,
                b.label,
                games,
                time);
        try {
            for (int g = 0; g < games; g++) {
                boolean aIsRed = g % 2 == 0;
                arena.playGame(g + 1, aIsRed ? a : b, aIsRed ? b : a);
            }
        } finally {
            a.close();
            b.close();
        }

        System.out.println();
        System.out.println(
                "| Joueur | Victoires | V. rouge | V. noir | Timeouts | Coups invalides | Plantages"
                        + " | ms/coup moyen | ms/coup max |");
        System.out.println("|---|---:|---:|---:|---:|---:|---:|---:|---:|");
        a.printRow(games);
        b.printRow(games);
        System.out.printf(
                Locale.ROOT,
                "RESULT;%s;%s;%d;%d;%d;%d;%d;%d;%d;%d;%.1f;%.1f;%.1f;%.1f%n",
                a.label,
                b.label,
                a.wins,
                b.wins,
                a.timeouts,
                b.timeouts,
                a.invalid,
                b.invalid,
                a.crashes,
                b.crashes,
                a.averageMs(),
                b.averageMs(),
                a.maxMs(),
                b.maxMs());
        System.out.println("Journaux des clients : " + logs);

        if (strict && (a.timeouts > 0 || a.invalid > 0 || a.crashes > 0)) System.exit(1);
        System.exit(0);
    }

    private static void usage() {
        System.err.println(
                "Usage : java -cp out Arena [--games N] [--time MS] [--grace MS] [--seed S]"
                        + " [--logs DIR] [--label-a NOM] [--label-b NOM] [--strict]"
                        + " JOUEUR_A JOUEUR_B\n  JOUEUR : chemin d'un jar ou "
                        + Bots.NAMES);
        System.exit(2);
    }

    private static String label(String label, String spec) {
        if (label != null) return label;
        return Bots.isBot(spec) ? spec : new File(spec).getName().replaceFirst("\\.jar$", "");
    }

    private static Seat connect(ServerSocket server, String spec, String label, long seed, Path log)
            throws IOException {
        Process process = null;
        if (Bots.isBot(spec)) {
            Bots.start(spec, "localhost", server.getLocalPort(), seed);
        } else {
            process =
                    new ProcessBuilder(
                                    "java",
                                    "-jar",
                                    spec,
                                    "localhost",
                                    Integer.toString(server.getLocalPort()))
                            .redirectErrorStream(true)
                            .redirectOutput(log.toFile())
                            .start();
        }
        try {
            return new Seat(label, server.accept(), process);
        } catch (IOException e) {
            if (process != null) process.destroyForcibly();
            throw new IOException(label + " ne s'est pas connecté (journal : " + log + ")", e);
        }
    }

    private final int time;
    private final int grace;

    private Arena(int time, int grace) {
        this.time = time;
        this.grace = grace;
    }

    private void playGame(int number, Seat red, Seat black) throws InterruptedException {
        IsolationRules game = IsolationRules.initial(SIZE, SIZE);
        red.lines.clear();
        black.lines.clear();
        String info = "0 " + PROTOCOL_VERSION + " " + SIZE + " " + SIZE + " " + time + " ";
        red.send(info + 0);
        black.send(info + 1);
        red.send("1 " + game.boardMessage());
        black.send("2 " + game.boardMessage());

        Seat current = red;
        Seat other = black;
        // Comme le serveur officiel : pas de « 3 » pour le premier coup, le rouge joue dès le « 1 »
        String last = null;
        int plies = 0;
        while (true) {
            int pawn = current == red ? IsolationRules.RED : IsolationRules.BLACK;
            if (last != null) {
                current.lines.clear();
                current.send("3 " + last);
            }

            IsolationRules.Move move = null;
            int invalid = 0;
            String loss = null;
            while (move == null && loss == null) {
                long start = System.nanoTime();
                String line =
                        current.alive
                                ? current.lines.poll(time + grace, TimeUnit.MILLISECONDS)
                                : EOF;
                long elapsed = System.nanoTime() - start;
                if (line == null) {
                    current.timeouts++;
                    loss = "TIMEOUT";
                } else if (line.equals(EOF)) {
                    current.alive = false;
                    current.crashes++;
                    loss = "LOSS";
                } else {
                    current.record(elapsed);
                    IsolationRules.Move parsed = game.parse(line);
                    if (parsed != null && game.isLegal(parsed, pawn)) {
                        move = parsed;
                    } else {
                        current.invalid++;
                        System.out.println("  coup invalide de " + current.label + " : " + line);
                        if (++invalid >= MAX_INVALID) loss = "LOSS";
                        else current.send("4");
                    }
                }
            }

            if (loss != null) {
                finish(number, other, current, other == red, loss, last, plies);
                return;
            }
            game.play(move);
            plies++;
            last = game.format(move);
            int next = IsolationRules.opponent(pawn);
            if (!game.hasMoves(next)) {
                finish(number, current, other, current == red, "LOSS", last, plies);
                return;
            }
            Seat swap = current;
            current = other;
            other = swap;
        }
    }

    private void finish(
            int number,
            Seat winner,
            Seat loser,
            boolean winnerIsRed,
            String loss,
            String last,
            int plies) {
        winner.wins++;
        if (winnerIsRed) winner.winsRed++;
        else winner.winsBlack++;
        String suffix = last == null ? "" : " " + last;
        winner.send("5 WIN" + suffix);
        loser.send("5 " + loss + suffix);
        System.out.printf(
                Locale.ROOT,
                "Partie %d : %s bat %s en %d coups%s%n",
                number,
                winner.label,
                loser.label,
                plies,
                loss.equals("LOSS") ? "" : " (" + loss + ")");
    }

    // Un joueur connecté : un fil lit ses lignes dans une file pour pouvoir attendre avec délai
    private static final class Seat {
        final String label;
        final Socket socket;
        final Process process;
        final BufferedWriter out;
        final BlockingQueue<String> lines = new LinkedBlockingQueue<>();
        volatile boolean alive = true;
        int wins;
        int winsRed;
        int winsBlack;
        int timeouts;
        int invalid;
        int crashes;
        long moves;
        long totalNanos;
        long maxNanos;

        Seat(String label, Socket socket, Process process) throws IOException {
            this.label = label;
            this.socket = socket;
            this.process = process;
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Thread reader =
                    new Thread(
                            () -> {
                                try {
                                    String line;
                                    while ((line = in.readLine()) != null) lines.add(line);
                                } catch (IOException e) {
                                    // Connexion fermée
                                }
                                lines.add(EOF);
                            },
                            "reader-" + label);
            reader.setDaemon(true);
            reader.start();
        }

        void send(String message) {
            if (!alive) return;
            try {
                out.write(message);
                out.newLine();
                out.flush();
            } catch (IOException e) {
                alive = false;
            }
        }

        void record(long nanos) {
            moves++;
            totalNanos += nanos;
            maxNanos = Math.max(maxNanos, nanos);
        }

        double averageMs() {
            return moves == 0 ? 0 : totalNanos / 1e6 / moves;
        }

        double maxMs() {
            return maxNanos / 1e6;
        }

        void printRow(int games) {
            System.out.printf(
                    Locale.ROOT,
                    "| %s | %d/%d | %d | %d | %d | %d | %d | %.1f | %.1f |%n",
                    label,
                    wins,
                    games,
                    winsRed,
                    winsBlack,
                    timeouts,
                    invalid,
                    crashes,
                    averageMs(),
                    maxMs());
        }

        void close() {
            try {
                socket.close();
            } catch (IOException e) {
                // Déjà fermé
            }
            if (process != null) {
                try {
                    if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly();
                } catch (InterruptedException e) {
                    process.destroyForcibly();
                }
            }
        }
    }
}

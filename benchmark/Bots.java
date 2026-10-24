import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.List;
import java.util.Random;

// Joueurs de référence de l'arène. Ils passent par le protocole réseau, comme un vrai client :
//  - random : coup légal tiré au hasard ;
//  - greedy : un coup d'avance, maximise (sa mobilité - 2 × mobilité adverse).
final class Bots {
    static final List<String> NAMES = List.of("random", "greedy");

    private Bots() {}

    static boolean isBot(String spec) {
        return NAMES.contains(spec);
    }

    static Thread start(String name, String host, int port, long seed) {
        Thread thread = new Thread(() -> run(name, host, port, new Random(seed)), "bot-" + name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void run(String name, String host, int port, Random random) {
        try (Socket socket = new Socket(host, port);
                BufferedReader in =
                        new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter out =
                        new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
            int cols = 0;
            int rows = 0;
            int me = IsolationRules.RED;
            IsolationRules game = null;
            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                switch (parts[0]) {
                    case "0" -> {
                        rows = Integer.parseInt(parts[2]);
                        cols = Integer.parseInt(parts[3]);
                        me = parts[5].equals("0") ? IsolationRules.RED : IsolationRules.BLACK;
                    }
                    case "1", "2" -> {
                        int[] cells = new int[cols * rows];
                        for (int i = 0; i < cells.length; i++) {
                            cells[i] = Integer.parseInt(parts[i + 1]);
                        }
                        game = new IsolationRules(cols, rows, cells);
                        // Les rouges jouent le premier coup dès réception du plateau
                        if (parts[0].equals("1")) play(name, game, me, random, out);
                    }
                    case "3" -> {
                        game.play(game.parse(line.substring(2)));
                        play(name, game, me, random, out);
                    }
                    default -> {}
                }
            }
        } catch (IOException e) {
            // Connexion fermée par l'arène
        }
    }

    private static void play(
            String name, IsolationRules game, int me, Random random, BufferedWriter out)
            throws IOException {
        IsolationRules.Move move = choose(name, game, me, random);
        game.play(move);
        out.write(game.format(move));
        out.newLine();
        out.flush();
    }

    private static IsolationRules.Move choose(
            String name, IsolationRules game, int me, Random random) {
        List<IsolationRules.Move> moves = game.legalMoves(me);
        if (name.equals("random")) return moves.get(random.nextInt(moves.size()));

        int opponent = IsolationRules.opponent(me);
        IsolationRules.Move best = null;
        int bestScore = Integer.MIN_VALUE;
        int ties = 0;
        for (IsolationRules.Move move : moves) {
            IsolationRules next = game.copy();
            next.play(move);
            int score =
                    !next.hasMoves(opponent)
                            ? Integer.MAX_VALUE
                            : next.destinations(me).size() - 2 * next.destinations(opponent).size();
            if (score > bestScore) {
                best = move;
                bestScore = score;
                ties = 1;
            } else if (score == bestScore && random.nextInt(++ties) == 0) {
                best = move;
            }
        }
        return best;
    }
}

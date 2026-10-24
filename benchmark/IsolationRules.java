import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Règles de la version modifiée du jeu Isolation (énoncé docs/isolation.pdf), indépendantes de
// toute branche : utilisées par l'arène pour valider les coups et par les bots de référence.
//
// Les cases sont indexées comme dans le protocole : index = y * cols + x, y = 0 en haut.
//
// Vérifié contre le serveur officiel (BoardGame.exe sous Wine) :
//  - notation : colonne A.. = x (de gauche à droite), ligne 1 = bas du plateau ; noir part de
//    F12, rouge de G1 ;
//  - une cible « à exactement d cases » de l'adversaire est sur l'une des 8 directions (comme une
//    dame aux échecs), pas sur l'anneau carré de rayon d ;
//  - le serveur envoie les coups sous la forme « F12 - I9 - D4 ».
//
// Hypothèses non vérifiées (cas rares) : une case détruite entre l'adversaire et la cible ne la
// masque pas ; la case de départ libérée peut être détruite ; sans aucune cible disponible, le
// coup se joue sans destruction.
final class IsolationRules {
    static final int EMPTY = 0;
    static final int DESTROYED = 1;
    static final int BLACK = 3;
    static final int RED = 5;

    private static final int MAX_STEPS = 3;
    private static final int[][] DIRECTIONS = {
        {-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}
    };
    private static final Pattern MOVE =
            Pattern.compile(
                    "([A-Za-z])(\\d{1,2})\\s*-?\\s*([A-Za-z])(\\d{1,2})"
                            + "(?:\\s*-?\\s*([A-Za-z])(\\d{1,2}))?");

    // Coup : case de départ, case d'arrivée et case détruite (-1 si aucune)
    record Move(int from, int to, int target) {}

    final int cols;
    final int rows;
    private final int[] cells;
    private int destroyed;

    IsolationRules(int cols, int rows, int[] cells) {
        if (cells.length != cols * rows) throw new IllegalArgumentException("Plateau invalide");
        this.cols = cols;
        this.rows = rows;
        this.cells = cells.clone();
        for (int cell : cells) if (cell == DESTROYED) destroyed++;
    }

    // Configuration initiale : noir en haut, rouge en bas, de part et d'autre du centre
    static IsolationRules initial(int cols, int rows) {
        int[] cells = new int[cols * rows];
        cells[cols / 2 - 1] = BLACK;
        cells[(rows - 1) * cols + cols / 2] = RED;
        return new IsolationRules(cols, rows, cells);
    }

    IsolationRules copy() {
        return new IsolationRules(cols, rows, cells);
    }

    static int opponent(int pawn) {
        return pawn == RED ? BLACK : RED;
    }

    int position(int pawn) {
        for (int i = 0; i < cells.length; i++) if (cells[i] == pawn) return i;
        throw new IllegalStateException("Pion absent du plateau : " + pawn);
    }

    int destroyedCount() {
        return destroyed;
    }

    String boardMessage() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(cells[i]);
        }
        return sb.toString();
    }

    // Cases atteignables : 1 à 3 cases en ligne droite, sans traverser de case occupée
    List<Integer> destinations(int pawn) {
        int from = position(pawn);
        int x0 = from % cols;
        int y0 = from / cols;
        List<Integer> result = new ArrayList<>();
        for (int[] d : DIRECTIONS) {
            for (int step = 1; step <= MAX_STEPS; step++) {
                int x = x0 + d[0] * step;
                int y = y0 + d[1] * step;
                if (x < 0 || x >= cols || y < 0 || y >= rows || cells[y * cols + x] != EMPTY) break;
                result.add(y * cols + x);
            }
        }
        return result;
    }

    // Un pion qui peut se déplacer mais n'a aucune case à détruire est bloqué : il a perdu
    boolean hasMoves(int pawn) {
        return !legalMoves(pawn).isEmpty();
    }

    // Cases vides à exactement « distance » cases du centre, dans l'une des 8 directions
    private List<Integer> emptyCellsAt(int center, int distance) {
        int cx = center % cols;
        int cy = center / cols;
        List<Integer> result = new ArrayList<>();
        for (int[] d : DIRECTIONS) {
            int x = cx + d[0] * distance;
            int y = cy + d[1] * distance;
            if (x >= 0 && x < cols && y >= 0 && y < rows && cells[y * cols + x] == EMPTY) {
                result.add(y * cols + x);
            }
        }
        return result;
    }

    // Distances de destruction de la phase, par ordre de priorité ; un groupe se joue entier
    private int[][] distanceGroups() {
        if (destroyed < 10) return new int[][] {{3}, {2}, {1}};
        if (destroyed < 20) return new int[][] {{2}, {1}};
        return new int[][] {{1, 2}};
    }

    // Le repli vers une distance plus courte est global, comme sur le serveur officiel : il n'a
    // lieu que si AUCUNE destination ne permet de détruire à la distance courante. Une
    // destination sans cible à cette distance n'est alors pas jouable du tout.
    List<Move> legalMoves(int pawn) {
        int from = position(pawn);
        int center = position(opponent(pawn));
        List<Integer> destinations = destinations(pawn);
        for (int[] group : distanceGroups()) {
            List<Move> moves = new ArrayList<>();
            for (int to : destinations) {
                movePawn(from, to);
                for (int distance : group) {
                    for (int target : emptyCellsAt(center, distance)) {
                        moves.add(new Move(from, to, target));
                    }
                }
                movePawn(to, from);
            }
            if (!moves.isEmpty()) return moves;
        }
        return List.of();
    }

    boolean isLegal(Move move, int pawn) {
        return legalMoves(pawn).contains(move);
    }

    // Joue un coup supposé légal
    void play(Move move) {
        movePawn(move.from(), move.to());
        cells[move.target()] = DESTROYED;
        destroyed++;
    }

    private void movePawn(int from, int to) {
        cells[to] = cells[from];
        cells[from] = EMPTY;
    }

    // Format envoyé par le serveur officiel : « F12 - I9 - D4 »
    String format(Move move) {
        return square(move.from()) + " - " + square(move.to()) + " - " + square(move.target());
    }

    String square(int index) {
        return (char) ('A' + index % cols) + Integer.toString(rows - index / cols);
    }

    // Accepte « G1-E3-C12 », « G1E3C12 » et « G1 - E3 - C12 » ; null si la syntaxe ou une case est
    // invalide
    Move parse(String text) {
        Matcher m = MOVE.matcher(text.trim());
        if (!m.matches()) return null;
        int from = index(m.group(1), m.group(2));
        int to = index(m.group(3), m.group(4));
        int target = m.group(5) == null ? -1 : index(m.group(5), m.group(6));
        if (from < 0 || to < 0 || (m.group(5) != null && target < 0)) return null;
        return new Move(from, to, target);
    }

    private int index(String column, String row) {
        int x = Character.toUpperCase(column.charAt(0)) - 'A';
        int y = rows - Integer.parseInt(row);
        return x >= 0 && x < cols && y >= 0 && y < rows ? y * cols + x : -1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < rows; y++) {
            sb.append(String.format("%2d ", rows - y));
            for (int x = 0; x < cols; x++) {
                sb.append(
                        switch (cells[y * cols + x]) {
                            case DESTROYED -> " #";
                            case BLACK -> " N";
                            case RED -> " R";
                            default -> " .";
                        });
            }
            sb.append('\n');
        }
        sb.append("   ");
        for (int x = 0; x < cols; x++) sb.append(' ').append((char) ('A' + x));
        return sb.append('\n').toString();
    }
}

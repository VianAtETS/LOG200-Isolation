import java.util.List;
import java.util.TreeSet;

// Vérifie IsolationRules sur des positions dont le comportement du serveur officiel est connu.
// Sans JUnit pour ne dépendre que du JDK : java -cp out IsolationRulesCheck (code 1 si échec).
final class IsolationRulesCheck {
    private static int failures = 0;

    public static void main(String[] args) {
        initialTargetsAreAtDistanceThree();
        fallbackIsGlobalNotPerMove();
        fallbackToDistanceTwoWhenNoDistanceThreeTarget();
        originSquareIsDestroyableArrivalIsNot();
        middlePhaseUsesDistanceTwo();
        finalPhaseAllowsDistanceOneAndTwo();
        moveWithoutDestructionIsIllegal();
        blockedPawnHasNoMoves();
        mobilePawnWithoutTargetHasNoMoves();

        if (failures > 0) {
            System.out.println(failures + " vérification(s) en échec");
            System.exit(1);
        }
        System.out.println("Règles de l'arène : OK");
    }

    // F12 : cibles à 3 sur les rayons restés dans le plateau
    private static void initialTargetsAreAtDistanceThree() {
        IsolationRules game = IsolationRules.initial(12, 12);
        TreeSet<String> targets = new TreeSet<>();
        for (IsolationRules.Move m : game.legalMoves(IsolationRules.RED)) {
            targets.add(game.square(m.target()));
        }
        check(
                "cibles initiales",
                targets.equals(new TreeSet<>(List.of("C12", "C9", "F9", "I12", "I9"))),
                targets);
    }

    // Noir en A12 : à distance 3 il ne reste que D9 (D12 et A9 sont détruites). Le rouge peut
    // atterrir en D9 ; ce coup-là n'a aucune cible à 3, mais d'autres coups en ont : le repli
    // global l'interdit complètement au lieu de le laisser détruire à 2.
    private static void fallbackIsGlobalNotPerMove() {
        IsolationRules game =
                diagram(
                        "B..#........",
                        "............",
                        "............",
                        "#...........",
                        "............",
                        "............",
                        "...R........",
                        "............",
                        "............",
                        "............",
                        "............",
                        "............");
        List<IsolationRules.Move> moves = game.legalMoves(IsolationRules.RED);
        check("repli global : coups", !moves.isEmpty(), moves);
        for (IsolationRules.Move m : moves) {
            check("repli global : cible D9", game.square(m.target()).equals("D9"), game.format(m));
            check(
                    "repli global : pas d'arrivée D9",
                    !game.square(m.to()).equals("D9"),
                    game.format(m));
        }
    }

    private static void fallbackToDistanceTwoWhenNoDistanceThreeTarget() {
        IsolationRules game =
                diagram(
                        "B..#........",
                        "............",
                        "............",
                        "#..#........",
                        "............",
                        "............",
                        "...R........",
                        "............",
                        "............",
                        "............",
                        "............",
                        "............");
        checkDistances("repli à 2", game, List.of(2));
    }

    // Seule cible restante à distance 3 de A12 : D9, occupée par le rouge. Elle se libère quand
    // il part ; sa case d'arrivée, elle, n'est jamais destructible.
    private static void originSquareIsDestroyableArrivalIsNot() {
        IsolationRules game =
                diagram(
                        "B..#........",
                        "............",
                        "............",
                        "#..R........",
                        "............",
                        "............",
                        "............",
                        "............",
                        "............",
                        "............",
                        "............",
                        "............");
        List<IsolationRules.Move> moves = game.legalMoves(IsolationRules.RED);
        check("case de départ : coups", !moves.isEmpty(), moves);
        for (IsolationRules.Move m : moves) {
            check("case de départ détruite", m.target() == m.from(), game.format(m));
        }
    }

    // 10 cases détruites loin des pions
    private static void middlePhaseUsesDistanceTwo() {
        IsolationRules game =
                diagram(
                        "............",
                        "............",
                        "............",
                        "......B.....",
                        "............",
                        "............",
                        "............",
                        "............",
                        "............",
                        "...R........",
                        "##########..",
                        "............");
        checkDistances("milieu de partie", game, List.of(2));
    }

    // 20 cases détruites
    private static void finalPhaseAllowsDistanceOneAndTwo() {
        IsolationRules game =
                diagram(
                        "............",
                        "............",
                        "............",
                        "......B.....",
                        "............",
                        "............",
                        "............",
                        "............",
                        "..........##",
                        "...R......##",
                        "############",
                        "....####....");
        checkDistances("fin de partie", game, List.of(1, 2));
    }

    // Le serveur répond « 4 » à un coup sans case détruite
    private static void moveWithoutDestructionIsIllegal() {
        IsolationRules game = IsolationRules.initial(12, 12);
        IsolationRules.Move move = game.parse("G1 - G3");
        check("coup sans destruction lu", move != null, move);
        check(
                "coup sans destruction refusé",
                move == null || !game.isLegal(move, IsolationRules.RED),
                move);
    }

    private static void blockedPawnHasNoMoves() {
        IsolationRules game =
                diagram(
                        "R#..........",
                        "##..........",
                        "............",
                        "............",
                        "............",
                        "............",
                        "............",
                        "............",
                        "............",
                        "............",
                        "............",
                        "...........B");
        check("pion bloqué", !game.hasMoves(IsolationRules.RED), game);
        check("adversaire libre", game.hasMoves(IsolationRules.BLACK), game);
    }

    // Noir en A1, toutes les cases à 1, 2 et 3 sur ses rayons détruites : le rouge peut bouger
    // mais n'a rien à détruire, il n'a donc aucun coup et perd
    private static void mobilePawnWithoutTargetHasNoMoves() {
        IsolationRules game =
                diagram(
                        "...........R",
                        "............",
                        "............",
                        "............",
                        "............",
                        "............",
                        "............",
                        "............",
                        "#..#........",
                        "#.#.........",
                        "##..........",
                        "B###........");
        check("rouge mobile", !game.destinations(IsolationRules.RED).isEmpty(), game);
        check("rouge sans cible : aucun coup", !game.hasMoves(IsolationRules.RED), game);
    }

    // Les distances (sur un rayon) entre le noir et les cibles des coups rouges valent exactement
    // « expected »
    private static void checkDistances(String name, IsolationRules game, List<Integer> expected) {
        int black = game.position(IsolationRules.BLACK);
        TreeSet<Integer> distances = new TreeSet<>();
        for (IsolationRules.Move m : game.legalMoves(IsolationRules.RED)) {
            distances.add(rayDistance(game.cols, black, m.target()));
        }
        check(name, distances.equals(new TreeSet<>(expected)), distances);
    }

    // Distance de Chebyshev si les deux cases sont sur une même ligne, colonne ou diagonale,
    // -1 sinon
    private static int rayDistance(int cols, int a, int b) {
        int dx = Math.abs(a % cols - b % cols);
        int dy = Math.abs(a / cols - b / cols);
        return dx == 0 || dy == 0 || dx == dy ? Math.max(dx, dy) : -1;
    }

    // Plateau 12x12 depuis ses lignes, ligne 12 en premier : . vide, # détruite, R rouge, B noir
    private static IsolationRules diagram(String... lines) {
        int[] cells = new int[144];
        int i = 0;
        for (String line : lines) {
            for (char c : line.toCharArray()) {
                cells[i++] =
                        switch (c) {
                            case '#' -> IsolationRules.DESTROYED;
                            case 'R' -> IsolationRules.RED;
                            case 'B' -> IsolationRules.BLACK;
                            default -> IsolationRules.EMPTY;
                        };
            }
        }
        return new IsolationRules(12, 12, cells);
    }

    private static void check(String name, boolean ok, Object detail) {
        if (!ok) {
            failures++;
            System.out.println("ÉCHEC " + name + " : " + detail);
        }
    }
}

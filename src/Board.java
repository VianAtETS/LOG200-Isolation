import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Game board of Isolation (12x12 by default).
 *
 * <p>Squares are stored as board[x][y]: x = column (A = 0) y = row of the array sent by the server
 * (0 = top row = row 12 in the notation)
 *
 * <p>A play is "F12-F10-F5" (start - destination - blocked square). The server sends it with
 * spaces: "F12 - F10 - F5".
 */
public class Board {
    // === Constants ===

    /** The 8 directions a pawn can move in, as {dx, dy}. */
    private static final int[][] DIRECTIONS = {
        {-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}
    };

    // Game phases, depending on the number of blocked squares
    private static final int OPENING = 1; // fewer than 10
    private static final int MIDGAME = 2; // 10 to 19
    private static final int ENDGAME = 3; // 20 or more

    // === Attributes ===

    private final int cols;
    private final int rows;
    private final int[][] board; // board[x][y]

    private int[] redPos = null; // {x, y}
    private int[] blackPos = null; // {x, y}

    private int blockedCount = 0; // used to find the game phase

    // === Constructor ===

    /** Empty 12x12 board. */
    public Board() {
        this(12, 12);
    }

    /** Empty board of the given size. */
    public Board(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        this.board = new int[cols][rows];
    }

    /**
     * Board of the given size, filled with the server data.
     *
     * @param message board data of message 1 or 2, without the command code
     * @throws IllegalArgumentException if the data does not contain cols * rows values
     */
    public Board(int cols, int rows, String message) {
        this(cols, rows);

        this.load(message);
    }

    // === Getters & Setters ===

    /** Squares of the board, as board[x][y]. */
    public int[][] getBoard() {
        return board;
    }

    /** Position {x, y} of a player's pawn (player = Square.RED or Square.BLACK value). */
    public int[] getPos(int player) {
        return player == Square.RED.getValue() ? redPos : blackPos;
    }

    /** Changes a player's pawn position. Does not change the board: update it too. */
    private void setPos(int player, int[] pos) {
        if (player == Square.RED.getValue()) redPos = pos;
        else blackPos = pos;
    }

    /** Number of blocked squares. */
    public int getBlockedCount() {
        return blockedCount;
    }

    /**
     * Current game phase: OPENING, MIDGAME or ENDGAME. Only depends on blockedCount, so
     * getAvailableBlocks can use it safely.
     */
    public int getGamePhase() {
        // 1 - Opening : Less than 10 cases have been blocked
        if (blockedCount < 10) {
            return OPENING;
        }
        // 2 - Midgame : Between 10 and 19 cases have been blocked
        else if (blockedCount < 20) {
            return MIDGAME;
        }
        // 3 - Endgame : 20 or more cases have been blocked
        return ENDGAME;
    }

    // === Methods ===

    /**
     * Fills the board with the server data (message 1 or 2, without the command code) and finds the
     * pawns and the blocked squares.
     *
     * @throws IllegalArgumentException if the data does not contain cols * rows values
     */
    private void load(String message) {
        String[] data = message.trim().split("\\s+");

        if (data.length != cols * rows)
            throw new IllegalArgumentException("Invalid data received.");

        blockedCount = 0;

        // TODO : Check if optimizable
        int index = 0;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {

                int value = Integer.parseInt(data[index++]);

                if (value == Square.RED.getValue()) {
                    redPos = new int[] {x, y};
                } else if (value == Square.BLACK.getValue()) {
                    blackPos = new int[] {x, y};
                } else if (value == Square.BLOCKED.getValue()) {
                    blockedCount++;
                }

                board[x][y] = value;
            }
        }
    }

    /**
     * Applies a play, e.g. "F12-F10-F5": moves the pawn, then blocks a square. The player is the
     * pawn found on the start square.
     */
    public void play(String move) {
        // TODO : Implement play method, in this order (undo relies on it,
        //        the destroyed square may be the start square):
        //        1. start square = EMPTY
        //        2. destination square = player
        //        3. destroyed square = BLOCKED
        //        4. setPos(player, to) and blockedCount++
    }

    /** Cancels a play previously applied with play(), e.g. "F12-F10-F5". */
    public void undo(String move) {
        int[][] sq = parsePlayIntoPosition(move);
        int[] from = sq[0], to = sq[1], block = sq[2];
        int player = board[to[0]][to[1]];

        board[block[0]][block[1]] = Square.EMPTY.getValue();
        board[to[0]][to[1]] = Square.EMPTY.getValue();
        board[from[0]][from[1]] = player;

        setPos(player, from);
        blockedCount--;
    }

    /** Score of the board for a player: higher = better for that player. */
    public int evaluate(int player) {
        // TODO : Implement evaluation function
        return (int) (Math.random() * 100);
    }

    /**
     * Game status. The game is over when the player who must play has no complete play (a move AND
     * a square to block): a pawn that can move but has nothing to block has lost.
     *
     * @param player the player who must play
     * @return -1 if the game is not over, 0 for a draw (never happens in Isolation), otherwise the
     *     winner (Square value)
     */
    public int getGameStatus(int player) {
        // TODO : Implement game over check (no complete play for player)
        return -1;
    }

    /**
     * Squares {x, y} a player's pawn can move to: 1 to 3 squares in one of the 8 directions,
     * without crossing a blocked square or a pawn.
     */
    public List<int[]> getAvailableMoves(int player) {
        // TODO : Implement available moves check
        return new ArrayList<>();
    }

    /**
     * Can a pawn move from "from" to "to"? Both {x, y}. True if: - aligned: dx == 0, dy == 0 or
     * |dx| == |dy| - 1 to 3 squares apart - every square on the way, destination included, is empty
     */
    private boolean isValidMove(int[] from, int[] to) {
        // TODO : Implement move validation
        return true;
    }

    /**
     * Squares {x, y} a player can block after moving their pawn to "to": empty squares at an exact
     * distance from the opponent, in one of the 8 directions. - OPENING: distance 3, else 2, else 1
     * - MIDGAME: distance 2, else 1 - ENDGAME: distance 1 or 2 Look at the board as after the move:
     * start square empty, "to" occupied.
     *
     * <p>"else 2" is decided over ALL the destinations, not for this "to" only: if any destination
     * has a block at distance 3, only distance 3 is allowed, and a destination without one is not
     * playable (the server answers 4). A move without a block is never valid.
     */
    public List<int[]> getAvailableBlocks(int player, int[] to) {
        // TODO : Implement available blocks check
        return new ArrayList<>();
    }

    /**
     * Can "pos" be blocked after the player's pawn moved to "to"? (= is it in getAvailableBlocks)
     */
    private boolean isValidBlock(int player, int[] to, int[] pos) {
        for (int[] block : getAvailableBlocks(player, to)) {
            if (Arrays.equals(block, pos)) // not contains(): int[] are compared by reference
            return true;
        }
        return false;
    }

    /** "F12-F10-F5" (with or without spaces) → {start, destination, blocked}, each {x, y}. */
    private int[][] parsePlayIntoPosition(String play) {
        String[] sq = play.replaceAll("\\s", "").split("-");
        return new int[][] {
            formatPosInCoord(sq[0]), formatPosInCoord(sq[1]), formatPosInCoord(sq[2])
        };
    }

    /** (0, 1) → "A11" */
    private String formatPosInNote(int x, int y) {
        return "" + (char) ('A' + x) + (this.rows - y);
    }

    /** "A11" → {0, 1} */
    private int[] formatPosInCoord(String s) {
        int col = s.charAt(0) - 'A';
        int row = this.rows - Integer.parseInt(s.substring(1));
        return new int[] {col, row};
    }

    // === Functions ===

}

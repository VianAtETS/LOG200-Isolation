import java.io.*;

class Client {

    public static void main(String[] args) {
        String host = args.length >= 1 ? args[0] : "localhost";
        int port = args.length >= 2 ? Integer.parseInt(args[1]) : 8888;
        int[][] board = null;

        try (GameConnection connection = new GameConnection(host, port);
                BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            GameConnection.GameInfo game = null;

            System.out.println("Connecté à " + host + ":" + port);

            while (true) {
                GameConnection.Message message = connection.readMessage();

                if (message == null) {
                    System.out.println("Connexion fermée.");
                    break;
                }

                switch (message.getCommand()) {
                    case GameConnection.GAME_INFO:
                        game = connection.parseGameInfo(message);
                        System.out.println(
                                "Nouvelle partie :"
                                        + "\n  Plateau : "
                                        + game.getRows()
                                        + " x "
                                        + game.getCols()
                                        + "\n  Temps maximal par coup : "
                                        + game.getMaxMoveTime()
                                        + " ms");

                        board = new int[game.getCols()][game.getRows()];
                        break;

                    case GameConnection.PLAY_RED:
                        readBoard(message.getData(), board, game.getRows(), game.getCols());

                        System.out.println("Nouvelle partie! Vous jouez rouge.");

                        System.out.println("Entrez votre premier coup : ");

                        connection.sendMove(console.readLine());
                        break;

                    case GameConnection.PLAY_BLACK:
                        readBoard(message.getData(), board, game.getRows(), game.getCols());

                        System.out.println("Nouvelle partie! Vous jouez noir.");

                        System.out.println("Attendez le coup des rouges.");

                        break;

                    case GameConnection.PLAY:
                        System.out.println("Dernier coup : " + message.getData());

                        System.out.println("Entrez votre coup : ");

                        connection.sendMove(console.readLine());

                        break;

                    case GameConnection.INVALID_MOVE:
                        System.out.println("Coup invalide, entrez un nouveau coup : ");

                        connection.sendMove(console.readLine());

                        break;

                    case GameConnection.GAME_OVER:
                        if (message.getData().isEmpty()) {
                            System.out.println("Partie terminée.");
                        } else {
                            System.out.println(
                                    "Partie terminée. Dernier coup : " + message.getData());
                        }
                        break;

                    default:
                        System.out.println("Commande inconnue : " + message.getCommand());
                        break;
                }
            }

        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Erreur réseau : " + e.getMessage());
        }
    }

    private static void readBoard(String data, int[][] board, int rows, int cols) {

        String[] values = data.trim().split("\\s+");

        if (values.length != rows * cols)
            throw new IllegalArgumentException("Invalid board received.");

        int index = 0;

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                board[x][y] = Integer.parseInt(values[index++]);
            }
        }
    }
}

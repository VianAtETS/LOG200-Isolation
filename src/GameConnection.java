import java.io.*;
import java.net.*;

public class GameConnection implements Closeable {

    public static final int GAME_INFO    = 0;
    public static final int PLAY_RED     = 1;
    public static final int PLAY_BLACK   = 2;
    public static final int PLAY         = 3;
    public static final int INVALID_MOVE = 4;
    public static final int GAME_OVER    = 5;

    public static final int PROTOCOL_VERSION = 2;

    public static final int RED   = 0;
    public static final int BLACK = 1;

    private final Socket socket;
    private final BufferedReader input;
    private final BufferedWriter output;

    public GameConnection(String host, int port) throws IOException {
        socket = new Socket(host, port);

        input = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

        output = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream()));
    }

    public GameInfo readGameInfo() throws IOException {
        Message message = readMessage();

        if (message == null || message.getCommand() != GAME_INFO)
            throw new IOException("Game information expected.");

        return parseGameInfo(message);
    }

    public GameInfo parseGameInfo(Message message) throws IOException {
        if (message == null || message.getCommand() != GAME_INFO)
            throw new IOException("Game information expected.");

        String[] values = message.getData().trim().split("\\s+");

        if (values.length != 5)
            throw new IOException("Invalid game information.");

        try {
            int version = Integer.parseInt(values[0]);

            if (version != PROTOCOL_VERSION)
                throw new IOException("Unsupported protocol version: " + version);

            int rows    = Integer.parseInt(values[1]);
            int cols    = Integer.parseInt(values[2]);
            int maxTime = Integer.parseInt(values[3]); 
            int side    = Integer.parseInt(values[4]);

            
            return new GameInfo(version, rows, cols, maxTime, side);

        } catch (NumberFormatException e) {
            throw new IOException("Invalid game information.", e);
        }
    }

    public Message readMessage() throws IOException {
        String line = input.readLine();

        if (line == null)
            return null;

        int separator = line.indexOf(' ');

        String commandText;
        String data;

        if (separator == -1) {
            commandText = line;
            data = "";
        } else {
            commandText = line.substring(0, separator);
            data = line.substring(separator + 1);
        }

        try {
            int command = Integer.parseInt(commandText);
            return new Message(command, data);

        } catch (NumberFormatException e) {
            throw new IOException(
                    "Invalid server message: " + line, e);
        }
    }

    public void sendMove(String move) throws IOException {
        output.write(move);
        output.newLine();
        output.flush();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    public static class Message {
        private final int command;
        private final String data;

        public Message(int command, String data) {
            this.command = command;
            this.data = data;
        }

        public int getCommand() {
            return command;
        }

        public String getData() {
            return data;
        }
    }

    public static class GameInfo {
        private final int protocolVersion;
        private final int rows;
        private final int cols;
        private final int maxMoveTime;
        private final int side;

        public GameInfo(int protocolVersion, int rows, int cols, int maxMoveTime, int side) {
            this.protocolVersion = protocolVersion;
            this.rows = rows;
            this.cols = cols;
            this.maxMoveTime = maxMoveTime;
            this.side = side;
        }

        public int getProtocolVersion() {
            return protocolVersion;
        }

        public int getRows() {
            return rows;
        }

        public int getCols() {
            return cols;
        }

        public int getMaxMoveTime(){
            return maxMoveTime;
        }

        public int getSide(){
            return side;
        }
    }
}

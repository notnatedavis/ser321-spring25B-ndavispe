package server;

import buffers.RequestProtos.*;
import buffers.ResponseProtos.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

class SockBaseServer {
    private String logFilename = "logs.txt";

    // Please use these as given so it works with our test cases
    static String menuOptions = "\nWhat would you like to do? \n 1 - to see the leader board \n 2 - to enter a game \n 3 - quit the game";
    static String gameOptions = "\nChoose an action: \n (1-9) - Enter an int to specify the row you want to update \n c - Clear number \n r - New Board";

    // make final ?
    ServerSocket serv = null;
    InputStream in = null;
    OutputStream out = null;
    Socket clientSocket = null;
    private final int id; // client id

    Game game; // current game

    private String name; // player name

    // server states
    private static final int STATE_NAME = 1;
    private static final int STATE_MENU = 2;
    private static final int STATE_INGAME = 3;

    // 1 -> NAME, 2 -> MENU, 3 -> INGAME
    private int currentState = STATE_NAME;
    private boolean inGame = false;

    private static boolean grading = true; // if the grading board should be used

    public SockBaseServer(Socket sock, Game game, int id) {
        this.clientSocket = sock;
        this.game = game;
        this.id = id;
        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();
        } catch (Exception e){
            System.out.println("Error in constructor: " + e);
        }
    }

    /**
     * Received a request, starts to evaluate what it is and handles it, not complete
     */
    public void runGameLoop() throws IOException {
        try {
            while (true) { // MAIN GAME LOOP
                // read the proto object and put into new object
                Request op = Request.parseDelimitedFrom(in);
                System.out.println("Got request: " + op.toString());
                Response response;
                boolean quit = false;

                // prefilter
                if (currentState == STATE_INGAME) {
                    switch (op.getOperationType()) {
                        case UPDATE :
                        case CLEAR :
                        case QUIT :
                            break;
                        default :
                            response = error(2, op.getOperationType().name());
                            response.writeDelimitedTo(out);
                            continue; // skip to next loop
                    }
                }

                // should handle all the other request types here, my advice is to put them in methods similar to nameRequest()
                switch (op.getOperationType()) { // REQUEST ROUTER
                    case NAME :
                        if (op.getName().isBlank()) {
                            response = error(1, "name");
                        } else {
                            response = nameRequest(op); // player id
                        }
                        break;
                    case LEADERBOARD :
                        response = buildLeaderboardResponse();
                        break;
                    case START :
                        response = handleStartRequest(op); // implement
                        break;
                    case UPDATE :
                        response = handleUpdateRequest(op); // implement
                        break;
                    case CLEAR :
                        response = handleClearRequest(op); // implement
                        break;
                    case QUIT :
                        response = quit();
                        quit = true;
                        break;
                    default :
                        response = error(2, op.getOperationType().name());
                        break;
                }
                response.writeDelimitedTo(out);

                if (quit) {
                    return; // break ?
                }
            }
        } catch (SocketException se) {
            System.out.println("Client disconnected");
        } catch (Exception ex) {
            Response error = error(0, "Unexpected server error: " + ex.getMessage());
            error.writeDelimitedTo(out);
        } finally {
            // cleanup code
            System.out.println("Client ID " + id + " disconnected");
            this.inGame = false;
            exitAndClose(in, out, clientSocket);
        }
    }

    void exitAndClose(InputStream in, OutputStream out, Socket serverSock) throws IOException {
        if (in != null)   in.close();
        if (out != null)  out.close();
        if (serverSock != null) serverSock.close();
    }

    // Leaderboard Logic
    private Response buildLeaderboardResponse() throws IOException {
        // read logs + aggregate stats
        Map<String, Integer> scoreMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" : ", 2);
                if (parts.length != 2) continue;
                String player = parts[0].trim();
                try {
                    int pts = Integer.parseInt(parts[1].trim());
                    scoreMap.put(player, pts);
                } catch (NumberFormatException e) {
                    // skip malformed
                }
            }
        }

        // convert to sorted Entry list
        List<Entry> entries = scoreMap.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(e -> Entry.newBuilder()
                        .setName(e.getKey())
                        .setLogins(0) // keep ?
                        .setPoints(e.getValue())
                        .build())
                .collect(Collectors.toList());

        return Response.newBuilder()
                .setResponseType(Response.ResponseType.LEADERBOARD)
                .addAllLeader(entries)
                .setMenuoptions(menuOptions)
                .setNext(STATE_MENU)  // Return to main menu
                .build();
    }

    // helper class for stats
    private static class PlayerStats {
        int wins = 0;
        int logins = 0;
        int points = 0;
    }

    // Update Move Logic
    private Response handleUpdateRequest(Request op) throws IOException {
        // validate presence of required fields
        if (!op.hasRow() || !op.hasColumn() || !op.hasValue()) {
            return error(1, "Missing fields in UPDATE");
        }

        // convert to 0 based
        int row = op.getRow() - 1;
        int col = op.getColumn() - 1;
        int value = op.getValue();

        // validate bounds
        if (row < 0 || row >= 9 || col < 0 || col >= 9) {
            return error(3, "Row/column out of bounds (1-9)");
        }
        if (value < 1 || value > 9) {
            return error(3, "Value must be 1-9");
        }

        // check if cell is modifiable (when reference board has 'X')
        if (game.isPreset(row, col)) {
            return Response.newBuilder()
                    .setResponseType(Response.ResponseType.ERROR)
                    .setErrorType(1)
                    .setMessage("Cannot modify preset cell")
                    .setBoard(game.getDisplayBoard())
                    .setNext(STATE_INGAME)
                    .build();
        }

        // attempt the move
        int result = game.updateBoard(row, col, value, 0);

        // scoring
        if (result == 0) {
            game.setPoints(+2);
        } else {
            game.setPoints(-2);
        }

        // build response
        Response.Builder response = Response.newBuilder()
                .setBoard(game.getDisplayBoard())
                .setPoints(game.getPoints())
                .setType(mapEvalType(result));

        // determine response type
        if (game.getWon()) {
            // writeToLog(name, Message.WIN);
            game.setPoints(+20);
            // overwrite the response's points field
            response.setPoints(game.getPoints());

            // append points line to same file
            try (FileWriter fw = new FileWriter(logFilename, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter pw = new PrintWriter(bw)) {
                pw.printf("%s : %d%n", name, game.getPoints());
            } catch (IOException e) {
                System.out.println("Failed to write score log");
            }

            inGame = false; // game over
            currentState = STATE_MENU;
            response.setResponseType(Response.ResponseType.WON)
                    .setMessage("You won")
                    .setNext(STATE_MENU); // back to main menu
        } else {
            response.setResponseType(Response.ResponseType.PLAY)
                    .setNext(STATE_INGAME); // stay in game
        }
        return response.build();
    }

    // map game result to Protobuf EvalType
    private Response.EvalType mapEvalType(int result) {
        switch (result) {
            case 1 :
                return Response.EvalType.PRESET_VALUE;
            case 2 :
                return Response.EvalType.DUP_ROW;
            case 3 :
                return Response.EvalType.DUP_COL;
            case 4 :
                return Response.EvalType.DUP_GRID;
            default :
                return Response.EvalType.UPDATE;
        }
    }

    // Clear Operations
    private Response handleClearRequest(Request op) throws IOException {
        if (!op.hasValue()) {
            return error(1, "Missing clear type");
        }

        int clearType = op.getValue();
        int row = op.hasRow() ? op.getRow() - 1 : -1;
        int col = op.hasColumn() ? op.getColumn() - 1 : -1;

        // validate clear type
        if (clearType < 1 || clearType > 6) {
            return error(2, "Invalid clear type");
        }

        if (clearType == 6) {
            // reset board logic, create a new board with same difficulty
            int currentDifficulty = game.getDifficulty(); // verify this works
            game.newGame(grading, currentDifficulty); // regenerate with same difficulty
        } else {
            // regular clear operation
            game.updateBoard(row, col, 0, clearType); // type 1-5
            game.setPoints(-5);
        }

        return Response.newBuilder()
                .setResponseType(Response.ResponseType.PLAY)
                .setBoard(game.getDisplayBoard())
                .setPoints(game.getPoints())
                .setType(mapClearType(clearType))
                .setNext(STATE_INGAME)
                .build();
    }

    // map clear type to Protobuf EvalType
    private Response.EvalType mapClearType(int type) {
        switch (type) {
            case 1 :
                return Response.EvalType.CLEAR_VALUE;
            case 2 :
                return Response.EvalType.CLEAR_ROW;
            case 3 :
                return Response.EvalType.CLEAR_COL;
            case 4 :
                return Response.EvalType.CLEAR_GRID;
            case 5 :
                return Response.EvalType.CLEAR_BOARD;
            case 6 :
                return Response.EvalType.RESET_BOARD;
            default :
                return Response.EvalType.CLEAR_VALUE;
        }
    }

    private Response handleStartRequest(Request op) throws IOException {
        // check if difficulty is provided
        if (!op.hasDifficulty()) {
            return error(1, "difficulty");
        }
        int difficulty = op.getDifficulty();

        // validate difficulty
        if (difficulty < 1 || difficulty > 20) {
            return error(5, "difficulty out of range (1-20)");
        }

        // initialize game with parsed difficulty
        game.newGame(grading, difficulty);

        inGame = true;
        currentState = STATE_INGAME;

        // build response with the new board
        return Response.newBuilder()
                .setResponseType(Response.ResponseType.START)
                .setBoard(game.getDisplayBoard())
                .setMenuoptions(gameOptions) // use in-game menu options
                .setNext(STATE_INGAME) // transition to in-game state
                .build();
    }

    /**
     * Handles the name request and returns the appropriate response
     * @return Request.Builder holding the reponse back to Client as specified in Protocol
     */
    private Response nameRequest(Request op) throws IOException {
        name = op.getName();

        writeToLog(name, Message.CONNECT);
        inGame = false;
        currentState = STATE_MENU;

        System.out.println("Got a connection and a name: " + name);
        return Response.newBuilder()
                .setResponseType(Response.ResponseType.GREETING)
                .setMessage("Hello " + name + " and welcome to a simple game of Sudoku.")
                .setMenuoptions(menuOptions)
                .setNext(STATE_MENU)
                .build();
    }

    /**
     * Handles the quit request, might need adaptation
     * @return Request.Builder holding the reponse back to Client as specified in Protocol
     */
    private Response quit() throws IOException {
        inGame = false;
        currentState = STATE_MENU;
        return Response.newBuilder()
                .setResponseType(Response.ResponseType.BYE)
                .setMessage("Thank you for playing! goodbye.")
                .build();
    }

    /**
     * Start of handling errors, not fully done
     * @return Request.Builder holding the reponse back to Client as specified in Protocol
     */
    private Response error(int err, String field) throws IOException {
        String message;

        switch (err) {
            case 1 :
                message = "\nError: required field [" + field + "] missing or empty";
                break;
            case 2 :
                message = "\nError: request not supported";
                break;
            case 3 :
                message = "\nError: invalid " + field;
                break;
            case 5 :
                message = "Error: difficulty must be between 1 and 20";
            default :
                message = "\nError: cannot process your request";
                break;
        }

        return Response.newBuilder()
                .setResponseType(Response.ResponseType.ERROR)
                .setErrorType(err)
                .setMessage(message)
                .setNext(currentState)
                .build();
    }
    
    /**
     * Writing a new entry to our log
     * @param name - Name of the person logging in
     * @param message - type Message from Protobuf which is the message to be written in the log (e.g. Connect) 
     * @return String of the new hidden image
     */
    public void writeToLog(String name, Message message) {
        try {
            // read old log file
            Logs.Builder logs = readLogFile();

            Date date = java.util.Calendar.getInstance().getTime();

            // we are writing a new log entry to our log
            // add a new log entry to the log list of the Protobuf object
            logs.addLog(date + ": " +  name + " - " + message);

            // open log file
            FileOutputStream output = new FileOutputStream(logFilename);
            Logs logsObj = logs.build();

            // write to log file
            logsObj.writeTo(output);
        } catch(Exception e) {
            System.out.println("Issue while trying to save");
        }
    }

    /**
     * Reading the current log file
     * @return Logs.Builder a builder of a logs entry from protobuf
     */
    public Logs.Builder readLogFile() throws IOException {
        Logs.Builder logs = Logs.newBuilder();
        File logFile = new File(logFilename);

        try {
            if (logFile.exists()) {
                logs.mergeFrom(new FileInputStream(logFile));
            } else {
                logFile.createNewFile(); // create empty file if missing
            }
        } catch (Exception e) {
            System.out.println("Error reading logs: " + e.getMessage());
        }
        return logs;
    }

    // main entry point
    public static void main (String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Expected arguments: <port(int)> <delay(int)>");
            System.exit(1);
        }
        int port = 8000; // default port
        grading = Boolean.parseBoolean(args[1]);
        Socket clientSocket = null;
        ServerSocket socket = null;

        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port|sleepDelay] must be an integer");
            System.exit(2);
        }
        try {
            socket = new ServerSocket(port);
            System.out.println("Server started..");
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
        int id = 1;
        while (true) {
            try {
                clientSocket = socket.accept();
                System.out.println("Attempting to connect to client-" + id);
                Game game = new Game();
                SockBaseServer server = new SockBaseServer(clientSocket, game, id++);
                server.runGameLoop();
            } catch (Exception e) {
                System.out.println("Error in accepting client connection.");
            }
        }
    }
}

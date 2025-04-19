package client;

import buffers.RequestProtos.*;
import buffers.ResponseProtos.*;

import java.io.*;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 *  Class: SockBaseClient
 *  Description: an extension of the existing Player.java class
 */
class SockBaseClient {
    private static final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
    // sync class for multithreading
    private static synchronized String readLine() throws IOException {
        return stdin.readLine();
    }
    /**
     * Main Entry Point
     *
     * @param args
     * @throws Exception
     */
    public static void main (String[] args) throws Exception {
        // server setup
        Socket serverSock = null;
        OutputStream out = null;
        InputStream in = null;
        int port = 8000; // default port

        // 2 args
        if (args.length != 2) {
            System.out.println("Expected args: <host(String)> <port(int)>");
            System.exit(1);
        }
        String host = args[0];
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be integer");
            System.exit(2);
        }

        // build obj request w/ name
        Request op = nameRequest().build();
        Response response;
        try {
            // connect to the server
            serverSock = new Socket(host, port);

            // write to the server
            out = serverSock.getOutputStream();
            in = serverSock.getInputStream();

            op.writeDelimitedTo(out); // proto

            while (true) { // MAIN GAME LOOP
                // read from the server
                response = Response.parseDelimitedFrom(in);
                System.out.println("got response : [" + response.toString() + "]");

                Request.Builder req = Request.newBuilder();

                switch (response.getResponseType()) {
                    case GREETING :
                        System.out.println(response.getMessage());
                        req = chooseMenu(req, response);
                        break;
                    case START :
                    case PLAY :
                        System.out.println("\nCurrent Board :\n" + response.getBoard());
                        if (response.hasType()) {
                            switch (response.getType()) {
                                case PRESET_VALUE :
                                    System.out.println("Error: cannot modify preset cell");
                                    break;
                                case DUP_ROW :
                                    System.out.println("Error: duplicate in row");
                                    break;
                                case DUP_COL :
                                    System.out.println("Error: duplicate in column");
                                    break;
                                case DUP_GRID :
                                    System.out.println("Error: duplicate in grid");
                                    break;
                            }
                        }
                        req = gameMenu(req); // core game logic entry
                        break;
                    case LEADERBOARD :
                        System.out.println("\nLeaderboard: \n");
                        response.getLeaderList().forEach(entry ->
                                System.out.println(entry.getName() + ": " + entry.getPoints() + " wins"));
                        req = chooseMenu(req, response);
                        break;
                    case WON :
                        System.out.println(response.getMessage());
                        System.out.println("Final Board:\n" + response.getBoard());
                        req.setOperationType(Request.OperationType.QUIT);
                        break;
                    case ERROR :
                        System.out.println("Error: " + response.getMessage() + "Type: " + response.getErrorType());
                        if (response.getNext() == 1) {
                            req = nameRequest();
                        } else {
                            System.out.println("That error type is not handled yet");
                            req = nameRequest();
                        }
                        break;
                }
                req.build().writeDelimitedTo(out); // proto
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            exitAndClose(in, out, serverSock);
        }
    }

    /**
     * handles building a simple name requests, asks the user for their name and builds the request
     * @return Request.Builder which holds all the information for the NAME request
     */
    static Request.Builder nameRequest() throws IOException {
        System.out.println("Please provide your name for the server.");
        String strToSend = readLine();

        return Request.newBuilder()
                .setOperationType(Request.OperationType.NAME)
                .setName(strToSend);
    }

    /**
     * Shows the main menu and lets the user choose a number, it builds the request for the next server call
     * @return Request.Builder which holds the information the server needs for a specific request
     */
    static Request.Builder chooseMenu(Request.Builder req, Response response) throws IOException {
        while (true) { // exit ?
            System.out.println(response.getMenuoptions());
            System.out.print("Enter a number 1-3: ");
            String menu_select = readLine();

            switch (menu_select) {
                case "1" :
                    // leaderboard
                    req.setOperationType(Request.OperationType.LEADERBOARD);
                    return req;
                case "2" :
                    // start new game
                    System.out.print("Enter difficulty (1-20): ");
                    String diffInput = readLine();

                    int difficulty;

                    try {
                        difficulty = Integer.parseInt(diffInput);
                        if (difficulty < 1 || difficulty > 20) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid difficulty. Please enter a number between 1 and 20.");
                        continue;
                    }

                    req.setOperationType(Request.OperationType.START)
                            .setDifficulty(difficulty);
                    return req;
                case "3" :
                    // quit
                    req.setOperationType(Request.OperationType.QUIT);
                    return req;
                default:
                    System.out.println("\nNot a valid choice, please choose again");
                    break;  // -> menu
            }
        }
    }

    static Request.Builder gameMenu(Request.Builder req) throws IOException {
        System.out.println(gameOptions);
        String input = readLine();

        if (input.equalsIgnoreCase("c")) {
            int[] clearParams = boardSelectionClear();
            req.setOperationType(Request.OperationType.CLEAR)
                    .setRow(clearParams[0])
                    .setColumn(clearParams[1])
                    .setValue(clearParams[2]);
        } else if (input.equalsIgnoreCase("r")) {
            req.setOperationType(Request.OperationType.CLEAR)
                    .setValue(6); // new board
        } else {
            // main game input handling
            // handle numeric input for moves
            int row = Integer.parseInt(input) - 1; // 0 based
            System.out.print("Enter column (1-9): ");
            int col = Integer.parseInt(readLine()) - 1;
            System.out.print("Enter value (1-9): ");
            int val = Integer.parseInt(readLine());

            // back to 1 based
            req.setOperationType(Request.OperationType.UPDATE)
                    .setRow(row + 1)
                    .setColumn(col + 1)
                    .setValue(val);
        }
        return req;
    }

    /**
     * Exits the connection
     */
    static void exitAndClose(InputStream in, OutputStream out, Socket serverSock) throws IOException {
        // resource cleanup
        if (in != null)   in.close();
        if (out != null)  out.close();
        if (serverSock != null) serverSock.close();
        System.exit(0);
    }

    /**
     * Handles the clear menu logic when the user chooses that in Game menu. It retuns the values exactly
     * as needed in the CLEAR request row int[0], column int[1], value int[3]
     */
    static int[] boardSelectionClear() throws IOException {
        System.out.println("Choose what kind of clear by entering an integer (1 - 5)");
        System.out.print(" 1 - Clear value \n 2 - Clear row \n 3 - Clear column \n 4 - Clear Grid \n 5 - Clear Board \n");

        String selection = readLine();

        while (true) { // exit ?
            if (selection.equalsIgnoreCase("exit")) {
                return new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            }
            try {
                int temp = Integer.parseInt(selection);

                if (temp < 1 || temp > 5) {
                    throw new NumberFormatException();
                }

                break;
            } catch (NumberFormatException nfe) {
                System.out.println("That's not an integer!");
                System.out.println("Choose what kind of clear by entering an integer (1 - 5)");
                System.out.print("1 - Clear value \n 2 - Clear row \n 3 - Clear column \n 4 - Clear Grid \n 5 - Clear Board \n");
            }
            selection = readLine();
        }

        int[] coordinates = new int[3];

        switch (selection) {
            case "1":
                // clear value, so array will have {row, col, 1}
                coordinates = boardSelectionClearValue();
                break;
            case "2":
                // clear row, so array will have {row, -1, 2}
                coordinates = boardSelectionClearRow();
                break;
            case "3":
                // clear col, so array will have {-1, col, 3}
                coordinates = boardSelectionClearCol();
                break;
            case "4":
                // clear grid, so array will have {gridNum, -1, 4}
                coordinates = boardSelectionClearGrid();
                break;
            case "5":
                // clear entire board, so array will have {-1, -1, 5}
                coordinates[0] = -1;
                coordinates[1] = -1;
                coordinates[2] = 5;
                break;
            default:
                break;
        }

        return coordinates;
    }

    static int[] boardSelectionClearValue() throws IOException {
        int[] coordinates = new int[3];

        System.out.println("Choose coordinates of the value you want to clear");
        System.out.print("Enter the row as an integer (1 - 9): ");
        String row = readLine();

        while (true) {
            if (row.equalsIgnoreCase("exit")) {
                return new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            }
            try {
                Integer.parseInt(row);
                break;
            } catch (NumberFormatException nfe) {
                System.out.println("That's not an integer!");
                System.out.print("Enter the row as an integer (1 - 9): ");
            }
            row = readLine();
        }

        coordinates[0] = Integer.parseInt(row);

        System.out.print("Enter the column as an integer (1 - 9): ");
        String col = readLine();

        while (true) {
            if (col.equalsIgnoreCase("exit")) {
                return new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            }
            try {
                Integer.parseInt(col);
                break;
            } catch (NumberFormatException nfe) {
                System.out.println("That's not an integer!");
                System.out.print("Enter the column as an integer (1 - 9): ");
            }
            col = readLine();
        }

        coordinates[1] = Integer.parseInt(col);
        coordinates[2] = 1;

        return coordinates;
    }

    static int[] boardSelectionClearRow() throws IOException {
        int[] coordinates = new int[3];

        System.out.println("Choose the row you want to clear");
        System.out.print("Enter the row as an integer (1 - 9): ");
        String row = readLine();

        while (true) {
            if (row.equalsIgnoreCase("exit")) {
                return new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            }
            try {
                Integer.parseInt(row);
                break;
            } catch (NumberFormatException nfe) {
                System.out.println("That's not an integer!");
                System.out.print("Enter the row as an integer (1 - 9): ");
            }
            row = readLine();
        }

        coordinates[0] = Integer.parseInt(row);
        coordinates[1] = -1;
        coordinates[2] = 2;

        return coordinates;
    }

    static int[] boardSelectionClearCol() throws IOException {
        int[] coordinates = new int[3];

        System.out.println("Choose the column you want to clear");
        System.out.print("Enter the column as an integer (1 - 9): ");
        String col = readLine();

        while (true) {
            if (col.equalsIgnoreCase("exit")) {
                return new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            }
            try {
                Integer.parseInt(col);
                break;
            } catch (NumberFormatException nfe) {
                System.out.println("That's not an integer!");
                System.out.print("Enter the column as an integer (1 - 9): ");
            }
            col = readLine();
        }

        coordinates[0] = -1;
        coordinates[1] = Integer.parseInt(col);
        coordinates[2] = 3;
        return coordinates;
    }

    static int[] boardSelectionClearGrid() throws IOException {
        int[] coordinates = new int[3];

        System.out.println("Choose area of the grid you want to clear");
        System.out.println(" 1 2 3 \n 4 5 6 \n 7 8 9 \n");
        System.out.print("Enter the grid as an integer (1 - 9): ");
        String grid = readLine();

        while (true) {
            if (grid.equalsIgnoreCase("exit")) {
                return new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            }
            try {
                Integer.parseInt(grid);
                break;
            } catch (NumberFormatException nfe) {
                System.out.println("That's not an integer!");
                System.out.print("Enter the grid as an integer (1 - 9): ");
            }
            grid = readLine();
        }

        coordinates[0] = Integer.parseInt(grid);
        coordinates[1] = -1;
        coordinates[2] = 4;

        return coordinates;
    }

    static String gameOptions = "\nChoose an action: \n (1-9) - Enter an int to specify the row you want to update \n c - Clear number \n r - New Board";
}

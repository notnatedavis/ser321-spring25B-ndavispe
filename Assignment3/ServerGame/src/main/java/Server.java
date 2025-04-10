import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.nio.file.Path; // for file finding
import java.nio.file.Paths; // for file finding
import java.util.Comparator;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.Timer;

import javax.imageio.ImageIO;

/**
 * Server Class
 * Handles all game logic, state management, and client communication
 */
public class Server {
  // Game State tracking
  // leaderboard maps and usernames
  static Map<String, Integer> leaderboard = new HashMap<String, Integer>();
  // list of movie titles available for guessing
  static List<String> movies = List.of("jaws", "joker", "moonlight", "the_godfather", "the_thing", "titanic", "us");
  // map client sockets to active game sessions
  static Map<Socket, GameState> activeGames = new HashMap<>();

  // network components
  static int port = 8888;

  /**
   * GameState Class
   * Handles maintaining complete game state for each client connection
   */
  static class GameState {
    String username;
    String targetMovie;
    int attemptsLeft = 3;
    boolean gameActive;
    String currentState; // "lobby", "running", "won", "lost", "inactive", "time_up"
    int currentClarity = 5; // start with least clear (5 = most pixelated, 1 = clearest)
    int skipsRemaining = 0; // set based on game duration
    long gameEndTime = 0; // (timestamp)
    Timer gameTimer = null; // timer for game duration
    String gameDuration = ""; // "short", "medium", "long"

    // constants for image clarity
    final int MIN_CLARITY = 5; // starting value, most pixelated
    final int MAX_CLARITY = 1; // lowest numeric value, clearest

    /**
     * Constructor to initialize a new game state
     *
     * @param username
     * @param targetMovie
     * @param duration
     */
    public GameState(String username, String targetMovie, String duration) {
      this.username = username;
      this.targetMovie = targetMovie;
      this.gameActive = true;
      this.currentState = "running";
      this.gameDuration = duration;
      // start with most pixelated
      this.currentClarity = MIN_CLARITY;

      // set skips based on duration
      switch (duration.toLowerCase()) {
        case "short":
          this.skipsRemaining = 2;
          // 30 seconds
          this.gameEndTime = System.currentTimeMillis() + 30000;
          break;
        case "medium":
          this.skipsRemaining = 4;
          // 60 seconds
          this.gameEndTime = System.currentTimeMillis() + 60000;
          break;
        case "long":
          this.skipsRemaining = 6;
          // 90 seconds
          this.gameEndTime = System.currentTimeMillis() + 90000;
          break;
      }

      // (grading)
      System.out.println("Movie to guess: " + targetMovie);
    }

    /**
     * Reset game state for a new round
     *
     * @param newTargetMovie
     * @param duration
     */
    public void reset(String newTargetMovie, String duration) {
      this.targetMovie = newTargetMovie;
      this.attemptsLeft = 3;
      this.gameActive = true;
      this.currentState = "running";
      this.currentClarity = MIN_CLARITY;
      this.gameDuration = duration;

      // cancel existing timer if any
      if (this.gameTimer != null) {
        this.gameTimer.cancel();
      }

      // set skips based on duration
      switch (duration.toLowerCase()) {
        case "short":
          this.skipsRemaining = 2;
          // 30 seconds
          this.gameEndTime = System.currentTimeMillis() + 30000;
          break;
        case "medium":
          this.skipsRemaining = 4;
          // 60 seconds
          this.gameEndTime = System.currentTimeMillis() + 60000;
          break;
        case "long":
          this.skipsRemaining = 6;
          // 90 seconds
          this.gameEndTime = System.currentTimeMillis() + 90000;
          break;
      }

      // (grading)
      System.out.println("Movie to guess: " + targetMovie);
    }

    /**
     * Ends game by marking inactive + setting state
     *
     * @param result
     */
    public void endGame(String result) {
      this.gameActive = false;
      this.currentState = result;

      // cancel timer if it exists
      if (this.gameTimer != null) {
        this.gameTimer.cancel();
        this.gameTimer = null;
      }
    }

    // decreases clarity to show a clearer image
    public boolean decreaseClarity() {
      if (currentClarity > 1) {
        currentClarity--;
        return true;
      }
      return false; // cant decrease clarity past 1
    }

    // returns whether the game timer has expired
    public boolean isGameExpired() {
      return System.currentTimeMillis() > gameEndTime;
    }

    // returns remaining game time in seconds
    public int getRemainingTime() {
      long remaining = gameEndTime - System.currentTimeMillis();
      return (int) Math.max(0, remaining / 1000); // convert to seconds
    }

    public int calculatePoints() {
      int basePoints = currentClarity;
      int timePoints = 0;

      switch (gameDuration.toLowerCase()) {
        case "short": // 30 seconds max
          timePoints = getRemainingTime() / 5; // Up to 6 bonus points
          break;
        case "medium": // 60 seconds max
          timePoints = getRemainingTime() / 10; // Up to 6 bonus points
          break;
        case "long": // 90 seconds max
          timePoints = getRemainingTime() / 15; // Up to 6 bonus points
          break;
      }

      return basePoints + timePoints;
    }
  }

  /**
   * Main Entry Point
   * Handles setting up server socket and handles incoming client connections
   *
   * @param args
   */
  public static void main(String args[]) {
    // port handling
    if (args.length != 1) {
      System.out.println("Expected arguments: <port(int)>");
      System.exit(1);
    }
    try {
      port = Integer.parseInt(args[0]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port|sleepDelay] must be an integer");
      System.exit(2);
    }

    try {
      //open socket
      ServerSocket serv = new ServerSocket(port);
      System.out.println("Server ready for connections");

      // loop accepting one client and one request
      while (true) {
        Socket clientSocket = serv.accept();
        System.out.println("Client connected");
        new Thread(() -> handleClient(clientSocket)).start(); // handles ...
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * handleClient method
   * Handles managing the client connection and communication loop
   *
   * @param clientSocket
   */
  private static void handleClient(Socket clientSocket) {
    try {
      InputStream in = clientSocket.getInputStream();
      OutputStream out = clientSocket.getOutputStream();

      // update flag
      boolean connected = true;

      while (connected) {
        byte[] requestBytes = receive(in);
        JSONObject request = fromByteArray(requestBytes);
        JSONObject response = processRequest(request, clientSocket);

        // check if client is disconnecting
        if (request.has("type") && request.getString("type").equals("quit")) {
          connected = false;
        }

        // send response
        send(out, toByteArray(response));
      }
    } catch (Exception e) {
      System.out.println("Client disconnected");
      e.printStackTrace();
    } finally {
      GameState game = activeGames.get(clientSocket);
      if (game != null && game.gameTimer != null) {
        game.gameTimer.cancel();
      }
      activeGames.remove(clientSocket);
      try {
        clientSocket.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * processRequest method
   * Handles routing client requests to appropriate handlers
   *
   * @param request
   * @param clientSocket
   * @return
   * @throws IOException
   */
  private static JSONObject processRequest(JSONObject request, Socket clientSocket) throws IOException {
    if (!request.has("type")) {
      return errorResponse("Missing message type");
    }

    String type = request.getString("type");

    // check for expired game time (unless the client is quitting).
    GameState game = activeGames.get(clientSocket);
    if (game != null && game.gameActive && game.isGameExpired() && !type.equals("quit")) {
      game.endGame("time_up");
      JSONObject timeResponse = new JSONObject();
      timeResponse.put("type", "result");
      timeResponse.put("result_details", "Time is up! The movie was: " + game.targetMovie);
      timeResponse.put("game_state", "time_up");
      JSONArray actions = new JSONArray();
      actions.put("start");
      actions.put("leaderboard");
      actions.put("quit");
      timeResponse.put("available_actions", actions);
      return timeResponse;
    }

    switch (type.toLowerCase()) {
      case "join":
        return handleJoin(request);
      case "start":
        return handleStart(request, clientSocket);
      case "guess":
        return handleGuess(request, clientSocket);
      case "next":
        return handleNext(clientSocket);
      case "skip":
        return handleSkip(clientSocket);
      case "remaining":
        return handleRemaining(clientSocket);
      case "quit":
        return handleQuit(clientSocket);
      case "leaderboard":
        return getLeaderboard();
      case "state":
        return getGameState(clientSocket);
      default:
        return errorResponse("Unsupported message type: " + type);
    }
  }

  /**
   * Retrieves game state for client
   *
   * @param clientSocket
   * @return
   * @throws IOException
   */
  private static JSONObject getGameState(Socket clientSocket) throws IOException {
    GameState game = activeGames.get(clientSocket);
    if (game == null) {
      return errorResponse("No active game");
    }
    return createGameStateResponse(game, clientSocket);
  }

  /**
   * Constructs game state JSON response
   *
   * @param game
   * @param clientSocket
   * @return
   * @throws IOException
   */
  private static JSONObject createGameStateResponse(GameState game, Socket clientSocket) throws IOException {
    JSONObject response = new JSONObject();
    response.put("type", "state");
    response.put("game_state", game.currentState);
    response.put("time_remaining", game.getRemainingTime());
    if (game.gameActive) {
      response.put("attempts", game.attemptsLeft);
      response.put("current_points", game.currentClarity); // Add current potential points

      // get the image for the current clarity level
      String imageFileName = game.targetMovie + game.currentClarity + ".png";
      Path imagePath = Paths.get("src", "main", "imgs", "movies", imageFileName).toAbsolutePath();
      File file = imagePath.toFile();

      System.out.println("Looking for image at: " + file.getAbsolutePath());
      System.out.println("Image exists: " + file.exists());

      if (!file.exists()) {
        return errorResponse("Movie image not found: " + imageFileName);
      }

      BufferedImage img = ImageIO.read(file);
      ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
      ImageIO.write(img, "png", byteStream);
      String base64img = Base64.getEncoder().encodeToString(byteStream.toByteArray());

      response.put("image_data", base64img);
      response.put("message", "Guess the movie title. Current value: " + game.currentClarity + " points");
      JSONArray actions = new JSONArray();
      actions.put("guess");
      if (game.currentClarity > game.MAX_CLARITY) {
        actions.put("next");
      }
      if (game.skipsRemaining > 0) {
        actions.put("skip");
      }
      actions.put("remaining");
      actions.put("quit");
      response.put("available_actions", actions);
    } else {
      // add available actions
      JSONArray actions = new JSONArray();
      actions.put("start");
      actions.put("leaderboard");
      actions.put("quit");
      response.put("available_actions", actions);
    }

    return response;
  }

  /**
   * handleJoin method
   * Handles registering a new user or returning existing user to lobby state
   *
   * @param request
   * @return
   */
  private static JSONObject handleJoin(JSONObject request) {
    if (!request.has("username")) {
      return errorResponse("Missing username");
    }

    String username = request.getString("username");
    leaderboard.putIfAbsent(username, 0);

    JSONObject response = new JSONObject();
    response.put("type", "state");
    response.put("game_state", "lobby");
    response.put("message", "Welcome " + username + "! You are in the lobby");

    // add options for client
    JSONArray actions = new JSONArray();
    actions.put("start");
    actions.put("leaderboard");
    actions.put("quit");
    response.put("available_actions", actions);

    return response;
  }

  /**
   * handleStart method
   * Handles initializing a new game session with random movie selection
   *
   * @param request
   * @param clientSocket
   * @return
   * @throws IOException
   */
  private static JSONObject handleStart(JSONObject request, Socket clientSocket) throws IOException {
    // remove any existing game state
    activeGames.remove(clientSocket);

    if (!request.has("username")) {
      return errorResponse("Missing username");
    }

    String username = request.getString("username");
    // generate random movie from server's list instead of receiving from client
    String targetMovie = movies.get(new Random().nextInt(movies.size()));

    // log movie selection
    System.out.println("Serving image for : " + targetMovie);

    // check if duration exists, default "medium" if not
    String duration = "medium";
    if (request.has("duration")) {
      duration = request.getString("duration");
    }

    // create new game state
    GameState game = new GameState(username, targetMovie, duration);
    activeGames.put(clientSocket, game);

    // return game state which includes image at current clarity level
    return createGameStateResponse(game, clientSocket);
  }

  /**
   * handleGuess method
   * Handles processing a player's guess and updating game state
   *
   * @param request
   * @param clientSocket
   * @return
   */
  private static JSONObject handleGuess(JSONObject request, Socket clientSocket) {
    GameState game = activeGames.get(clientSocket);
    if (game == null || !game.gameActive) {
      return errorResponse("No active game");
    }

    if (game.isGameExpired()) {
      game.endGame("time_up");
      JSONObject resp = new JSONObject();
      resp.put("type", "result");
      resp.put("result_details", "Time up! The movie was: " + game.targetMovie);
      resp.put("game_state", "time_up");
      JSONArray actions = new JSONArray();
      actions.put("start");
      actions.put("leaderboard");
      actions.put("quit");
      resp.put("available_actions", actions);
      return resp;
    }
    if (!request.has("title")) {
      return errorResponse("Missing guess title");
    }
    String guess = request.getString("title").toLowerCase();
    JSONObject response = new JSONObject();
    JSONArray actions = new JSONArray();
    if (guess.equals(game.targetMovie)) {
      // add points to leaderboard
      int pointsAwarded = game.calculatePoints();
      updateLeaderboard(game.username, pointsAwarded);

      response.put("type", "result");
      response.put("result_details", "Correct! The movie was: " + game.targetMovie);

      // reset the game with a new random movie.
      String newMovie = movies.get(new Random().nextInt(movies.size()));
      game.targetMovie = newMovie;
      game.attemptsLeft = 3;
      game.currentClarity = game.MIN_CLARITY;
      game.currentState = "running";

      // (grading)
      System.out.println("New movie to guess: " + game.targetMovie);

      // prepare next round
      actions.put("guess");
      if (game.currentClarity > game.MAX_CLARITY) {
        actions.put("next");
      }
      if (game.skipsRemaining > 0) {
        actions.put("skip");
      }
      actions.put("remaining");
      actions.put("quit");
      response.put("available_actions", actions);

      // include new game state.
      try {
        return createGameStateResponse(game, clientSocket).put("result_details", response.getString("result_details"));
      } catch (IOException e) {
        e.printStackTrace();
        return errorResponse("Error updating game state after correct guess");
      }
    } else {
      // incorrect guess
      game.attemptsLeft--;

      if (game.attemptsLeft > 0) {
        // still has attempts
        response.put("type", "result");
        response.put("result_details", "Incorrect guess. Try again!");
        response.put("attempts_left", game.attemptsLeft);
        response.put("game_state", "running");

        // add available actions
        actions.put("guess");
        if (game.currentClarity > game.MAX_CLARITY) {
          actions.put("next");
        }
        if (game.skipsRemaining > 0) {
          actions.put("skip");
        }

        actions.put("remaining");
        actions.put("quit");
        response.put("available_actions", actions);
      } else {
        // out of attempts
        game.endGame("lost");

        response.put("type", "result");
        response.put("result_details", "Game over! Out of attempts. The movie was: " + game.targetMovie);
        response.put("game_state", "lost");

        // add available actions
        actions.put("start");
        actions.put("leaderboard");
        actions.put("quit");
        response.put("available_actions", actions);
      }
    }

    return response;
  }

  /**
   * handleNext method
   * Handles processing next command to show a clearer img
   *
   * @param clientSocket
   * @return
   * @throws IOException
   */
  private static JSONObject handleNext(Socket clientSocket) throws IOException {
    GameState game = activeGames.get(clientSocket);
    if (game == null || !game.gameActive) {
      return errorResponse("No active game");
    }
    if (!game.decreaseClarity()) {
      return errorResponse("Already at clearest image");
    }
    return createGameStateResponse(game, clientSocket);
  }

  /**
   * handleSkip method
   * Handles processing 'skip' command to switch to a new random movie
   *
   * @param clientSocket
   * @return
   * @throws IOException
   */
  private static JSONObject handleSkip(Socket clientSocket) throws IOException {
    GameState game = activeGames.get(clientSocket);
    if (game == null || !game.gameActive) {
      return errorResponse("No active game");
    }
    if (game.skipsRemaining <= 0) {
      return errorResponse("No skips remaining");
    }
    game.skipsRemaining--;
    String newMovie = movies.get(new Random().nextInt(movies.size()));
    game.targetMovie = newMovie;
    game.attemptsLeft = 3;
    game.currentClarity = game.MIN_CLARITY;
    game.currentState = "running";

    return createGameStateResponse(game, clientSocket);
  }

  /**
   * handleRemaining method
   * Handles returning the number of skips remaining
   *
   * @param clientSocket
   * @return
   */
  private static JSONObject handleRemaining(Socket clientSocket) {
    GameState game = activeGames.get(clientSocket);
    if (game == null || !game.gameActive) {
      return errorResponse("No active game");
    }
    JSONObject response = new JSONObject();
    response.put("type", "remaining");
    response.put("message", "Skips remaining: " + game.skipsRemaining);
    JSONArray actions = new JSONArray();
    actions.put("guess");
    if (game.currentClarity > game.MAX_CLARITY) {
      actions.put("next");
    }
    if (game.skipsRemaining > 0) {
      actions.put("skip");
    }
    actions.put("remaining");
    actions.put("quit");
    response.put("available_actions", actions);
    response.put("time_remaining", game.getRemainingTime());
    return response;
  }

  /**
   * handleQuit method
   * Handles player quitting the game
   *
   * @param clientSocket
   * @return
   */
  private static JSONObject handleQuit(Socket clientSocket) {
    GameState game = activeGames.get(clientSocket);
    if (game != null) {
      updateLeaderboard(game.username, 0); // 0 points for quitting
      game.endGame("inactive");
    }

    JSONObject response = new JSONObject();
    response.put("type", "state");
    response.put("game_state", "inactive");
    response.put("message", "You have left the game");

    return response;
  }

  // --- Helper methods --- //

  /**
   * updateLeaderboard method
   * Handles updating player's score on leaderboard
   *
   * @param username
   * @param score
   */
  private static void updateLeaderboard(String username, int score) {
    leaderboard.put(username, leaderboard.getOrDefault(username, 0) + score);
  }

  /**
   * getLeaderboard method
   * Handles returning current leaderboard
   *
   * @return
   */
  private static JSONObject getLeaderboard() {
    JSONObject response = new JSONObject();
    response.put("type", "leaderboard");

    JSONArray players = new JSONArray();
    leaderboard.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .limit(10) // # of players to display at a time
            .forEach(entry -> {
              JSONObject player = new JSONObject();
              player.put("username", entry.getKey());
              player.put("score", entry.getValue());
              players.put(player);
            });
    response.put("top_players", players);

    // add available actions
    JSONArray actions = new JSONArray();
    actions.put("start");
    actions.put("quit");
    response.put("available_actions", actions);

    return response;
  }

  /**
   * errorResponse method
   * Handles creating standard error response format
   *
   * @param message
   * @return
   */
  private static JSONObject errorResponse(String message) {
    JSONObject response = new JSONObject();
    response.put("type", "error");
    response.put("message", message);
    return response;
  }

  // --- Helper Methods for Networking and JSON --- //

  // converts an int to a 4-byte array
  private static byte[] intToBytes(final int data) {
    return new byte[]{
            (byte)((data >> 24) & 0xff),
            (byte)((data >> 16) & 0xff),
            (byte)((data >> 8)  & 0xff),
            (byte)(data & 0xff)
    };
  }

  // converts a 4-byte array to an integer
  private static int bytesToInt(byte[] bytes) {
    return ((bytes[0] & 0xFF) << 24)
            | ((bytes[1] & 0xFF) << 16)
            | ((bytes[2] & 0xFF) << 8)
            | ((bytes[3] & 0xFF));
  }

  // sends data to the output stream with a length header
  private static void send(OutputStream out, byte... bytes) throws IOException {
    out.write(intToBytes(bytes.length));
    out.write(bytes);
    out.flush();
  }

  // reads exactly 'length' bytes from the input stream
  private static byte[] read(InputStream in, int length) throws IOException {
    byte[] bytes = new byte[length];
    int bytesRead = 0;
    while (bytesRead < length) {
      int result = in.read(bytes, bytesRead, length - bytesRead);
      if (result == -1) {
        throw new EOFException("End of stream reached before reading bytes");
      }
      bytesRead += result;
    }
    return bytes;
  }

  // receives a message: first 4 bytes indicate message length
  private static byte[] receive(InputStream in) throws IOException {
    byte[] lengthBytes = read(in, 4);
    int length = bytesToInt(lengthBytes);
    return read(in, length);
  }

  // converts a JSONObject to a byte array
  private static byte[] toByteArray(JSONObject object) {
    return object.toString().getBytes();
  }

  // converts a byte array to a JSONObject
  private static JSONObject fromByteArray(byte[] bytes) {
    String jsonString = new String(bytes);
    return new JSONObject(jsonString);
  }
}
import org.json.JSONArray;
import org.json.JSONObject;
import java.awt.*;
import java.io.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.json.*;

/**
 * Client class
 * Simplified client implementation focused on UI and communication
 */
class Client {
  private static Socket sock;
  private static OutputStream out;
  private static InputStream in;
  private static String username;
  private static JFrame gameFrame;
  private static JLabel titleLabel;
  private static JLabel messageLabel;
  private static JLabel attemptsLabel;
  private static JLabel timerLabel;
  private static JLabel imageLabel;
  private static JPanel buttonPanel;

  /**
   * Main Entry Point
   * Handles initializing client and starting game flow
   *
   * @param args
   */
  public static void main(String[] args) {
    try {
      connect("localhost", 8888);
      initializeGUI();
      handleAuth();
      gameLoop(); // just initialize UI with available actions

      // keep application running even when game loop exits
      // ui handle the game flow through buttons
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, "Error: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
    } finally {
      // closeConnection(); // ignore close for now
    }
  }

  /**
   * initializeGUI method
   * Handles setting up the UI components
   */
  private static void initializeGUI() {
    gameFrame = new JFrame("Movie Guesser");
    gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    gameFrame.setSize(500, 450);
    gameFrame.setLayout(new BorderLayout());

    // initialize components
    titleLabel = new JLabel("Movie Guesser", SwingConstants.CENTER);
    titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
    gameFrame.add(titleLabel, BorderLayout.NORTH); // add to top

    // add message label
    messageLabel = new JLabel("Welcome to Movie Guesser", SwingConstants.CENTER);

    // image display
    imageLabel = new JLabel();
    imageLabel.setHorizontalAlignment(JLabel.CENTER);

    // wrap image label in a scroll pane
    JScrollPane scrollPane = new JScrollPane(imageLabel);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

    // info panel w/ (attempts counter + message + timer)
    JPanel infoPanel = new JPanel();
    infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

    attemptsLabel = new JLabel("", SwingConstants.CENTER);
    attemptsLabel.setFont(new Font("Arial", Font.BOLD, 16));
    attemptsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    timerLabel = new JLabel("", SwingConstants.CENTER);
    timerLabel.setFont(new Font("Arial", Font.BOLD, 16));
    timerLabel.setForeground(Color.RED);
    timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    infoPanel.add(timerLabel);
    infoPanel.add(attemptsLabel);
    infoPanel.add(messageLabel);

    // button panel for actions
    buttonPanel = new JPanel();
    buttonPanel.setLayout(new GridLayout(0, 1, 5, 20));
    buttonPanel.setPreferredSize(new Dimension(150, 100));
    // add a border
    buttonPanel.setBorder(BorderFactory.createTitledBorder("Actions"));

    // add components to layout
    gameFrame.add(scrollPane, BorderLayout.CENTER);
    gameFrame.add(infoPanel, BorderLayout.SOUTH);
    gameFrame.add(buttonPanel, BorderLayout.EAST);

    // configure window
    gameFrame.setLocationRelativeTo(null); // center window
    gameFrame.setVisible(true);
  }

  /**
   * handleAuth method
   * Handles user login process
   *
   * @throws IOException
   */
  private static void handleAuth() throws IOException {
    username = JOptionPane.showInputDialog("Enter your username: ");

    if (username == null || username.trim().isEmpty()) {
      JOptionPane.showMessageDialog(gameFrame, "Username can't be empty");
      System.exit(0);
    }

    JSONObject joinRequest = new JSONObject();
    joinRequest.put("type", "join");
    joinRequest.put("username", username);

    JSONObject response = sendRequest(joinRequest);
    handleResponse(response);
  }

  /**
   * gameLoop method
   * Handles main game loop simplified to handle user input and server responses
   */
  private static void gameLoop() {
    try {
      // send initial state request to get available actions
      JSONObject stateRequest = new JSONObject();
      stateRequest.put("type", "state");
      JSONObject response = sendRequest(stateRequest);
      handleResponse(response);
    } catch (IOException e) {
        e.printStackTrace();
    }
  }

  /**
   * sendRequest method
   * Handles communication with server
   *
   * @param request
   * @return
   * @throws IOException
   */
  private static JSONObject sendRequest(JSONObject request) throws IOException {
    try {
      ensureConnection(); // check connection before sending
      send(out, toByteArray(request));
      byte[] responseBytes = receive(in);
      if (responseBytes.length <= 0) {
        return new JSONObject().put("type", "error").put("message", "Empty response from server");
      }
      return fromByteArray(responseBytes);
    } catch (JSONException e) {
      System.out.println("Invalid JSON response");
      return new JSONObject().put("type", "error").put("message", "Invalid server response");
    } catch (IOException e) {
      // try to reconnect , send again
      System.out.println("Connection error: " + e.getMessage() + ". Attempting to reconnect...");
      closeConnection();
      connect("localhost", 8888);
      // try again
      send(out, toByteArray(request));
      byte[] responseBytes = receive(in);
      if (responseBytes.length <= 0) {
        return new JSONObject().put("type", "error").put("message", "Empty response from server");
      }
      return fromByteArray(responseBytes);
    }
  }

  /**
   * handleResponse method
   * Handles reacting to server instructions
   *
   * @param response
   */
  private static void handleResponse(JSONObject response) {
    if (response.has("error")) {
      JOptionPane.showMessageDialog(gameFrame, response.getString("error"));
      return; // remove ?
    }

    // show message if present
    if (response.has("message")) {
      messageLabel.setText(response.getString("message"));
    }

    // process based on response type
    String responseType = response.getString("type");
    switch (responseType) {
      case "state":
        updateGameState(response);
        break;
      case "result":
        handleGameResult(response);
        break;
      case "leaderboard":
        displayLeaderboard(response);
        break;
      case "remaining":
        messageLabel.setText(response.getString("message"));
        break;
      case "error":
        JOptionPane.showMessageDialog(gameFrame, response.getString("message"), "Error", JOptionPane.ERROR_MESSAGE);
        break;
    }

    // add action buttons based on available actions
    if (response.has("available_actions")) {
      addActionButtons(response.getJSONArray("available_actions"));
    }

    // refresh UI
    gameFrame.revalidate();
    gameFrame.repaint();
  }

  /**
   * updateGameState method
   * Handles updating UI based on game state data from server
   *
   * @param state
   */
  private static void updateGameState(JSONObject state) {
    // update game state UI elements
    if (state.has("game_state")) {
      String gameState = state.getString("game_state");

      switch (gameState) {
        case "lobby":
          titleLabel.setText("Movie Guesser - Lobby");
          attemptsLabel.setText("");
          // Clear any previous image
          imageLabel.setIcon(null);
          break;
        case "running":
          titleLabel.setText("Movie Guesser - Game In Progress");

          // Show attempts and points info
          StringBuilder statusText = new StringBuilder();
          if (state.has("attempts")) {
            statusText.append("Attempts left: ").append(state.getInt("attempts"));
          }
          if (state.has("current_points")) {
            if (statusText.length() > 0) statusText.append(" | ");
            statusText.append("Current value: ").append(state.getInt("current_points")).append(" points");
          }
          attemptsLabel.setText(statusText.toString());
          break;
        case "inactive":
          titleLabel.setText("Movie Guesser - Disconnected");
          attemptsLabel.setText("");
          imageLabel.setIcon(null);
          break;
        case "time_up":
          titleLabel.setText("Game Over - Time's Up");
          attemptsLabel.setText("");
          break;
      }
    }

    // Update timer if provided.
    if (state.has("time_remaining")) {
      timerLabel.setText("Time left: " + state.getInt("time_remaining") + " sec");
    }

    // update image if provided
    if (state.has("image_data")) {
      SwingUtilities.invokeLater(() -> {
        try {
          // Decode the Base64 image data
          byte[] imageData = Base64.getDecoder().decode(state.getString("image_data"));
          InputStream inputStream = new ByteArrayInputStream(imageData);

          // Read the image using ImageIO for better format compatibility
          BufferedImage img = ImageIO.read(inputStream);
          if (img == null) {
            throw new IOException("Failed to decode image");
          }

          // set the image directly w/ scaling
          Image scaledImage = img.getScaledInstance(300, 300, Image.SCALE_SMOOTH);
          imageLabel.setIcon(new ImageIcon(scaledImage));
        } catch (Exception e) {
          JOptionPane.showMessageDialog(gameFrame, "Error loading image: " + e.getMessage());
          e.printStackTrace();
        }
      });
    }
  }

  /**
   * handleGameResult method
   * Handles processing game result information from server
   *
   * @param result
   */
  private static void handleGameResult(JSONObject result) {
    String details = result.getString("result_details");

    // set the message rather than showing a dialog
    messageLabel.setText(details);

    if (result.has("attempts_left")) {
      attemptsLabel.setText("Attempts left: " + result.getInt("attempts_left"));
    }

    if (result.has("game_state")) {
      String gameState = result.getString("game_state");

      if (gameState.equals("won")) {
        titleLabel.setText("Game Over - You Won!");
        JOptionPane.showMessageDialog(gameFrame, "Congratulations! You guessed correctly!");
      } else if (gameState.equals("lost")) {
        titleLabel.setText("Game Over - You Lost");
        JOptionPane.showMessageDialog(gameFrame, "Game over! Better luck next time!");
      }

      if (gameState.equals("won") || gameState.equals("lost")) {
        attemptsLabel.setText("");
      }
    }
  }

  /**
   * displayLeaderboard method
   * Handles showing leaderboard information from server
   *
   * @param leaderboard
   */
  private static void displayLeaderboard(JSONObject leaderboard) {
    JSONArray players = leaderboard.getJSONArray("top_players");
    StringBuilder sb = new StringBuilder("Leaderboard:\n");

    for (int i = 0; i < players.length(); i++) {
      JSONObject player = players.getJSONObject(i);
      sb.append(String.format("%d. %s - %d points\n",
              i + 1,
              player.getString("username"),
              player.getInt("score")));
    }
    // Use a JTextArea for better formatting of leaderboard
    JTextArea textArea = new JTextArea(sb.toString());
    textArea.setEditable(false);
    JScrollPane scrollPane = new JScrollPane(textArea);
    scrollPane.setPreferredSize(new Dimension(300, 200));

    JOptionPane.showMessageDialog(gameFrame, scrollPane, "Leaderboard", JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * addActionButtons method
   * Handles adding buttons for available actions from server
   *
   * @param actions
   */
  private static void addActionButtons(JSONArray actions) {
    // Clear existing buttons
    buttonPanel.removeAll();

    buttonPanel.setLayout(new GridLayout(actions.length(), 1, 5, 20));

    for (int i = 0; i < actions.length(); i++) {
      String action = actions.getString(i);
      JButton button = new JButton(formatActionName(action));

      button.setFont(new Font("Arial", Font.BOLD, 12));
      button.setPreferredSize(new Dimension(80, 20)); // Make buttons bigger

      // Define final variable for use in lambda
      final String actionFinal = action;

      button.addActionListener(e -> {
        try {
          System.out.println("Button clicked: " + actionFinal);
          switch (actionFinal) {
            case "start":
              handleStartAction();
              break;
            case "guess":
              handleGuessAction();
              break;
            case "next":
              handleNextAction();
              break;
            case "skip":
              handleSkipAction();
              break;
            case "remaining":
              handleRemainingAction();
              break;
            case "leaderboard":
              handleLeaderboardAction();
              break;
            case "quit":
              handleQuitAction();
              break;
          }
        } catch (IOException ex) {
          ex.printStackTrace();
          JOptionPane.showMessageDialog(gameFrame, "Error communicating with server: " + ex.getMessage());
        }
      });

      buttonPanel.add(button);
      System.out.println("Added button: " + formatActionName(action));
    }

    // Ensure the button panel is visible and properly sized
    buttonPanel.setVisible(true);

    // Force layout refresh
    buttonPanel.revalidate();
    buttonPanel.repaint();
  }

  /**
   * formatActionName method
   * Handles making action names more elaborate
   *
   * @param action
   * @return
   */
  private static String formatActionName(String action) {
    switch (action) {
      case "start" :
        return "Start New Game";
      case "guess" :
        return "Make a Guess";
      case "next":
        return "Get Clearer Image";
      case "skip":
        return "Skip Movie";
      case "remaining":
        return "Skips Remaining";
      case "leaderboard" :
        return "View Leaderboard";
      case "quit" :
        return "Quit Game";
      default :
        return action.substring(0, 1).toUpperCase() + action.substring(1);
    }
  }

  /**
   * handleStartAction method
   * Handles sending start game request to server
   *
   * @throws IOException
   */
  private static void handleStartAction() throws IOException {
    JSONObject request = new JSONObject();
    request.put("type", "start");
    request.put("username", username);

    // Prompt for game duration (short, medium, long)
    String duration = JOptionPane.showInputDialog(gameFrame, "Enter game duration (short, medium, long):", "medium");
    if (duration == null || duration.trim().isEmpty()) {
      duration = "medium";
    }

    request.put("duration", duration.toLowerCase());
    JSONObject response = sendRequest(request);
    handleResponse(response);
  }

  /**
   * handleGuessAction method
   * Handles prompts for and sends guess to server
   *
   * @throws IOException
   */
  private static void handleGuessAction() throws IOException {
    String guess = JOptionPane.showInputDialog(gameFrame, "Enter your guess:");

    if (guess != null && !guess.trim().isEmpty()) {
      JSONObject request = new JSONObject();
      request.put("type", "guess");
      request.put("title", guess);

      JSONObject response = sendRequest(request);
      handleResponse(response);
    }
  }

  /**
   * handleNextAction method
   * Handles
   *
   * @throws IOException
   */
  private static void handleNextAction() throws IOException {
    JSONObject request = new JSONObject();
    request.put("type", "next");
    JSONObject response = sendRequest(request);
    handleResponse(response);
  }

  /**
   * handleSkipAction method
   * Handles
   *
   * @throws IOException
   */
  private static void handleSkipAction() throws IOException {
    JSONObject request = new JSONObject();
    request.put("type", "skip");
    JSONObject response = sendRequest(request);
    handleResponse(response);
  }

  /**
   * handleRemainingAction method
   * Handles
   *
   * @throws IOException
   */
  private static void handleRemainingAction() throws IOException {
    JSONObject request = new JSONObject();
    request.put("type", "remaining");
    JSONObject response = sendRequest(request);
    handleResponse(response);
  }

  /**
   * handleLeaderboardAction method
   * Handles requesting leaderboard from server
   *
   * @throws IOException
   */
  private static void handleLeaderboardAction() throws IOException {
    JSONObject request = new JSONObject();
    request.put("type", "leaderboard");

    JSONObject response = sendRequest(request);
    handleResponse(response);
  }

  /**
   * handleQuitAction method
   * Handles sending quit request to server
   *
   * @throws IOException
   */
  private static void handleQuitAction() throws IOException {
    JSONObject request = new JSONObject();
    request.put("type", "quit");

    JSONObject response = sendRequest(request);
    handleResponse(response);

    // Ask if user wants to exit application
    int choice = JOptionPane.showConfirmDialog(gameFrame, "Do you want to exit the application?", "Exit Application", JOptionPane.YES_NO_OPTION);

    if (choice == JOptionPane.YES_OPTION) {
      closeConnection(); // close connection before exiting
      System.exit(0);
    }
  }
  /**
   * connect method
   * Handles establishing connection with server
   *
   * @param host
   * @param port
   * @throws IOException
   */
  private static void connect(String host, int port) throws IOException {
    // open the connection
    sock = new Socket(host, port);
    // get output channel
    out = sock.getOutputStream();
    in = sock.getInputStream();
  }

  /**
   * ensureConnection method
   * Handles [...]
   *
   * @throws IOException
   */
  private static void ensureConnection() throws IOException {
    if (sock == null || sock.isClosed() || !sock.isConnected()) {
      System.out.println("Reconnecting to server...");
      connect("localhost", 8888);
    }
  }

  /**
   * closeConnection method
   * Handles closing connection to server
   */
  private static void closeConnection() {
    try {
      if (sock != null) {
        sock.close();
      }
      if (out != null) {
        out.close();
      }
      if (in != null) {
        in.close();
      }

      // reset to null so ensureConnection knows to recreate them
      sock = null;
      out = null;
      in = null;
    } catch (IOException e) {
      System.out.println("Error closing connection" + e.getMessage());
    }
  }

  // ================= Helper Methods for Networking and JSON ===================

  private static byte[] intToBytes(final int data) {
    return new byte[]{
            (byte)((data >> 24) & 0xff),
            (byte)((data >> 16) & 0xff),
            (byte)((data >> 8) & 0xff),
            (byte)(data & 0xff)
    };
  }

  private static int bytesToInt(byte[] bytes) {
    return ((bytes[0] & 0xFF) << 24)
            | ((bytes[1] & 0xFF) << 16)
            | ((bytes[2] & 0xFF) << 8)
            | ((bytes[3] & 0xFF));
  }

  private static void send(OutputStream out, byte... bytes) throws IOException {
    out.write(intToBytes(bytes.length));
    out.write(bytes);
    out.flush();
  }

  private static byte[] read(InputStream in, int length) throws IOException {
    byte[] bytes = new byte[length];
    int bytesRead = 0;
    while (bytesRead < length) {
      int result = in.read(bytes, bytesRead, length - bytesRead);
      if (result == -1) {
        throw new EOFException("End of stream reached.");
      }
      bytesRead += result;
    }
    return bytes;
  }

  private static byte[] receive(InputStream in) throws IOException {
    byte[] lengthBytes = read(in, 4);
    int length = bytesToInt(lengthBytes);
    return read(in, length);
  }

  private static byte[] toByteArray(JSONObject object) {
    return object.toString().getBytes();
  }

  private static JSONObject fromByteArray(byte[] bytes) {
    String jsonString = new String(bytes);
    return new JSONObject(jsonString);
  }
}
/**
 * File : ThreadedServer.java
 * Author : ndavispe
 * Description : ThreadedServer class in package taskone
 */

package taskone;

/* --- Imports --- */
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * Class : ThreadedServer
 * Description : Server tasks (multithreaded : boundless)
 */
class ThreadedServer {
    // shared across all threads
    static StringList strings = new StringList();
    static Performer performer = new Performer(strings);

    public static void main(String[] args) throws Exception {
        // Setup
        int port = 8000; // default
        if (args.length != 1) {
            // gradle runTask2 -Pport=8000 -q --console=plain
            System.out.println("Usage: gradle runServer -Pport=8000 -q --console=plain");
            System.exit(1);
        }
        port = -1;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be an integer");
            System.exit(2);
        }

        ServerSocket server = new ServerSocket(port);
        System.out.println("Server Started...");
        while (true) {
            System.out.println("Accepting a Request...");
            Socket conn = server.accept();
            // create new thread for each client
            new ClientHandler(conn).start();
        }
    }

    // inner class to handle client in a thread
    static class ClientHandler extends Thread {
        private Socket conn;

        // initializes ClientHandler w/ specific conn
        public ClientHandler(Socket socket) {
            this.conn = socket;
        }

        @Override // good practice to @Override interface/abstract method implemenations
        public void run() {
            boolean quit = false;

            // attempt to establish input/output connection
            try (OutputStream out = conn.getOutputStream(); InputStream in = conn.getInputStream()) {
                System.out.println("Server connected to client : ThreadedServer"); // debugging

                while (!quit) {
                    // doPerform logic here

                    byte[] messageBytes = NetworkUtils.receive(in);
                    JSONObject message = JsonUtils.fromByteArray(messageBytes);
                    JSONObject returnMessage;

                    try {
                        int choice = message.getInt("selected");
                        switch (choice) {
                            case (1): // handle Add
                                String inStr = (String) message.get("data");
                                returnMessage = performer.add(inStr);
                                break;
                            case (3): // handle Display
                                returnMessage = performer.display();
                                break;
                            case (4): // handle Count
                                returnMessage = performer.count();
                                break;
                            case (0): // handle Quit
                                returnMessage = performer.quit();
                                quit = true; // while loop flag to close connection
                                break;
                            default:
                                returnMessage = performer.error("Invalid selection: " + choice + " is not an option");
                                break;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        returnMessage = Performer.error("Invalid request: " + e.getMessage());
                    }

                    byte[] output = JsonUtils.toByteArray(returnMessage);
                    NetworkUtils.send(out, output);
                }
                System.out.println("Client disconnected"); //debugging
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
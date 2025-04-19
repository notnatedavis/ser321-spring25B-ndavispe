/**
 * File : ThreadedPoolServer.java
 * Author : ndavispe
 * Description : ThreadedPoolServer class in package taskone
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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * Class : ThreadedPoolServer
 * Description : Server tasks (multithreaded : bounded)
 */
class ThreadedPoolServer {

    // static variables accessible accross method and classes within ThreadedPoolServer
    static StringList strings = new StringList();
    static Performer performer = new Performer(strings);
    static ExecutorService pool;

    public static void main(String[] args) throws Exception {
        // Setup
        // initialize variables (defaults)
        int port = 8000;
        int poolSize = 2;

        // parse cli args for port & poolSize
        try {
            port = Integer.parseInt(args[0]);
            poolSize = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("[Port] and [Pool] must be integers");
            System.exit(2); // .exit(2) for CLI args
        }

        // fixed thread pool w/ specified pool size
        pool = Executors.newFixedThreadPool(poolSize);

        // ServerSocket listen for client connections on port
        ServerSocket server = new ServerSocket(port);
        System.out.println("Server Started with pool size " + poolSize + "...");

        // accept new client connections
        while (true) {
            System.out.println("Accepting a Request...");
            // accept() blocks until connection is made
            Socket conn = server.accept();
            // submit new ClientTask to thread pool for execution
            pool.execute(new ClientTask(conn));
        }
    }

    // inner class ClientTask implements Runnable so each instance can be executed by thread pool
    static class ClientTask implements Runnable {
        private Socket conn;

        // initializes ClientTask w/ specific conn
        public ClientTask(Socket socket) {
            this.conn = socket;
        }

        @Override // good practice to @Override interface/abstract method implemenations
        public void run() {
            boolean quit = false; // close conn flag

            // attempt to establish input/output connection
            try (OutputStream out = conn.getOutputStream(); InputStream in = conn.getInputStream()) {
                System.out.println("Server connected to client : ThreadedPoolServer"); // debugging

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
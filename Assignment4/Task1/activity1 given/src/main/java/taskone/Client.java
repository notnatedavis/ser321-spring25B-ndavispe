/**
  File: Client.java
  Author: Student in Fall 2020B
  Description: Client class in package taskone.
*/

package taskone;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Base64;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.json.JSONObject;

/**
 * Class: Client
 * Description: Client tasks.
 */
public class Client {
    private static BufferedReader stdin;

    /**
     * Function JSONObject add().
     */
    public static JSONObject add() {
        String strToSend = null;
        JSONObject request = new JSONObject();
        request.put("selected", 1);
        try {
            System.out.print("Please input the string: ");
            strToSend = stdin.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        request.put("data", strToSend);
        return request;
    }


    /**
     * Function JSONObject display().
     */
    public static JSONObject display() {
        JSONObject request = new JSONObject();
        request.put("selected", 3); // updated to match ReadMe.md
        request.put("data", "");
        return request;
    }

    /**
     * Function JSONObject count().
     */
    public static JSONObject count() {
        JSONObject request = new JSONObject();
        request.put("selected", 4); // updated to match ReadMe.md
        request.put("data", "");
        return request;
    }


    /**
     * Function JSONObject quit().
     */
    public static JSONObject quit() {
        JSONObject request = new JSONObject();
        request.put("selected", 0);
        request.put("data", ".");
        return request;
    }

    /**
     * Function main().
     */
    public static void main(String[] args) throws IOException {
        String host;
        int port;
        Socket sock;
        stdin = new BufferedReader(new InputStreamReader(System.in));
        try {
            if (args.length != 2) {
                // gradle runClient -Phost=localhost -Pport=8000 -q --console=plain
                System.out.println("Usage: gradle Client -Phost=localhost -Pport=8000");
                System.exit(0);
            }

            host = args[0];
            port = -1;
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException nfe) {
                System.out.println("[Port] must be an integer");
                System.exit(2);
            }

            sock = new Socket(host, port);
            OutputStream out = sock.getOutputStream();
            InputStream in = sock.getInputStream();
            Scanner input = new Scanner(System.in);
            int choice;
            do {
                System.out.println();
                System.out.println("Client Menu"); // updated to match ReadMe.md
                System.out.println("Please select a valid option (1-5). 0 to diconnect the client");
                System.out.println("1. add <string> - adds a string to the list and display it");
                System.out.println("3. display - display the list");
                System.out.println("4. count - returns the elements in the list");
                System.out.println("0. quit");
                System.out.println();
                choice = input.nextInt(); // what if not int.. should error handle this
                JSONObject request = null;
                switch (choice) {
                    case (1):
                        request = add();
                        break;
                    case (3): // updated to match ReadMe.md
                        request = display();
                        break;
                    case (4): // updated to match ReadMe.md
                        request = count();
                        break;
                    case (0):
                        request = quit();
                        break;
                    default:
                        System.out.println("Please select a valid option (1,3,4,0).");
                        break;
                }
                if (request != null) {
                    System.out.println(request);
                    NetworkUtils.send(out, JsonUtils.toByteArray(request));
                    byte[] responseBytes = NetworkUtils.receive(in);
                    JSONObject response = JsonUtils.fromByteArray(responseBytes);

                    if (response.has("error")) {
                        System.out.println(response.getString("error"));
                    } else {
                        System.out.println();
                        System.out.println("The response from the server: ");
                        System.out.println("datatype: " + response.getString("type"));

                        // count was giving issues
                        String typeStr = (String) response.getString("type");
                        if (typeStr.equals("count")) {
                            int data = response.getInt("data");
                            System.out.println("data: " + data);
                        } else {
                            String data = response.getString("data");
                            System.out.println("data: " + data);
                        }

                        System.out.println();
                        if (typeStr.equals("quit")) {
                            sock.close();
                            out.close();
                            in.close();
                            System.exit(0);
                        }
                    }
                }
            } while (true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
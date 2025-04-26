import java.io.*;
import java.net.Socket;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class : Client
 * Description : Accepts a list of numbers and a delay value from the user and
 *    sends it to the leader for processing. 
 */
 class Client {
     // Input handling setup
     private static final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
     private static synchronized String readLine() throws IOException { return stdin.readLine(); }

    /**
     * Function JSONObject error()
     * Handles error message generation
     *
     * @param err
     * @return
     */
    public static JSONObject error(String err) {
        JSONObject json  = new JSONObject();
        json.put("type", "error");
        json.put("error", err);
        return json; // needed ?
    }

    /**
     * Main Entry Point
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // resource initialization
        Socket serverSock = null;
        OutputStream out = null;
        InputStream in = null;

        // arg check
        if (args.length != 2) {
            System.out.println("Usage : Client <host> <port>");
            System.exit(1);
        }

        // assign args values (network config)
        String host = args[0];
        int port;

        try {
            port = Integer.parseInt(args[1]); // port validation
        } catch (NumberFormatException e) {
            System.out.println("[Port] must be integer");
            System.exit(2);
            return;
        }

        try {
            // connect to the server
            serverSock = new Socket(host, port);

            // write to  the server
            out = serverSock.getOutputStream(); // requests
            in = serverSock.getInputStream(); // responses

            // user input handling
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter numbers separated by commas: ");
            String numbersLine = scanner.nextLine().trim();

            // data parsing + validation
            List<Integer> numbers = Arrays.stream(numbersLine.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            System.out.print("Enter delay in milliseconds: ");
            int delay = scanner.nextInt();

            // request construction
            JSONObject request = new JSONObject();
            request.put("type", "client_request"); // protocol
            request.put("numbers", new JSONArray(numbers));
            request.put("delay", delay);

            // network communication
            byte[] data = JsonUtils.toByteArray(request);
            NetworkUtils.send(out, data);

            // response handling
            byte[] responseBytes = NetworkUtils.receive(in);
            if (responseBytes.length == 0) {
                System.out.println("No response from server");
                exitAndClose(in, out, serverSock);
            }

            JSONObject response = JsonUtils.fromByteArray(responseBytes);

            // response processing
            if (response.getString("type").equals("error")) {
                System.out.println("[Error] : " + response.getString("error"));
            } else if (response.getString("type").equals("result")) {
                // success output
                System.out.println("\nTotal sum: " + response.getInt("total_sum"));
                System.out.println("Single processing time: " + response.getLong("time_single") + " ms");
                System.out.println("Distributed processing time: " + response.getLong("time_distributed") + " ms\n");
            } else {
                System.out.println("Unknown response type");
            }

            // exitAndClose(in, out, serverSock); // not needed (covered in finally)
        } catch (Exception e) {
            System.out.println("[Error] : " + e.getMessage());
            // exitAndClose(in, out, serverSock);
        } finally {
            exitAndClose(in, out, serverSock); // cleanup
        }
    }

    /**
     * Exits the connection
     */
    static void exitAndClose(InputStream in, OutputStream out, Socket serverSock) throws IOException {
        // resource cleanup
        if (in != null) { in.close(); }
        if (out != null) { out.close(); }
        if (serverSock != null) { serverSock.close(); }
        System.exit(0);
    }
 }
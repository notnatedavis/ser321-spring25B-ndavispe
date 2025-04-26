import java.io.*;
import java.net.Socket;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Class : Nodes
 * Description : Each node calculates the sum of its portion of the list, 
 *    simulating computation time by sleeping for a given duration (eg. 100-500ms),
 *    then sends the result back to the leader.
 */
class Nodes {
    private static boolean faulty = false;

    /**
     * Main Entry Point
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: Nodes <leaderHost> <leaderPort> <fault>");
            System.exit(1);
        }

        String leaderHost = args[0];
        int leaderPort = Integer.parseInt(args[1]);
        faulty = args[2].equals("1"); // 1 = (true) fault exist, 0 = (false) fault !exist

        // connection and registration
        try (Socket leaderSocket = new Socket(leaderHost, leaderPort);
             OutputStream out = leaderSocket.getOutputStream();
             InputStream in = leaderSocket.getInputStream()) {

            // register with leader
            JSONObject registration = new JSONObject();
            registration.put("type", "node_registration");
            NetworkUtils.send(out, JsonUtils.toByteArray(registration));

            System.out.println("Node connected to leader");

            while (true) { // main processing
                byte[] received = NetworkUtils.receive(in);
                if (received.length == 0 || received == null) { System.out.println("Connection closed by Leader"); break; } // update ?

                JSONObject task = JsonUtils.fromByteArray(received);
                String type = task.getString("type");

                // main node task
                if (type.equals("node_task")) {
                    // Process node_task
                    JSONArray numbers = task.getJSONArray("numbers");
                    int delay = task.getInt("delay");

                    int sum = calculateSum(numbers, delay);
                    if (faulty) sum += 1;

                    JSONObject response = new JSONObject();
                    response.put("type", "node_response");
                    response.put("sum", sum);
                    NetworkUtils.send(out, JsonUtils.toByteArray(response));
                } else if (type.equals("consensus_check")) {

                    // process consensus_check
                    JSONArray numbers = task.getJSONArray("numbers");
                    int expectedSum = task.getInt("expected_sum");
                    int delay = task.getInt("delay");

                    int actualSum = calculateSum(numbers, delay);
                    if (faulty) {
                        actualSum += 1; // Apply same error as in node_task
                    }
                    boolean verified = (actualSum == expectedSum);

                    JSONObject response = new JSONObject();
                    response.put("type", "consensus_result");
                    response.put("verified", verified);
                    NetworkUtils.send(out, JsonUtils.toByteArray(response));
                } else {
                    System.out.println("Unknown type: " + type);
                }
            }
        } catch (Exception e) {
            System.err.println("Node error : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * calculateSum() method
     * Handles per element processing
     *
     * @param numbers
     * @param delay
     * @return
     * @throws InterruptedException
     */
    private static int calculateSum(JSONArray numbers, int delay) throws InterruptedException {
        int sum = 0;
        for (int i = 0; i < numbers.length(); i++) {
            sum += numbers.getInt(i);
            Thread.sleep(delay);
        }
        return sum;
    }
}
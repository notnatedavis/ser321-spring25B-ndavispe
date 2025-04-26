import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Class : Leader
 * Description : Divides the list into smaller portions, sends each portion to 
 *    a different node, waits for results, and combines these to get the final
 *    sum. The leader also performs a simple consensus to verify results.
 */
class Leader {
    private static List<Socket> nodes = new ArrayList<>();
    private static final Object nodesLock = new Object(); // sync access to shared node list

    /**
     * Main Entry Point
     * 
     * @param args
     */
    public static void main (String args[]) {
        // arg check
        if (args.length != 1) {
            System.out.println("Usage: Leader <port>");
            System.exit(1);
        }

        int port = 8000; // default port
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Port must be an integer");
            System.exit(2);
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Leader started on port " + port);

            ExecutorService executor = Executors.newCachedThreadPool();

            while (true) {
                Socket socket = serverSocket.accept();
                executor.submit(() -> handleConnection(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * handleConnection() method
     * Handles node registration from client requests
     *
     * @param socket
     */
    private static void handleConnection(Socket socket) {
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            byte[] received = NetworkUtils.receive(in);
            if (received.length == 0) { socket.close(); return; }

            JSONObject json = JsonUtils.fromByteArray(received);
            String type = json.getString("type");

            if (type.equals("node_registration")) {
                synchronized (nodesLock) {
                    nodes.add(socket);
                    System.out.println("Node registered. Total nodes : " + nodes.size());
                }
                // keep node connection open for future
            } else if (type.equals("client_request")) {
                handleClientRequest(json, out);
                socket.close();
            } else {
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * handleClientRequest() method
     * Handles validating nodes, partitioning, parallel tasks, and result verification
     *
     * @param json
     * @param out
     */
    private static void handleClientRequest(JSONObject json, OutputStream out) {
        synchronized (nodesLock) {
            // validation
            if (nodes.size() < 3) {
                sendError(out, "Not enough nodes (min 3 required)");
                return;
            }
        }

        try {
            // data processing
            JSONArray numbersArray = json.getJSONArray("numbers");
            int delay = json.getInt("delay");

            // single-threaded sum calculation
            long startSingle = System.currentTimeMillis();
            int singleSum = calculateSum(numbersArray, delay);
            long timeSingle = System.currentTimeMillis() - startSingle;

            // distribute tasks to nodes
            List<List<Integer>> partitions = partitionList(numbersArray, 3);
            ExecutorService executor = Executors.newFixedThreadPool(3);
            List<Future<Integer>> futures = new ArrayList<>();

            // task distribution
            for (int i = 0; i < 3; i++) {
                List<Integer> partition = partitions.get(i);
                Socket nodeSocket = nodes.get(i % nodes.size());
                futures.add(executor.submit(() -> processNodeTask(nodeSocket, partition, delay)));
            }

            // collect results
            List<Integer> partialSums = new ArrayList<>();
            int distributedSum = 0;
            long startDistributed = System.currentTimeMillis();
            for (Future<Integer> future : futures) {
                int partial = future.get();
                partialSums.add(partial);
                distributedSum += partial;
            }
            long timeDistributed = System.currentTimeMillis() - startDistributed;

            // consensus check
            boolean consensus = performConsensusCheck(partitions, partialSums, delay);

            if (consensus) {
                JSONObject response = new JSONObject();
                response.put("type", "result");
                response.put("total_sum", distributedSum);
                response.put("time_single", timeSingle);
                response.put("time_distributed", timeDistributed);
                NetworkUtils.send(out, JsonUtils.toByteArray(response));
            } else {
                sendError(out, "Consensus check failed");
            }
        } catch (Exception e) {
            sendError(out, "Error processing request: " + e.getMessage());
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

    /**
     * partitionList() method
     * Handles partitioning data
     *
     * @param numbers
     * @param parts
     * @return
     */
    private static List<List<Integer>> partitionList(JSONArray numbers, int parts) {
        List<List<Integer>> partitions = new ArrayList<>();
        int size = (numbers.length() + parts - 1) / parts;
        for (int i = 0; i < numbers.length(); i+= size) {
            int end = Math.min(i + size, numbers.length());
            List<Integer> part = new ArrayList<>();
            for (int j = i; j < end; j++) {
                part.add(numbers.getInt(j));
            }
            partitions.add(part);
        }
        return partitions;
    }

    /**
     * processNodeTask() method
     * Handles processing individual nodes
     *
     * @param nodeSocket
     * @param partition
     * @param delay
     * @return
     * @throws Exception
     */
    private static Integer processNodeTask(Socket nodeSocket, List<Integer> partition, int delay) throws Exception {
        try {
            nodeSocket.setSoTimeout(10000); // 10 second emergency timeout
            JSONObject task = new JSONObject();
            task.put("type", "node_task");
            task.put("numbers", new JSONArray(partition));
            task.put("delay", delay);

            OutputStream nodeOut = nodeSocket.getOutputStream();
            InputStream nodeIn = nodeSocket.getInputStream();

            NetworkUtils.send(nodeOut, JsonUtils.toByteArray(task));
            byte[] responseBytes = NetworkUtils.receive(nodeIn);
            JSONObject response = JsonUtils.fromByteArray(responseBytes);
            return response.getInt("sum");
        } catch (SocketTimeoutException e) {
            // remove dead node from list
            synchronized (nodesLock) {
                nodes.remove(nodeSocket);
            }
            throw new Exception("Node timeout: " + e.getMessage());
        }
    }

    /**
     * performConsensusCheck() method
     * Handles unanimous agreement across all nodes for validation
     *
     * @param partitions
     * @param partialSums
     * @param delay
     * @return
     */
    private static boolean performConsensusCheck(List<List<Integer>> partitions, List<Integer> partialSums, int delay) {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();

            // Create verification tasks
            for (int i = 0; i < partitions.size(); i++) {
                final int idx = i;
                futures.add(executor.submit(() -> {
                    try {
                        List<Integer> partition = partitions.get(idx);
                        int expectedSum = partialSums.get(idx);
                        int verifyNodeIndex = (idx + 1) % nodes.size(); // Verify with next node
                        Socket node = nodes.get(verifyNodeIndex);

                        JSONObject task = new JSONObject();
                        task.put("type", "consensus_check");
                        task.put("numbers", new JSONArray(partition));
                        task.put("expected_sum", expectedSum);
                        task.put("delay", delay);

                        NetworkUtils.send(node.getOutputStream(), JsonUtils.toByteArray(task));
                        JSONObject response = JsonUtils.fromByteArray(NetworkUtils.receive(node.getInputStream()));

                        return response.getBoolean("verified");
                    } catch (Exception e) {
                        return false;
                    }
                }));
            }

            // Verify all responses
            for (Future<Boolean> future : futures) {
                if (!future.get()) return false;
            }
            return true;

        } catch (Exception e) {
            return false;
        } finally {
            executor.shutdown(); // force shutdown
        }
    }

    /**
     * sendError() method
     * Handles error message generation
     *
     * @param out
     * @param message
     */
    private static void sendError(OutputStream out, String message) {
        try {
            JSONObject error = new JSONObject();
            error.put("type", "error");
            error.put("error", message);
            NetworkUtils.send(out, JsonUtils.toByteArray(error));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
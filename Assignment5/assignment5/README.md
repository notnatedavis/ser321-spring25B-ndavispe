# Execution

1. `gradle runLeader -PleaderPort=8000` / `gradle runLeader` launches Leader
2. `gradle runNode -PleaderPort=8000` / `gradle runNode` launches Node instance
3. ensure 3 Node instances are running
4. `gradle runClient -PleaderHost=localhost -PleaderPort=8000` / `gradle runClient` launches Client 
5. follow CLI inputs

# Purpose & Functionality
Building a simple distributed algorithm that distributes computation across multiple nodes. Includes basic consensus algorithm to verify results. Focused on robustness, cleanliness, and proper documentation.

# Protocol

### Client Request (Client -> Leader)

Request : 

    {
        "type" : "client_request",  
        "numbers" : [1,2,3,4,5],
        "delay" : 100
    }

Success Response :

    {
        "type" : "result",
        "total_sum" : 15, 
        "time_single" : 500,
        "time_distributed" : 200
    }

Error Response :

    {
        "type" : error",
        "error" : "Not enough nodes"
    }

    {
        "type" : error",
        "error" : "Invalid number format"
    }

### Node Task Distribution (Leader -> Nodes)

Request :

    {
        "type" : "node_task",
        "numbers" : [1,2,3],
        "delay" : 100
    }

Response :

    {
        "type" : "node_response",
        "sum" : 6
    }

Faulty Response :

    {
        "type" : "node_response",
        "sum" : sum + 100 
        // intentionally incorrect
    }

### Consensus Check (Leader -> Node)

Consensus Request :

    {
        "type" : "consensus_check",
        "partial_sums" : [6,5,4],
        "expected_sum": 15,
        "delay": 100
    }

Response :

    {
        "type" : "consensus_result",
        "verified" : true
    }

Faulty Response :

    {
        "type" : "consensus_result",
        "verified" : false
    }

# Workflow
1. Launch Leader , then 3 instances of Nodes (min required) , then Client
2. Input List and delay
3. Leader processes and distributes tasks across nodes along with own computations
4. Leader distributes verification tasks for consensus
5. Each node recalculates sum with original delay
6. Nodes return verification status (true/false)
7. Leader requires 100% approval from Nodes for success 
8. Results are returned to Client and Leader

# Requirements
- Client accepts a list of numbers and a delay value from the user and sends it to the leader for processing
- Leader divides the list into smaller portions, sends each portion to a different node, waits for results, and combines these to get the final sum
- Leader performs simple consensus to verify results
- Each node calculates the sum of its portion of the list, simulating computation time by sleeping for a given duration (100-500ms), then sends the result back to Leader
- Leader can be started via a gradle Task
- Client can be started via a gradle Task
- Each Node can be started via a gradle task
- (All Nodes in the system are instances of single Node class)
- 3 Nodes are connected to Leader
- if < 3 Nodes in the network, Leader sends error message to Client and stops processing
- Client asks the user to input a list of numbers and a delay time, which it then sends to the leader [Ex: {1,2,3,4,...,15}, delay=50ms]
- Leader calculates the sum on its own, adding X milliseconds delay to each iteration to simulate time-consuming calculation
- Leader divides the list into equal parts with 3 nodes
- Distributed Sum Calculation is handled through (Leader sends each Node a portion of the list along with Client specified delay, threaded so that nodes calculate in parallel, Each node computes the sum of its portion, applying delay between each addition as Leader did, after receiving all partial sums from Nodes Leader combines and calculates total sum and time taken for distributed processing)
- Leader compares the time taken for single sum processing with distributed processing and prints the result
- Nodes can simulate faults, in which case they will calculate the sum of the given list incorrectly, use `-pFault=1` to make a Node perform an incorrect calculation
- Consensus Check for Result Verification is handled through : (Leader sends each Node the sum and list from another Node, sending information should be threaded, Each Node recalculates the sum and compares it with the sum received from Leader, Nodes return true/false response to Leader to indicate agreement with results. Nodes should always respond true/yes if no faulty nodes in network, if all Nodes agree on result, Leader sends final sum and computation times to the Client, if consensus fails, Leader sends an error msg to client)
- Client displays the results clearly
- Robust error handling w/ no crashes

# Screencast
(https://youtu.be/N6ph2pE8DXs)

# Analysis
The distributed system demonstrates performance advantages in situations where computational workload is substantial. Parallel processing across nodes consistently outperforms single threaded computation in scenarios of larger scale but its benefits are lost at smaller scales. Centralized processing is best in scenarios of low system overhead since communication and JSON responses bring their own delays. The impact of the delays becomes neglected when the overhead outweighs the delay.
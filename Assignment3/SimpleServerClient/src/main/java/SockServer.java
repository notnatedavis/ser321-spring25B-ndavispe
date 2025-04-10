import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.*;

import javax.swing.JScrollPane;

import java.io.*;

/**
 * A class to demonstrate a simple client-server connection using sockets.
 */
public class SockServer {
  static Socket sock;
  static DataOutputStream os;
  static ObjectInputStream in;

  static int port = 8888;

  public static void main (String args[]) {

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

      /**
       * Simple loop accepting one client and calling handling one request
       */


      while (true){
        System.out.println("Server waiting for a connection");
        sock = serv.accept(); // blocking wait
        System.out.println("Client connected");

        // setup the object reading channel
        in = new ObjectInputStream(sock.getInputStream());

        // get output channel
        OutputStream out = sock.getOutputStream();

        // create an object output writer (Java only)
        os = new DataOutputStream(out);

        boolean connected = true;
        while (connected) {
          String s = "";
          try {
            s = (String) in.readObject(); // attempt to read string in from client
          } catch (Exception e) { // catch rough disconnect
            System.out.println("Client disconnect");
            connected = false;
            continue;
          }

          JSONObject res = isValid(s); // validates inputed string from client

          if (res.has("ok")) { // 'ok' serves as flag for if error
            writeOut(res);
            continue;
          }

          JSONObject req = new JSONObject(s);

          res = testField(req, "type");
          if (!res.getBoolean("ok")) { // no "type" header provided
            res = noType(req);
            writeOut(res);
            continue;
          }
          // check which request it is (could also be a switch statement)
          // update here
          if (req.getString("type").equals("echo")) {
            res = echo(req);
          } else if (req.getString("type").equals("add")) {
            res = add(req);
          } else if (req.getString("type").equals("addmany")) {
            res = addmany(req);
          } else if (req.getString("type").equals("stringconcatenation")) {
            res = stringconcatenation(req); // implement/create stringconcatenation()
          } else if (req.getString("type").equals("charcount")) {
            res = charcount(req); // implement/create charcount()
          } else {
            res = wrongType(req);
          }
          writeOut(res);
        }
        // if we are here - client has disconnected so close connection to socket
        overandout();
      }
    } catch(Exception e) {
      e.printStackTrace();
      overandout(); // close connection to socket upon error
    }
  }


  /**
   * Checks if a specific field exists
   */
  static JSONObject testField(JSONObject req, String key){
    JSONObject res = new JSONObject();

    // field does not exist
    if (!req.has(key)){
      res.put("ok", false);
      res.put("message", "Field " + key + " does not exist in request");
      return res;
    }
    return res.put("ok", true);
  }

  // handles the simple echo request
  static JSONObject echo(JSONObject req){
    System.out.println("Echo request: " + req.toString());
    JSONObject res = testField(req, "data");
    if (res.getBoolean("ok")) {
      if (!req.get("data").getClass().getName().equals("java.lang.String")){
        res.put("ok", false);
        res.put("message", "Field data needs to be of type: String");
        return res;
      }

      res.put("type", "echo");
      res.put("echo", "Here is your echo: " + req.getString("data"));
    }
    return res;
  }

  // handles the simple add request with two numbers
  static JSONObject add(JSONObject req){
    System.out.println("Add request: " + req.toString());
    JSONObject res1 = testField(req, "num1");
    if (!res1.getBoolean("ok")) {
      return res1;
    }

    JSONObject res2 = testField(req, "num2");
    if (!res2.getBoolean("ok")) {
      return res2;
    }

    JSONObject res = new JSONObject();
    res.put("ok", true);
    res.put("type", "add");
    try {
      res.put("result", req.getInt("num1") + req.getInt("num2"));
    } catch (org.json.JSONException e){
      res.put("ok", false);
      res.put("message", "Field num1/num2 needs to be of type: int");
    }
    return res;
  }

  ///// implement
  static JSONObject stringconcatenation(JSONObject req) { // updated inventory->stringconcatenation
    JSONObject res = new JSONObject();
    res.put("type", "stringconcatenation");

    // check for required fields (are both strings ok)
    JSONObject res1 = testField(req, "string1");
    if (!res1.getBoolean("ok")) {
      return res1;
    }
    JSONObject res2 = testField(req, "string2");
    if (!res2.getBoolean("ok")) {
      return res2;
    }

    // validate string types
    try {
      String string1 = req.getString("string1");
      String string2 = req.getString("string2");
    } catch (JSONException e) {
      res.put("ok", false);
      res.put("message", "string1 & string2 must be valid strings");
      return res;
    }

    // concatenate + respond
    res.put("ok", true);
    res.put("result", req.getString("string1") + req.getString("string2"));
    return res;
  }

  ///// implement
  static JSONObject charcount(JSONObject req) {
    JSONObject res = new JSONObject();
    res.put("type", "charcount");

    // validate findchar
    JSONObject checkFindChar = testField(req, "findchar");
    if (!checkFindChar.getBoolean("ok")) {
      return checkFindChar;
    }
    boolean findchar;
    try {
      findchar = req.getBoolean("findchar");
    } catch (JSONException e) {
      res.put("ok", false);
      res.put("message", "findchar must be valid boolean");
      return res;
    }

    // validate count
    JSONObject checkCount = testField(req, "count");
    if (!checkCount.getBoolean("ok")) {
      return checkCount;
    }
    String countStr;
    try {
      countStr = req.getString("count");
    } catch (JSONException e) {
      res.put("ok", false);
      res.put("message", "count must be a string");
      return res;
    }

    if (findchar) {
      // validate find
      JSONObject checkFind = testField(req, "find");
      if (!checkFind.getBoolean("ok")) {
        return checkFind;
      }
      String findStr;
      try {
        findStr = req.getString("find");
      } catch (JSONException e) {
        res.put("ok", false);
        res.put("message", "find must be a string");
        return res;
      }
      if (findStr.length() != 1) {
        res.put("ok", false);
        res.put("message", "find must be a single character");
        return res;
      }

      // count occurrences (counting logic)
      char target = findStr.charAt(0);
      int total = 0;
      for (char c : countStr.toCharArray()) {
        if (c == target) {
          total++;
        }
      }
      res.put("result", total);
    } else {
      // return length of count
      res.put("result", countStr.length());
    }
    res.put("ok", true);
    return res;
  }

  // handles the simple addmany request
  static JSONObject addmany(JSONObject req){
    System.out.println("Add many request: " + req.toString());
    JSONObject res = testField(req, "nums");
    if (!res.getBoolean("ok")) {
      return res;
    }

    int result = 0;
    JSONArray array = req.getJSONArray("nums");
    for (int i = 0; i < array.length(); i ++){
      try{
        result += array.getInt(i);
      } catch (org.json.JSONException e){
        res.put("ok", false);
        res.put("message", "Values in array need to be ints");
        return res;
      }
    }

    res.put("ok", true);
    res.put("type", "addmany");
    res.put("result", result);
    return res;
  }

  // creates the error message for wrong type
  static JSONObject wrongType(JSONObject req){
    System.out.println("Wrong type request: " + req.toString());
    JSONObject res = new JSONObject();
    res.put("ok", false);
    res.put("message", "Type " + req.getString("type") + " is not supported.");
    return res;
  }

  // creates the error message for no given type
  static JSONObject noType(JSONObject req){
    System.out.println("No type request: " + req.toString());
    JSONObject res = new JSONObject();
    res.put("ok", false);
    res.put("message", "No request type was given.");
    return res;
  }

  // From: https://www.baeldung.com/java-validate-json-string
  public static JSONObject isValid(String json) {
    try {
      new JSONObject(json);
    } catch (JSONException e) {
      try {
        new JSONArray(json);
      } catch (JSONException ne) {
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "req not JSON");
        return res;
      }
    }
    return new JSONObject();
  }

  // sends the response and closes the connection between client and server.
  static void overandout() {
    try {
      os.close();
      in.close();
      sock.close();
    } catch(Exception e) {e.printStackTrace();}

  }

  // sends the response and closes the connection between client and server.
  static void writeOut(JSONObject res) {
    try {
      os.writeUTF(res.toString());
      // make sure it wrote and doesn't get cached in a buffer
      os.flush();

    } catch(Exception e) {e.printStackTrace();}

  }
}
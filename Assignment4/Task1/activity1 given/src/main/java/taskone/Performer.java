/**
  File: Performer.java
  Author: Student in Fall 2020B
  Description: Performer class in package taskone.
*/

package taskone;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

import static java.lang.Thread.sleep;

/**
 * Class: Performer 
 * Description: Threaded Performer for server tasks.
 */
class Performer {

    private StringList state;

    public Performer(StringList strings) {
        this.state = strings;
    }

    // add method (1)
    public JSONObject add(String str) throws InterruptedException {
        System.out.println("Start add"); 
        JSONObject json = new JSONObject();
        // json.put("datatype", 1); // not needed since "type", "add"
        json.put("type", "add");
        sleep(6000); // to make this take a bit longer
        state.add(str);
        json.put("data", state.toString());
        System.out.println("end add");
        return json;
    }

    // display method (3)
    public JSONObject display() {
        JSONObject json = new JSONObject();
        json.put("type", "display");
        json.put("data", state.toString()); // return entire list as string
        return json;
    }

    // count method (4)
    public JSONObject count() {
        JSONObject json = new JSONObject();
        json.put("type", "count");
        json.put("data", state.size()); // return int count
        return json;
    }

    // quit method (0)
    public JSONObject quit() {
        JSONObject json = new JSONObject();
        json.put("type", "quit");
        json.put("data", ""); // nothing needed for quit
        return json;
    }

    public static JSONObject error(String err) {
        JSONObject json = new JSONObject();
        json.put("type", "error"); // updated to match ReadMe.md protocol
        json.put("error", err); // key
        return json;
    }
}

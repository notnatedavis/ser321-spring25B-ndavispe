import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import org.json.JSONObject;

public class WhiteBox {

    @Test // first new Test
    public void reverseCorrect() {
        JSONObject req = new JSONObject();
        req.put("type", "reverse");
        req.put("data", "hello");

        JSONObject res = SockServer.reverse(req);

        assertEquals("reverse", res.getString("type"));
        assertEquals(res.getBoolean("ok"), true);
        assertEquals(res.getString("result"), "olleh");
    }

    @Test // second new Test
    public void uppercaseMissingData() {
        JSONObject req = new JSONObject();
        req.put("type", "uppercase");

        JSONObject res = SockServer.uppercase(req);

        assertEquals(res.getBoolean("ok"), false);
        assertEquals(res.getString("message"), "Field data does not exist in request");
    }
}
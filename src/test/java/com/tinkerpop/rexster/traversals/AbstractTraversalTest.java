package com.tinkerpop.rexster.traversals;

import junit.framework.TestCase;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public class AbstractTraversalTest extends TestCase {

    public void testQueryParametersToJson() {
        TestTraversal tt = new TestTraversal();
        Map<String, String> qp = new HashMap<String, String>();
        qp.put("a", "true");
        qp.put("b", "false");
        qp.put("c.a", "12.0");
        qp.put("c.b", "\"marko\"");
        qp.put("c.c", "peter");
        qp.put("c.d.a.b", "true");
        qp.put("d", "[marko,rodriguez,10]");
        tt.buildRequestObject(qp);
        assertTrue((Boolean) tt.requestObject.get("a"));
        assertFalse((Boolean) tt.requestObject.get("b"));
        assertEquals(((JSONObject) tt.requestObject.get("c")).get("a"), 12.0);
        assertEquals(((JSONObject) tt.requestObject.get("c")).get("b"), "marko");
        assertEquals(((JSONObject) tt.requestObject.get("c")).get("c"), "peter");
        assertTrue((Boolean) ((JSONObject) ((JSONObject) ((JSONObject) tt.requestObject.get("c")).get("d")).get("a")).get("b"));
        assertEquals(((JSONArray) tt.requestObject.get("d")).get(0), "marko");
        assertEquals(((JSONArray) tt.requestObject.get("d")).get(1), "rodriguez");
        // TODO: make this not a string but a number?
        assertEquals(((JSONArray) tt.requestObject.get("d")).get(2), "10");
    }

    public void testParsing() throws Exception {
        JSONParser parser = new JSONParser();
        assertEquals(JSONArray.class, parser.parse("[\"a\",\"b\"]").getClass());
    }

    private class TestTraversal extends AbstractTraversal {
        public String getTraversalName() {
            return "test-traversal";
        }

        public void traverse() {

        }
    }
}

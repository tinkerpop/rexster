package com.tinkerpop.rexster;

import com.tinkerpop.rexster.traversals.AbstractTraversal;
import junit.framework.TestCase;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class BaseResourceTest extends TestCase {

    public void testQueryParametersToJson() {
        BaseResource tt = new VertexResource();
        Map<String, String> qp = new HashMap<String, String>();
        qp.put("a", "true");
        qp.put("b", "false");
        qp.put("c.a", "12.0");
        qp.put("c.b", "\"marko\"");
        qp.put("c.c", "peter");
        qp.put("c.d.a.b", "true");
        qp.put("d", "[marko,rodriguez,10]");
        tt.buildRequestObject(qp);
        assertTrue((Boolean) tt.getRequestObject().get("a"));
        assertFalse((Boolean) tt.getRequestObject().get("b"));
        assertEquals(((JSONObject) tt.getRequestObject().get("c")).get("a"), 12.0);
        assertEquals(((JSONObject) tt.getRequestObject().get("c")).get("b"), "marko");
        assertEquals(((JSONObject) tt.getRequestObject().get("c")).get("c"), "peter");
        assertTrue((Boolean) ((JSONObject) ((JSONObject) ((JSONObject) tt.getRequestObject().get("c")).get("d")).get("a")).get("b"));
        assertEquals(((JSONArray) tt.getRequestObject().get("d")).get(0), "marko");
        assertEquals(((JSONArray) tt.getRequestObject().get("d")).get(1), "rodriguez");
        // TODO: make this not a string but a number?
        assertEquals(((JSONArray) tt.getRequestObject().get("d")).get(2), "10");
    }

    public void testParsing() throws Exception {
        JSONParser parser = new JSONParser();
        assertEquals(JSONArray.class, parser.parse("[\"a\",\"b\"]").getClass());
    }

    public void testOffsetParsing() {
        BaseResource tt = new VertexResource();
        tt.buildRequestObject("{ \"offset\": { \"start\":10, \"end\":100 }}");
        assertEquals((long)tt.getStartOffset(), 10l);
        assertEquals((long)tt.getEndOffset(), 100l);

        tt = new VertexResource();
        tt.buildRequestObject("{ \"offset\": { \"start\":-10, \"end\":10001 }}");
        assertEquals((long)tt.getStartOffset(), -10l);
        assertEquals((long)tt.getEndOffset(), 10001l);
    }


}

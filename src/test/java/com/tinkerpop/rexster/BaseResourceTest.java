package com.tinkerpop.rexster;

import junit.framework.TestCase;

import org.codehaus.jettison.json.JSONException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class BaseResourceTest extends TestCase {

    public void testQueryParametersToJson()  throws JSONException {
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
        assertTrue(tt.getRequestObject().optBoolean("a"));
        assertFalse(tt.getRequestObject().optBoolean("b"));
        assertEquals(tt.getRequestObject().optJSONObject("c").optDouble("a"), 12.0);
        assertEquals(tt.getRequestObject().optJSONObject("c").optString("b"), "\"marko\"");
        assertEquals(tt.getRequestObject().optJSONObject("c").optString("c"), "peter");
        assertTrue(tt.getRequestObject().optJSONObject("c").optJSONObject("d").optJSONObject("a").optBoolean("b"));
        assertEquals(tt.getRequestObject().optJSONArray("d").optString(0), "marko");
        assertEquals(tt.getRequestObject().optJSONArray("d").optString(1), "rodriguez");
        // TODO: make this not a string but a number?
        assertEquals(tt.getRequestObject().optJSONArray("d").optString(2), "10");
    }

    public void testOffsetParsing() throws JSONException {
        BaseResource tt = new VertexResource();
        tt.buildRequestObject("{\"rexster\": { \"offset\": { \"start\":10, \"end\":100 }}}");
        assertEquals((long)tt.getStartOffset(), 10l);
        assertEquals((long)tt.getEndOffset(), 100l);

        tt = new VertexResource();
        tt.buildRequestObject("{\"rexster\": { \"offset\": { \"start\":-10, \"end\":10001 }}}");
        assertEquals((long)tt.getStartOffset(), -10l);
        assertEquals((long)tt.getEndOffset(), 10001l);
    }


}

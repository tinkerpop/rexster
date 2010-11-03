package com.tinkerpop.rexster;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class VertexResourceTest extends BaseTest {

    public void setUp() {
        try {
            this.startWebServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void tearDown() {
        try {
            this.stopWebServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testGetAllVertices() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices");
        JSONObject object = getResource(uri);
        printPerformance("GET all vertices", null, uri, sh.stopWatch());
        assertEquals(object.getLong("total_size"), 809l);
        
        JSONArray arr = object.getJSONArray("results");
        
        for (int ix = 0; ix < arr.length(); ix ++) {
        	JSONObject vertex = arr.getJSONObject(ix);
            assertEquals(vertex.getString("_type"), "vertex");
        }
        assertEquals((long) ((JSONArray) object.get("results")).length(), object.getLong("total_size"));
    }

    public void testGetAllVerticesWithOffset() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices?rexster.offset.start=10&rexster.offset.end=20");
        JSONObject object = getResource(uri);
        printPerformance("GET vertices 10-20", null, uri, sh.stopWatch());
        assertEquals(object.getLong("total_size"), 809l);
        
        JSONArray arr = object.getJSONArray("results");
        
        for (int ix = 0; ix < arr.length(); ix ++) {
        	JSONObject vertex = arr.getJSONObject(ix);

            assertEquals(vertex.getString("_type"), "vertex");
        }
        assertEquals((long) ((JSONArray) object.get("results")).length(), 10);
    }

    public void testGetVertexIndex() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices?name=Garcia");
        JSONObject object = getResource(uri);
        printPerformance("GET single vertex through index", null, uri, sh.stopWatch());
        assertEquals(object.getLong("total_size"), 1l);
        assertEquals((long) ((JSONArray) object.get("results")).length(), object.getLong("total_size"));
    }

    public void testGetVertexEdges() throws Exception {
        long total = 0l;
        sh.stopWatch();
        String uri = createURI("vertices/1/outE");
        JSONObject object = getResource(uri);
        printPerformance("GET vertex out edges", null, uri, sh.stopWatch());
        total = total + object.getLong("total_size");
        assertEquals((long) ((JSONArray) object.get("results")).length(), object.getLong("total_size"));

        sh.stopWatch();
        uri = createURI("vertices/1/inE");
        object = getResource(uri);
        printPerformance("GET vertex in edges", null, uri, sh.stopWatch());
        total = total + object.getLong("total_size");
        assertEquals((long) ((JSONArray) object.get("results")).length(), object.getLong("total_size"));

        sh.stopWatch();
        uri = createURI("vertices/1/bothE");
        object = getResource(uri);
        printPerformance("GET vertex both edges", null, uri, sh.stopWatch());
        assertEquals(total, object.getLong("total_size"));
        assertEquals(object.getLong("total_size"), total);
        assertEquals((long) ((JSONArray) object.get("results")).length(), object.getLong("total_size"));

    }

    public void testGetVertexEdgesLabelFilter() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices/1/outE?_label=sung_by");
        JSONObject object = getResource(uri);
        printPerformance("GET vertex out edges label filter", null, uri, sh.stopWatch());
        assertEquals(object.getLong("total_size"), 1l);
        assertEquals((long) ((JSONArray) object.get("results")).length(), object.getLong("total_size"));

        sh.stopWatch();
        uri = createURI("vertices/1/outE?_label=written_by");
        object = getResource(uri);
        printPerformance("GET vertex out edges label filter", null, uri, sh.stopWatch());
        assertEquals(object.getLong("total_size"), 1l);
        assertEquals((long) ((JSONArray) object.get("results")).length(), object.getLong("total_size"));
    }

    public void testPostVertex() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices/9999");
        JSONObject object = postResource(uri);
        printPerformance("POST vertex", null, uri, sh.stopWatch());
        object = object.getJSONObject("results");
        assertEquals(object.getString("_id"), "9999");
        assertEquals(object.getString("_type"), "vertex");
        assertEquals(object.opt("_outE"), null);
        uri = createURI("vertices/9999/");
        object = getResource(uri);
        object = object.getJSONObject("results");
        assertEquals(object.getString("_id"), "9999");
        assertEquals(object.getString("_type"), "vertex");
        assertEquals(object.opt("_outE"), null);
    }

    public void testPostManyVertices() throws Exception {
        sh.stopWatch();
        int startVertexId = 9999;
        int total = 250;
        for (int i = startVertexId; i < startVertexId + total; i++) {
            String uri = createURI("vertices/" + i);
            JSONObject object = postResource(uri);
            object = object.getJSONObject("results");
            assertEquals(object.getString("_id"), "" + i);
            assertEquals(object.getString("_type"), "vertex");
            assertEquals(object.opt("_outE"), null);
        }
        printPerformance("POST vertices", total, "vertices/xxxx", sh.stopWatch());

        sh.stopWatch();
        for (int i = startVertexId; i < startVertexId + total; i++) {
            String uri = createURI("vertices/" + i);
            JSONObject object = getResource(uri);
            object = object.getJSONObject("results");
            assertEquals(object.getString("_id"), "" + i);
            assertEquals(object.getString("_type"), "vertex");
            assertEquals(object.opt("_outE"), null);
        }
        printPerformance("GET vertices", total, "vertices/xxxx", sh.stopWatch());
    }

    public void testPostVertexProperties() throws Exception {
        // to existing vertex
        sh.stopWatch();
        String uri = createURI("vertices/1?key1=value1&key2=value2&name=REXSTER");
        JSONObject object = postResource(uri);
        printPerformance("POST vertex", null, uri, sh.stopWatch());
        object = object.getJSONObject("results");
        assertEquals(object.getString("_id"), "1");
        assertEquals(object.getString("_type"), "vertex");
        assertEquals(object.getString("key1"), "value1");
        assertEquals(object.getString("key2"), "value2");
        assertEquals(object.getString("name"), "REXSTER");
        uri = createURI("vertices/1");
        object = getResource(uri);
        object = object.getJSONObject("results");
        printPerformance("GET vertex", null, uri, sh.stopWatch());
        assertEquals(object.getString("_id"), "1");
        assertEquals(object.getString("_type"), "vertex");
        assertEquals(object.getString("key1"), "value1");
        assertEquals(object.getString("key2"), "value2");
        assertEquals(object.getString("name"), "REXSTER");

        // to new vertex
        sh.stopWatch();
        uri = createURI("vertices/9999?key1=value1&key2=value2&name=REXSTER");
        object = postResource(uri);
        printPerformance("POST vertex", null, uri, sh.stopWatch());
        object = object.getJSONObject("results");
        assertEquals(object.getString("_id"), "9999");
        assertEquals(object.getString("_type"), "vertex");
        assertEquals(object.getString("key1"), "value1");
        assertEquals(object.getString("key2"), "value2");
        assertEquals(object.getString("name"), "REXSTER");
        uri = createURI("vertices/9999");
        object = getResource(uri);
        object = object.getJSONObject("results");
        printPerformance("GET vertex", null, uri, sh.stopWatch());
        assertEquals(object.getString("_id"), "9999");
        assertEquals(object.getString("_type"), "vertex");
        assertEquals(object.getString("key1"), "value1");
        assertEquals(object.getString("key2"), "value2");
        assertEquals(object.getString("name"), "REXSTER");
    }



    public void testDeleteVertex() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices/1");
        deleteResource(uri);
        printPerformance("DELETE vertex", null, uri, sh.stopWatch());

        sh.stopWatch();
        uri = createURI("vertices/1");
        JSONObject object = getResource(uri);
        printPerformance("GET vertex", null, uri, sh.stopWatch());
        assertNull(object.opt("results"));

        sh.stopWatch();
        for (int i = 0; i < 900; i++) {
            uri = createURI("vertices/" + i);
            deleteResource(uri);
        }
        printPerformance("DELETE vertices", 900, uri, sh.stopWatch());

        sh.stopWatch();
        uri = createURI("vertices");
        object = getResource(uri);
        printPerformance("GET vertices", null, uri, sh.stopWatch());
        assertEquals(object.getJSONArray("results").length(), 0);
    }
}

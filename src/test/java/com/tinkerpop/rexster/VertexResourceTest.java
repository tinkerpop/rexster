package com.tinkerpop.rexster;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
        printPerformance("GET all vertices", 1, uri, sh.stopWatch());
        assertEquals(object.get("total_size"), 809l);
        for (Object vertex : (JSONArray) object.get("result")) {
            assertEquals(((JSONObject) vertex).get("_type"), "vertex");
        }
        assertEquals((long) ((JSONArray) object.get("result")).size(), object.get("total_size"));
    }

    public void testGetVertexIndex() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices?name=Garcia");
        JSONObject object = getResource(uri);
        printPerformance("GET single vertex through index", 1, uri, sh.stopWatch());
        assertEquals(object.get("total_size"), 1l);
        assertEquals((long) ((JSONArray) object.get("result")).size(), object.get("total_size"));
    }

    public void testGetVertexEdges() throws Exception {
        long total = 0l;
        sh.stopWatch();
        String uri = createURI("vertices/1/outE");
        JSONObject object = getResource(uri);
        printPerformance("GET vertex out edges", 1, uri, sh.stopWatch());
        total = total + (Long) object.get("total_size");
        assertEquals((long) ((JSONArray) object.get("result")).size(), object.get("total_size"));

        sh.stopWatch();
        uri = createURI("vertices/1/inE");
        object = getResource(uri);
        printPerformance("GET vertex in edges", 1, uri, sh.stopWatch());
        total = total + (Long) object.get("total_size");
        assertEquals((long) ((JSONArray) object.get("result")).size(), object.get("total_size"));

        sh.stopWatch();
        uri = createURI("vertices/1/bothE");
        object = getResource(uri);
        printPerformance("GET vertex both edges", 1, uri, sh.stopWatch());
        assertEquals(total, ((Long) object.get("total_size")).longValue());
        assertEquals(object.get("total_size"), total);
        assertEquals((long) ((JSONArray) object.get("result")).size(), object.get("total_size"));

    }

    public void testGetVertexEdgesLabelFilter() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices/1/outE?_label=sung_by");
        JSONObject object = getResource(uri);
        printPerformance("GET vertex out edges label filter", 1, uri, sh.stopWatch());
        assertEquals(object.get("total_size"), 1l);
        assertEquals((long) ((JSONArray) object.get("result")).size(), object.get("total_size"));

        sh.stopWatch();
        uri = createURI("vertices/1/outE?_label=written_by");
        object = getResource(uri);
        printPerformance("GET vertex out edges label filter", 1, uri, sh.stopWatch());
        assertEquals(object.get("total_size"), 1l);
        assertEquals((long) ((JSONArray) object.get("result")).size(), object.get("total_size"));
    }

    public void testPostVertex() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices/9999");
        JSONObject object = postResource(uri);
        printPerformance("POST vertex", 1, uri, sh.stopWatch());
        object = (JSONObject) object.get("result");
        assertEquals(object.get("_id"), "9999");
        assertEquals(object.get("_type"), "vertex");
        assertEquals(object.get("_outE"), null);
        uri = createURI("vertices/9999/");
        object = getResource(uri);
        object = (JSONObject) object.get("result");
        assertEquals(object.get("_id"), "9999");
        assertEquals(object.get("_type"), "vertex");
        assertEquals(object.get("_outE"), null);
    }

    public void testPostManyVertices() throws Exception {
        sh.stopWatch();
        int startVertexId = 9999;
        int total = 250;
        for (int i = startVertexId; i < startVertexId + total; i++) {
            String uri = createURI("vertices/" + i);
            JSONObject object = postResource(uri);
            object = (JSONObject) object.get("result");
            assertEquals(object.get("_id"), "" + i);
            assertEquals(object.get("_type"), "vertex");
            assertEquals(object.get("_outE"), null);
        }
        printPerformance("POST vertices", total, "vertices/xxxx", sh.stopWatch());

        sh.stopWatch();
        for (int i = startVertexId; i < startVertexId + total; i++) {
            String uri = createURI("vertices/" + i);
            JSONObject object = getResource(uri);
            object = (JSONObject) object.get("result");
            assertEquals(object.get("_id"), "" + i);
            assertEquals(object.get("_type"), "vertex");
            assertEquals(object.get("_outE"), null);
        }
        printPerformance("GET vertices", total, "vertices/xxxx", sh.stopWatch());
    }

    public void testPostVertexProperties() throws Exception {
        // to existing vertex
        sh.stopWatch();
        String uri = createURI("vertices/1?key1=value1&key2=value2&name=REXSTER");
        JSONObject object = postResource(uri);
        printPerformance("POST vertex", 1, uri, sh.stopWatch());
        object = (JSONObject) object.get("result");
        assertEquals(object.get("_id"), "1");
        assertEquals(object.get("_type"), "vertex");
        assertEquals(object.get("key1"), "value1");
        assertEquals(object.get("key2"), "value2");
        assertEquals(object.get("name"), "REXSTER");
        uri = createURI("vertices/1");
        object = getResource(uri);
        object = (JSONObject) object.get("result");
        printPerformance("GET vertex", 1, uri, sh.stopWatch());
        assertEquals(object.get("_id"), "1");
        assertEquals(object.get("_type"), "vertex");
        assertEquals(object.get("key1"), "value1");
        assertEquals(object.get("key2"), "value2");
        assertEquals(object.get("name"), "REXSTER");

        // to new vertex
        sh.stopWatch();
        uri = createURI("vertices/9999?key1=value1&key2=value2&name=REXSTER");
        object = postResource(uri);
        printPerformance("POST vertex", 1, uri, sh.stopWatch());
        object = (JSONObject) object.get("result");
        assertEquals(object.get("_id"), "9999");
        assertEquals(object.get("_type"), "vertex");
        assertEquals(object.get("key1"), "value1");
        assertEquals(object.get("key2"), "value2");
        assertEquals(object.get("name"), "REXSTER");
        uri = createURI("vertices/9999");
        object = getResource(uri);
        object = (JSONObject) object.get("result");
        printPerformance("GET vertex", 1, uri, sh.stopWatch());
        assertEquals(object.get("_id"), "9999");
        assertEquals(object.get("_type"), "vertex");
        assertEquals(object.get("key1"), "value1");
        assertEquals(object.get("key2"), "value2");
        assertEquals(object.get("name"), "REXSTER");
    }

    public void testPostEdge() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices/1/outE?_label=test&_target=2&key1=value1");
        JSONObject object = postResource(uri);
        printPerformance("POST vertex edge", 1, uri, sh.stopWatch());
        object = (JSONObject) object.get("result");
        assertEquals(object.get("_type"), "edge");
        assertEquals(object.get("_label"), "test");
        assertEquals(object.get("key1"), "value1");
        assertEquals(object.get("_outV"), "1");
        assertEquals(object.get("_inV"), "2");
        uri = createURI("vertices/1/outE?_label=test");
        object = getResource(uri);
        object = (JSONObject) ((JSONArray) object.get("result")).get(0);
        printPerformance("GET vertex edge by label filter", 1, uri, sh.stopWatch());
        assertEquals(object.get("_type"), "edge");
        assertEquals(object.get("_label"), "test");
        assertEquals(object.get("key1"), "value1");
        assertEquals(object.get("_outV"), "1");
        assertEquals(object.get("_inV"), "2");

    }

    public void testPostEdgeProperties() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices/1/outE?_label=test&_target=2&key1=value1");
        JSONObject object = postResource(uri);
        printPerformance("POST vertex edge", 1, uri, sh.stopWatch());
        object = (JSONObject) object.get("result");
        assertEquals(object.get("_type"), "edge");
        assertEquals(object.get("_label"), "test");
        assertEquals(object.get("key1"), "value1");
        assertEquals(object.get("_outV"), "1");
        assertEquals(object.get("_inV"), "2");
        String id = (String) object.get("_id");

        uri = createURI("vertices/1/outE/" + id + "?key2=value2&key3=value3&key1=asdf");
        object = postResource(uri);
        printPerformance("POST vertex edge properties", 1, uri, sh.stopWatch());
        object = (JSONObject) object.get("result");
        assertEquals(object.get("_type"), "edge");
        assertEquals(object.get("_label"), "test");
        assertEquals(object.get("key1"), "asdf");
        assertEquals(object.get("key2"), "value2");
        assertEquals(object.get("key3"), "value3");
        assertEquals(object.get("_outV"), "1");
        assertEquals(object.get("_inV"), "2");

    }
}

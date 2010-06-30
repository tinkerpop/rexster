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
        printPerformance("Single all vertices", 1, uri, sh.stopWatch());
        assertEquals(object.get("total_size"), 809l);
        assertEquals((long) ((JSONArray) object.get("result")).size(), object.get("total_size"));
    }

    public void testGetVertexIndex() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices?name=Garcia");
        JSONObject object = getResource(uri);
        printPerformance("Single vertex through index", 1, uri, sh.stopWatch());
        assertEquals(object.get("total_size"), 1l);
        assertEquals((long) ((JSONArray) object.get("result")).size(), object.get("total_size"));
    }

    public void testGetVertexEdges() throws Exception {
        long total = 0l;

        sh.stopWatch();
        String uri = createURI("vertices/1/outE");
        JSONObject object = getResource(uri);
        printPerformance("Single vertex through index", 1, uri, sh.stopWatch());
        total = total + (Long) object.get("total_size");
        assertEquals((long) ((JSONArray) object.get("result")).size(), object.get("total_size"));

        sh.stopWatch();
        uri = createURI("vertices/1/inE");
        object = getResource(uri);
        printPerformance("Single vertex through index", 1, uri, sh.stopWatch());
        total = total + (Long) object.get("total_size");
        assertEquals((long) ((JSONArray) object.get("result")).size(), object.get("total_size"));

        sh.stopWatch();
        uri = createURI("vertices/1/bothE");
        object = getResource(uri);
        printPerformance("Single vertex through index", 1, uri, sh.stopWatch());
        assertEquals(total, ((Long) object.get("total_size")).longValue());
        assertEquals(object.get("total_size"), total);
        assertEquals((long) ((JSONArray) object.get("result")).size(), object.get("total_size"));

    }

    public void testGetVertexEdgesLabelFilter() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices/1/outE?_label=sung_by");
        JSONObject object = getResource(uri);
        printPerformance("Single vertex through index", 1, uri, sh.stopWatch());
        assertEquals(object.get("total_size"), 1l);
        assertEquals((long) ((JSONArray) object.get("result")).size(), object.get("total_size"));
    }
}

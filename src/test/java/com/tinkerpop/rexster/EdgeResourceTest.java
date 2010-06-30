package com.tinkerpop.rexster;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class EdgeResourceTest extends BaseTest {

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

    public void testGetAllEdges() throws Exception {
        sh.stopWatch();
        String uri = createURI("edges");
        JSONObject object = getResource(uri);
        printPerformance("GET all edges", null, uri, sh.stopWatch());
        for (Object vertex : (JSONArray) object.get("result")) {
            assertEquals(((JSONObject) vertex).get("_type"), "edge");
        }
        assertEquals((long) ((JSONArray) object.get("result")).size(), object.get("total_size"));
    }
}

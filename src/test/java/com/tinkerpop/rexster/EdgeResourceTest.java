package com.tinkerpop.rexster;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

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

    public void testGetEdge() throws Exception {
        sh.stopWatch();
        String uri = createURI("edges/6872");
        JSONObject object = getResource(uri);
        printPerformance("GET edge", null, uri, sh.stopWatch());
        assertEquals(((JSONObject) object.get("result")).get("_type"), "edge");
        assertEquals(((JSONObject) object.get("result")).get("_outV"), "64");
        assertEquals(((JSONObject) object.get("result")).get("_label"), "followed_by");
        assertEquals(((JSONObject) object.get("result")).get("_inV"), "30");
    }

    public void testPostEdge() throws Exception {
        sh.stopWatch();
        String uri = createURI("edges/999999?_outV=1&_inV=2&_label=test&key1=value1");
        JSONObject object = postResource(uri);
        printPerformance("POST edge", null, uri, sh.stopWatch());
        object = (JSONObject) object.get("result");
        assertEquals(object.get("_type"), "edge");
        assertEquals(object.get("_label"), "test");
        assertEquals(object.get("key1"), "value1");
        assertEquals(object.get("_outV"), "1");
        assertEquals(object.get("_inV"), "2");
    }

    public void testPostEdgeProperties() throws Exception {
        sh.stopWatch();
        String uri = createURI("edges/6872?key1=value1");
        JSONObject object = postResource(uri);
        printPerformance("POST edge properties", null, uri, sh.stopWatch());
        object = (JSONObject) object.get("result");
        assertEquals(object.get("_type"), "edge");
        assertEquals(object.get("_label"), "followed_by");
        assertEquals(object.get("key1"), "value1");
        assertEquals(object.get("_outV"), "64");
        assertEquals(object.get("_inV"), "30");
        String id = (String) object.get("_id");

        uri = createURI("edges/6872?key2=value2&key3=value3&key1=asdf");
        object = postResource(uri);
        printPerformance("POST edge properties", null, uri, sh.stopWatch());
        object = (JSONObject) object.get("result");
        assertEquals(object.get("_type"), "edge");
        assertEquals(object.get("_label"), "followed_by");
        assertEquals(object.get("key1"), "asdf");
        assertEquals(object.get("key2"), "value2");
        assertEquals(object.get("key3"), "value3");
        assertEquals(object.get("_outV"), "64");
        assertEquals(object.get("_inV"), "30");

    }

    public void testDeleteEdge() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices/1/outE?rexster.return_keys=[_id]");
        JSONObject object = getResource(uri);
        printPerformance("GET vertex out edges ids", null, uri, sh.stopWatch());
        List<String> edgeIds = new ArrayList<String>();
        for (Object edge : (JSONArray) object.get("result")) {
            edgeIds.add((String) ((JSONObject) edge).get("_id"));
        }

        sh.stopWatch();
        for (String edgeId : edgeIds) {
            uri = createURI("edges/" + edgeId);
            deleteResource(uri);
        }
        printPerformance("DELETE edges", edgeIds.size(), uri, sh.stopWatch());

        sh.stopWatch();
        uri = createURI("vertices/1/outE");
        object = getResource(uri);
        printPerformance("GET vertice out edges", null, uri, sh.stopWatch());
        assertEquals(((JSONArray) object.get("result")).size(), 0);
    }
}

package com.tinkerpop.rexster;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

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
        JSONArray arr = object.getJSONArray("results");
        
        for (int ix = 0; ix < arr.length(); ix ++) {
        	JSONObject vertex = arr.getJSONObject(ix);
            assertEquals(vertex.get("_type"), "edge");
        }
        assertEquals((long) (object.getJSONArray("results")).length(), object.getLong("total_size"));
    }

    public void testGetEdge() throws Exception {
        sh.stopWatch();
        String uri = createURI("edges/6872");
        JSONObject object = getResource(uri);
        printPerformance("GET edge", null, uri, sh.stopWatch());
        assertEquals(((JSONObject) object.get("results")).getString("_type"), "edge");
        assertEquals(((JSONObject) object.get("results")).getString("_outV"), "64");
        assertEquals(((JSONObject) object.get("results")).getString("_label"), "followed_by");
        assertEquals(((JSONObject) object.get("results")).getString("_inV"), "30");
    }

    public void testPostEdge() throws Exception {
        sh.stopWatch();
        String uri = createURI("edges/999999?_outV=1&_inV=2&_label=test&key1=value1");
        JSONObject object = postResource(uri);
        printPerformance("POST edge", null, uri, sh.stopWatch());
        object = (JSONObject) object.get("results");
        assertEquals(object.getString("_type"), "edge");
        assertEquals(object.getString("_label"), "test");
        assertEquals(object.getString("key1"), "value1");
        assertEquals(object.getString("_outV"), "1");
        assertEquals(object.getString("_inV"), "2");
    }

    public void testPostEdgeProperties() throws Exception {
        sh.stopWatch();
        String uri = createURI("edges/6872?key1=value1");
        JSONObject object = postResource(uri);
        printPerformance("POST edge properties", null, uri, sh.stopWatch());
        object = object.getJSONObject("results");
        assertEquals(object.getString("_type"), "edge");
        assertEquals(object.getString("_label"), "followed_by");
        assertEquals(object.getString("key1"), "value1");
        assertEquals(object.getString("_outV"), "64");
        assertEquals(object.getString("_inV"), "30");
        String id = object.getString("_id");

        uri = createURI("edges/6872?key2=value2&key3=value3&key1=asdf");
        object = postResource(uri);
        printPerformance("POST edge properties", null, uri, sh.stopWatch());
        object = object.getJSONObject("results");
        assertEquals(object.getString("_type"), "edge");
        assertEquals(object.getString("_label"), "followed_by");
        assertEquals(object.getString("key1"), "asdf");
        assertEquals(object.getString("key2"), "value2");
        assertEquals(object.getString("key3"), "value3");
        assertEquals(object.getString("_outV"), "64");
        assertEquals(object.getString("_inV"), "30");

    }

    public void testDeleteEdge() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices/1/outE?rexster.return_keys=[_id]");
        JSONObject object = getResource(uri);
        printPerformance("GET vertex out edges ids", null, uri, sh.stopWatch());
        List<String> edgeIds = new ArrayList<String>();
        JSONArray arr = object.getJSONArray("results");
        
        for (int ix = 0; ix < arr.length(); ix ++) {
        	JSONObject edge = arr.getJSONObject(ix);
            edgeIds.add(edge.getString("_id"));
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
        assertEquals(object.getJSONArray("results").length(), 0);
    }
}

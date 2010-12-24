package com.tinkerpop.rexster;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EdgeResourceIntegrationTest extends BaseTest {

    @Before
    public void setUp() {
        try {
            this.startWebServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        try {
            this.stopWebServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getAllEdges() throws Exception {
        sh.stopWatch();
        String uri = createURI("edges");
        JSONObject object = getResource(uri);
        printPerformance("GET all edges", null, uri, sh.stopWatch());
        JSONArray arr = object.getJSONArray("results");

        for (int ix = 0; ix < arr.length(); ix++) {
            JSONObject vertex = arr.getJSONObject(ix);
            Assert.assertEquals("edge", vertex.get("_type"));
        }
        Assert.assertEquals(object.getLong("total_size"), (long) (object.getJSONArray("results")).length());
    }

    @Test
    public void getEdge() throws Exception {
        sh.stopWatch();
        String uri = createURI("edges/6872");
        JSONObject object = getResource(uri);
        printPerformance("GET edge", null, uri, sh.stopWatch());
        Assert.assertEquals("edge", ((JSONObject) object.get("results")).getString("_type"));
        Assert.assertEquals("64", ((JSONObject) object.get("results")).getString("_outV"));
        Assert.assertEquals("followed_by", ((JSONObject) object.get("results")).getString("_label"));
        Assert.assertEquals("30", ((JSONObject) object.get("results")).getString("_inV"));
    }

    @Test
    public void postEdge() throws Exception {
        sh.stopWatch();
        String uri = createURI("edges/999999?_outV=1&_inV=2&_label=test&key1=value1");
        JSONObject object = postResource(uri);
        printPerformance("POST edge", null, uri, sh.stopWatch());
        object = (JSONObject) object.get("results");
        Assert.assertEquals("edge", object.getString("_type"));
        Assert.assertEquals("test", object.getString("_label"));
        Assert.assertEquals("value1", object.getString("key1"));
        Assert.assertEquals("1", object.getString("_outV"));
        Assert.assertEquals("2", object.getString("_inV"));
    }

    @Test
    public void postEdgeProperties() throws Exception {
        sh.stopWatch();
        String uri = createURI("edges/6872?key1=value1");
        JSONObject object = postResource(uri);
        printPerformance("POST edge properties", null, uri, sh.stopWatch());
        object = object.getJSONObject("results");
        Assert.assertEquals("edge", object.getString("_type"));
        Assert.assertEquals("followed_by", object.getString("_label"));
        Assert.assertEquals("value1", object.getString("key1"));
        Assert.assertEquals("64", object.getString("_outV"));
        Assert.assertEquals("30", object.getString("_inV"));
        String id = object.getString("_id");

        uri = createURI("edges/6872?key2=value2&key3=value3&key1=asdf");
        object = postResource(uri);
        printPerformance("POST edge properties", null, uri, sh.stopWatch());
        object = object.getJSONObject("results");
        Assert.assertEquals("edge", object.getString("_type"));
        Assert.assertEquals("followed_by", object.getString("_label"));
        Assert.assertEquals("asdf", object.getString("key1"));
        Assert.assertEquals("value2", object.getString("key2"));
        Assert.assertEquals("value3", object.getString("key3"));
        Assert.assertEquals("64", object.getString("_outV"));
        Assert.assertEquals("30", object.getString("_inV"));

    }

    @Test
    public void deleteEdge() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices/1/outE?rexster.return_keys=[_id]");
        JSONObject object = getResource(uri);
        printPerformance("GET vertex out edges ids", null, uri, sh.stopWatch());
        List<String> edgeIds = new ArrayList<String>();
        JSONArray arr = object.getJSONArray("results");

        for (int ix = 0; ix < arr.length(); ix++) {
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
        Assert.assertEquals(0, object.getJSONArray("results").length());
    }
}

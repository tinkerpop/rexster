package com.tinkerpop.rexster;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class VertexResourceIntegrationTest extends BaseTest {
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
    public void getAllVertices() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices");
        JSONObject object = getResource(uri);
        printPerformance("GET all vertices", null, uri, sh.stopWatch());
        Assert.assertEquals(809l, object.getLong("total_size"));

        JSONArray arr = object.getJSONArray("results");

        for (int ix = 0; ix < arr.length(); ix++) {
            JSONObject vertex = arr.getJSONObject(ix);
            Assert.assertEquals("vertex", vertex.getString("_type"));
        }
        Assert.assertEquals(object.getLong("total_size"), (long) ((JSONArray) object.get("results")).length());
    }

    @Test
    public void getAllVerticesWithOffset() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices?rexster.offset.start=10&rexster.offset.end=20");
        JSONObject object = getResource(uri);
        printPerformance("GET vertices 10-20", null, uri, sh.stopWatch());
        Assert.assertEquals(809l, object.getLong("total_size"));

        JSONArray arr = object.getJSONArray("results");

        for (int ix = 0; ix < arr.length(); ix++) {
            JSONObject vertex = arr.getJSONObject(ix);

            Assert.assertEquals("vertex", vertex.getString("_type"));
        }
        Assert.assertEquals(10, (long) ((JSONArray) object.get("results")).length());
    }

    @Test
    public void getVertexEdges() throws Exception {
        long total = 0l;
        sh.stopWatch();
        String uri = createURI("vertices/1/outE");
        JSONObject object = getResource(uri);
        printPerformance("GET vertex out edges", null, uri, sh.stopWatch());
        total = total + object.getLong("total_size");
        Assert.assertEquals(object.getLong("total_size"), (long) ((JSONArray) object.get("results")).length());

        sh.stopWatch();
        uri = createURI("vertices/1/inE");
        object = getResource(uri);
        printPerformance("GET vertex in edges", null, uri, sh.stopWatch());
        total = total + object.getLong("total_size");
        Assert.assertEquals(object.getLong("total_size"), (long) ((JSONArray) object.get("results")).length());

        sh.stopWatch();
        uri = createURI("vertices/1/bothE");
        object = getResource(uri);
        printPerformance("GET vertex both edges", null, uri, sh.stopWatch());
        Assert.assertEquals(total, object.getLong("total_size"));
        Assert.assertEquals(object.getLong("total_size"), (long) ((JSONArray) object.get("results")).length());

    }

    @Test
    public void getVertexEdgesLabelFilter() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices/1/outE?_label=sung_by");
        JSONObject object = getResource(uri);
        printPerformance("GET vertex out edges label filter", null, uri, sh.stopWatch());
        Assert.assertEquals(1l, object.getLong("total_size"));
        Assert.assertEquals(object.getLong("total_size"), (long) ((JSONArray) object.get("results")).length());

        sh.stopWatch();
        uri = createURI("vertices/1/outE?_label=written_by");
        object = getResource(uri);
        printPerformance("GET vertex out edges label filter", null, uri, sh.stopWatch());
        Assert.assertEquals(1l, object.getLong("total_size"));
        Assert.assertEquals(object.getLong("total_size"), (long) ((JSONArray) object.get("results")).length());
    }

    @Test
    public void postVertex() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices/9999");
        JSONObject object = postResource(uri);
        printPerformance("POST vertex", null, uri, sh.stopWatch());
        object = object.getJSONObject("results");
        Assert.assertEquals("9999", object.getString("_id"));
        Assert.assertEquals("vertex", object.getString("_type"));
        Assert.assertNull(object.opt("_outE"));

        uri = createURI("vertices/9999");
        object = getResource(uri);
        object = object.getJSONObject("results");
        Assert.assertEquals("9999", object.getString("_id"));
        Assert.assertEquals("vertex", object.getString("_type"));
        Assert.assertNull(object.opt("_outE"));
    }

    @Test
    public void postManyVertices() throws Exception {
        sh.stopWatch();
        int startVertexId = 9999;
        int total = 250;
        for (int i = startVertexId; i < startVertexId + total; i++) {
            String uri = createURI("vertices/" + i);
            JSONObject object = postResource(uri);
            object = object.getJSONObject("results");
            Assert.assertEquals("" + i, object.getString("_id"));
            Assert.assertEquals("vertex", object.getString("_type"));
            Assert.assertNull(object.opt("_outE"));
        }
        printPerformance("POST vertices", total, "vertices/xxxx", sh.stopWatch());

        sh.stopWatch();
        for (int i = startVertexId; i < startVertexId + total; i++) {
            String uri = createURI("vertices/" + i);
            JSONObject object = getResource(uri);
            object = object.getJSONObject("results");
            Assert.assertEquals("" + i, object.getString("_id"));
            Assert.assertEquals("vertex", object.getString("_type"));
            Assert.assertNull(object.opt("_outE"));
        }
        printPerformance("GET vertices", total, "vertices/xxxx", sh.stopWatch());
    }

    @Test
    public void postVertexProperties() throws Exception {
        // to existing vertex
        sh.stopWatch();
        String uri = createURI("vertices/1?key1=value1&key2=value2&name=REXSTER");
        JSONObject object = postResource(uri);
        printPerformance("POST vertex", null, uri, sh.stopWatch());
        object = object.getJSONObject("results");
        Assert.assertEquals("1", object.getString("_id"));
        Assert.assertEquals("vertex", object.getString("_type"));
        Assert.assertEquals("value1", object.getString("key1"));
        Assert.assertEquals("value2", object.getString("key2"));
        Assert.assertEquals("REXSTER", object.getString("name"));
        uri = createURI("vertices/1");
        object = getResource(uri);
        object = object.getJSONObject("results");
        printPerformance("GET vertex", null, uri, sh.stopWatch());
        Assert.assertEquals("1", object.getString("_id"));
        Assert.assertEquals("vertex", object.getString("_type"));
        Assert.assertEquals("value1", object.getString("key1"));
        Assert.assertEquals("value2", object.getString("key2"));
        Assert.assertEquals("REXSTER", object.getString("name"));

        // to new vertex
        sh.stopWatch();
        uri = createURI("vertices/9999?key1=value1&key2=value2&name=REXSTER");
        object = postResource(uri);
        printPerformance("POST vertex", null, uri, sh.stopWatch());
        object = object.getJSONObject("results");
        Assert.assertEquals("9999", object.getString("_id"));
        Assert.assertEquals("vertex", object.getString("_type"));
        Assert.assertEquals("value1", object.getString("key1"));
        Assert.assertEquals("value2", object.getString("key2"));
        Assert.assertEquals("REXSTER", object.getString("name"));
        uri = createURI("vertices/9999");
        object = getResource(uri);
        object = object.getJSONObject("results");
        printPerformance("GET vertex", null, uri, sh.stopWatch());
        Assert.assertEquals("9999", object.getString("_id"));
        Assert.assertEquals("vertex", object.getString("_type"));
        Assert.assertEquals("value1", object.getString("key1"));
        Assert.assertEquals("value2", object.getString("key2"));
        Assert.assertEquals("REXSTER", object.getString("name"));
    }

    @Test
    public void deleteVertex() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices/1");
        deleteResource(uri);
        printPerformance("DELETE vertex", null, uri, sh.stopWatch());

        sh.stopWatch();
        uri = createURI("vertices/1");
        JSONObject object = getResource(uri);
        printPerformance("GET vertex", null, uri, sh.stopWatch());

        // this is a not found 404 error so should return a JSON error message
        Assert.assertTrue(object.has("message"));

        int vertexAmount = 809;

        sh.stopWatch();
        for (int i = 0; i < vertexAmount; i++) {
            // the first vertex was delete as the first part of the test
            if (i != 1) {
                uri = createURI("vertices/" + i);
                deleteResource(uri);
            }
        }
        printPerformance("DELETE vertices", vertexAmount, uri, sh.stopWatch());

        sh.stopWatch();
        uri = createURI("vertices");
        object = getResource(uri);
        printPerformance("GET vertices", null, uri, sh.stopWatch());
        Assert.assertEquals(0, object.getJSONArray("results").length());
    }

    /*@Test
    public void deleteAllVertices() throws Exception {
        sh.stopWatch();
        String uri = createURI("vertices");
        System.out.println(uri);
        deleteResource(uri);
        printPerformance("DELETE all vertices", null, uri, sh.stopWatch());

        uri = createURI("vertices");
        JSONObject object = getResource(uri);
        System.out.println(object);
    }*/
}

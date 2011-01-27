package com.tinkerpop.rexster;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class IndexResourceIntegrationTest extends BaseTest {

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
    public void getAllIndices() throws Exception {
        sh.stopWatch();
        String uri = createURI("indices");
        JSONObject object = getResource(uri);
        printPerformance("GET all indices", null, uri, sh.stopWatch());
        JSONArray arr = object.getJSONArray("results");
        Assert.assertEquals(arr.length(), 2);
        for (int ix = 0; ix < arr.length(); ix++) {
            JSONObject index = arr.getJSONObject(ix);
            Assert.assertEquals(index.get("type"), "automatic");
            Assert.assertTrue(index.get("name").equals("edges") || index.get("name").equals("vertices"));
            Assert.assertTrue(index.has("class"));
        }
    }

    @Test
    public void getVertexFromIndex() throws Exception {
        sh.stopWatch();
        String uri = createURI("indices/vertices?key=name&value=DARK%20STAR");
        JSONObject object = getResource(uri);
        printPerformance("GET vertex from index", null, uri, sh.stopWatch());
        JSONArray arr = object.getJSONArray("results");
        Assert.assertEquals(arr.length(), 1);
        Assert.assertEquals(((JSONObject) arr.get(0)).get("name"), "DARK STAR");
    }

    @Test
    public void createIndicesAndManipulateThem() throws Exception {
        sh.stopWatch();
        String uri = createURI("indices/testIndex?class=vertex&type=manual");
        JSONObject object = postResource(uri);
        printPerformance("POST a new manual index", null, uri, sh.stopWatch());
        object = object.getJSONObject("results");
        Assert.assertEquals(object.get("name"), "testIndex");
        Assert.assertEquals(object.get("type"), "manual");
        Assert.assertTrue(object.getString("class").toLowerCase().contains("vertex"));

        sh.stopWatch();
        uri = createURI("indices/testIndex?key=akey&value=(integer,22)&class=vertex&id=89");
        object = postResource(uri);
        printPerformance("POST vertex to index", null, uri, sh.stopWatch());

        uri = createURI("indices/testIndex?key=akey&value=(integer,22)");
        object = getResource(uri);
        JSONArray arr = object.getJSONArray("results");
        Assert.assertEquals(arr.length(), 1);
        Assert.assertEquals(((JSONObject) arr.get(0)).get("name"), "DARK STAR");

        sh.stopWatch();
        uri = createURI("indices/testIndex?key=akey&value=(integer,22)&class=vertex&id=89");
        deleteResource(uri);
        printPerformance("POST vertex to index", null, uri, sh.stopWatch());

        uri = createURI("indices/testIndex?key=akey&value=(integer,22)");
        object = getResource(uri);
        arr = object.getJSONArray("results");
        Assert.assertEquals(arr.length(), 0);

        sh.stopWatch();
        uri = createURI("indices/testIndex?class=vertex&type=manual");
        object = postResource(uri);
        Assert.assertTrue(object.has("message"));
    }


    @Test
    public void testAutomaticIndexKeys() throws Exception {
        sh.stopWatch();
        String uri = createURI("indices/vertices/keys");
        JSONObject object = getResource(uri);
        printPerformance("GET automatic index keys", null, uri, sh.stopWatch());
        JSONArray arr = object.getJSONArray("results");
        Assert.assertEquals(arr.length(), 1);
        Assert.assertEquals(arr.get(0), null);

        sh.stopWatch();
        uri = createURI("indices/testIndex?class=vertex&type=automatic&keys=[name,age]");
        object = postResource(uri);
        printPerformance("POST automatic index", null, uri, sh.stopWatch());
        sh.stopWatch();
        uri = createURI("indices/testIndex/keys");
        object = getResource(uri);
        printPerformance("GET automatic index keys", null, uri, sh.stopWatch());
        arr = object.getJSONArray("results");
        Assert.assertEquals(arr.length(), 2);
        Assert.assertEquals(arr.get(0), "name");
        Assert.assertEquals(arr.get(1), "age");
    }
}
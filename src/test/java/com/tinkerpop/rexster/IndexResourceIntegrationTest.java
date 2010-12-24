package com.tinkerpop.rexster;

import java.util.Arrays;
import java.util.List;

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
        uri = createURI("indices/vertices/keys?key1&key2&key3");
        object = postResource(uri);
        Assert.assertEquals(object.length(), 1); // void return only "version"
        printPerformance("PUT 3 automatic index keys", null, uri, sh.stopWatch());
        uri = createURI("indices/vertices/keys");
        object = getResource(uri);

        arr = object.getJSONArray("results");
        Assert.assertEquals(arr.length(), 3);
        List<String> keys = Arrays.asList("key1", "key2", "key3");
        Assert.assertTrue(keys.contains(arr.get(0)));
        Assert.assertTrue(keys.contains(arr.get(1)));
        Assert.assertTrue(keys.contains(arr.get(2)));

        sh.stopWatch();
        uri = createURI("indices/vertices/keys?key1&key2");
        deleteResource(uri);
        printPerformance("DELETE 2 automatic index keys", null, uri, sh.stopWatch());
        uri = createURI("indices/vertices/keys");
        object = getResource(uri);

        arr = object.getJSONArray("results");
        Assert.assertEquals(arr.length(), 1);
        keys = Arrays.asList("key3");
        Assert.assertTrue(keys.contains(arr.get(0)));

    }
}
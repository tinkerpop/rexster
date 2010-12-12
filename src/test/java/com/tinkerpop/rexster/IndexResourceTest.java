package com.tinkerpop.rexster;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class IndexResourceTest extends BaseTest {

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
        for (int ix = 0; ix < arr.length(); ix++) {
            JSONObject vertex = arr.getJSONObject(ix);
            Assert.assertEquals(vertex.get("name"), "DARK STAR");
        }
    }
}
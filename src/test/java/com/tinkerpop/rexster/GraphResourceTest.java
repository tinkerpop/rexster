package com.tinkerpop.rexster;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class GraphResourceTest extends BaseTest {

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
    public void clearGraph() throws Exception {
        sh.stopWatch();
        String uri = createURI("");
        deleteResource(uri);
        printPerformance("DELETE clear graph", null, uri, sh.stopWatch());

        uri = createURI("vertices");
        JSONObject object = getResource(uri);
        printPerformance("GET all vertices", null, uri, sh.stopWatch());
        Assert.assertEquals(0, object.getLong("total_size"));
    }
}
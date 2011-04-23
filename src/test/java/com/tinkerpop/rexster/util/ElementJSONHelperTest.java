package com.tinkerpop.rexster.util;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraphFactory;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class ElementJSONHelperTest {

    @Test
    public void testMapToJSON() throws Exception {
        Graph graph = TinkerGraphFactory.createTinkerGraph();
        Map map = new LinkedHashMap();
        map.put(graph.getVertex(1), 10);
        map.put(graph.getVertex(2), 3);
        JSONArray array = ElementJSONHelper.convertMap(map, "test", null, false);
        Assert.assertEquals(array.length(), 2);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = (JSONObject) array.get(i);
            if (i == 0) {
                Assert.assertEquals(object.get("name"), "marko");
                Assert.assertEquals(object.get("test"), 10);
            } else {
                Assert.assertEquals(object.get("name"), "vadas");
                Assert.assertEquals(object.get("test"), 3);
            }
        }
    }
}

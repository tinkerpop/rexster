package com.tinkerpop.rexster;

import junit.framework.TestCase;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public class ResultObjectCacheTest extends TestCase {

    public void testElderModel() {
        ResultObjectCache.maxSize = 1000;
        List<String> uuids = new ArrayList<String>();
        for (int i = 0; i < 1000; i++) {
            JSONObject temp = new JSONObject();
            temp.put("value", i);
            UUID uuid = UUID.randomUUID();
            ResultObjectCache.addCachedResult(uuid.toString(), temp);
            uuids.add(uuid.toString());
        }
        int counter = 0;
        for (String uuid : uuids) {
            assertTrue(ResultObjectCache.getCachedResult(uuid) != null);
            assertEquals(ResultObjectCache.getCachedResult(uuid).get("value"), counter);
            counter++;
        }
        assertEquals(ResultObjectCache.getCacheSize(), 1000);
        assertEquals(ResultObjectCache.getCachedResult(uuids.get(0)).get("value"), 0);
        ResultObjectCache.addCachedResult(UUID.randomUUID().toString(), new JSONObject());
        assertNull(ResultObjectCache.getCachedResult(uuids.get(0)));
        assertEquals(ResultObjectCache.getCachedResult(uuids.get(1)).get("value"), 1);
        assertEquals(ResultObjectCache.getCachedResult(uuids.get(1)).get("value"), 1);
        ResultObjectCache.addCachedResult(UUID.randomUUID().toString(), new JSONObject());
        assertNull(ResultObjectCache.getCachedResult(uuids.get(1)));
    }
}

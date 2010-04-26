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
        ResultObjectCache resultObjectCache = new ResultObjectCache();
        List<String> uuids = new ArrayList<String>();
        for (int i = 0; i < 1000; i++) {
            JSONObject temp = new JSONObject();
            temp.put("value", i);
            UUID uuid = UUID.randomUUID();
            resultObjectCache.putCachedResult(uuid.toString(), temp);
            uuids.add(uuid.toString());
        }
        int counter = 0;
        for (String uuid : uuids) {
            assertTrue(resultObjectCache.getCachedResult(uuid) != null);
            assertEquals(resultObjectCache.getCachedResult(uuid).get("value"), counter);
            counter++;
        }
        assertEquals(resultObjectCache.getCacheSize(), 1000);
        assertEquals(resultObjectCache.getCachedResult(uuids.get(0)).get("value"), 0);
        resultObjectCache.putCachedResult(UUID.randomUUID().toString(), new JSONObject());
        assertNull(resultObjectCache.getCachedResult(uuids.get(0)));
        assertEquals(resultObjectCache.getCachedResult(uuids.get(1)).get("value"), 1);
        assertEquals(resultObjectCache.getCachedResult(uuids.get(1)).get("value"), 1);
        resultObjectCache.putCachedResult(UUID.randomUUID().toString(), new JSONObject());
        assertNull(resultObjectCache.getCachedResult(uuids.get(1)));
    }
}

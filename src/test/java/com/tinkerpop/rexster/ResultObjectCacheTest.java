package com.tinkerpop.rexster;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public class ResultObjectCacheTest extends TestCase {

    private static final List<String> uuids = new ArrayList<String>();
    private static final Random random = new Random();
    private static int counter = 0;
    private static final int totalThreads = 100;

    static {
        for (int i = 0; i < 1000; i++) {
            uuids.add(UUID.randomUUID().toString());
        }
    }

    public void testElderModel() throws JSONException {
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

    public void testThreadSafety() {
        ResultObjectCache resultObjectCache = new ResultObjectCache();
        for (int i = 0; i < totalThreads; i++) {
            new Thread(new CacheTester(resultObjectCache)).start();
        }
        while (counter < totalThreads) {
            Thread.yield();
        }

    }

    private class CacheTester implements Runnable {

        private ResultObjectCache cache;
        private static final int totalRuns = 1000;

        public CacheTester(ResultObjectCache cache) {
            this.cache = cache;
        }

        public void run() {
            for (int i = 0; i < totalRuns; i++) {
                JSONObject object = new JSONObject();
                try{
                	object.put("thread", Thread.currentThread().getName());
                } catch (JSONException e) {}
                cache.putCachedResult(uuids.get(random.nextInt(uuids.size())), object);
                cache.getCachedResult(uuids.get(random.nextInt(uuids.size())));
            }
            counter++;
        }
    }
}

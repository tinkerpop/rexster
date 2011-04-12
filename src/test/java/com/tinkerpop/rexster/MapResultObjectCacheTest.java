package com.tinkerpop.rexster;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

/**
 * @author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public class MapResultObjectCacheTest {

    private static final List<String> uuids = new ArrayList<String>();
    private static final Random random = new Random();
    private static int counter = 0;
    private static final int totalThreads = 100;

    static {
        for (int i = 0; i < 1000; i++) {
            uuids.add(UUID.randomUUID().toString());
        }
    }

    @Before
    public void setUp() {
        MapResultObjectCache.maxSize = 1000;
    }

    @Test
    public void constructorNoConfiguredCacheSize() {

        Properties props = new Properties();

        int defaultSize = MapResultObjectCache.maxSize;
        Assert.assertEquals(defaultSize, new MapResultObjectCache(props).maxSize);
    }

    @Test
    public void constructorConfiguredEmptyCacheSize() {

        Properties props = new Properties();
        props.put(Tokens.REXSTER_CACHE_MAXSIZE_PATH, "");

        int defaultSize = MapResultObjectCache.maxSize;
        Assert.assertEquals(defaultSize, new MapResultObjectCache(props).maxSize);
    }

    @Test
    public void constructorConfigureInvalidCacheSize() {

        Properties props = new Properties();
        props.put(Tokens.REXSTER_CACHE_MAXSIZE_PATH, "one hundred");

        int defaultSize = MapResultObjectCache.maxSize;
        Assert.assertEquals(defaultSize, new MapResultObjectCache(props).maxSize);
    }

    @Test
    public void constructorConfigureCacheSize() {

        int expectedCacheSize = 100;
        Properties props = new Properties();
        props.put(Tokens.REXSTER_CACHE_MAXSIZE_PATH, expectedCacheSize);

        Assert.assertEquals(expectedCacheSize, new MapResultObjectCache(props).maxSize);
    }

    @Test
    public void testElderModel() throws JSONException {
        ResultObjectCache resultObjectCache = new MapResultObjectCache();
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
            Assert.assertTrue(resultObjectCache.getCachedResult(uuid) != null);
            Assert.assertEquals(resultObjectCache.getCachedResult(uuid).get("value"), counter);
            counter++;
        }

        Assert.assertEquals(resultObjectCache.getCacheSize(), 1000);
        Assert.assertEquals(resultObjectCache.getCachedResult(uuids.get(0)).get("value"), 0);
        resultObjectCache.putCachedResult(UUID.randomUUID().toString(), new JSONObject());
        Assert.assertNull(resultObjectCache.getCachedResult(uuids.get(0)));
        Assert.assertEquals(resultObjectCache.getCachedResult(uuids.get(1)).get("value"), 1);
        Assert.assertEquals(resultObjectCache.getCachedResult(uuids.get(1)).get("value"), 1);
        resultObjectCache.putCachedResult(UUID.randomUUID().toString(), new JSONObject());
        Assert.assertNull(resultObjectCache.getCachedResult(uuids.get(1)));
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
                try {
                    object.put("thread", Thread.currentThread().getName());
                } catch (JSONException e) {
                }
                cache.putCachedResult(uuids.get(random.nextInt(uuids.size())), object);
                cache.getCachedResult(uuids.get(random.nextInt(uuids.size())));
            }
            counter++;
        }
    }
}

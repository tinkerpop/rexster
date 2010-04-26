package com.tinkerpop.rexster;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public class ResultObjectCache {

    protected static Logger logger = Logger.getLogger(RexsterApplication.class);
    public static int maxSize = 1000;
    private static final Map<String, JSONObject> requestToResultMap = new LinkedHashMap<String, JSONObject>() {
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return this.size() > maxSize;
        }
    };

    public ResultObjectCache() {
        logger.info("Cache constructed with a maximum size of " + ResultObjectCache.maxSize);
    }

    public ResultObjectCache(Properties properties) {
        ResultObjectCache.maxSize = new Integer(properties.getProperty(RexsterTokens.REXSTER_CACHE_MAXSIZE));
        logger.info("Cache constructed with a maximum size of " + ResultObjectCache.maxSize);
    }

    public synchronized JSONObject getCachedResult(String uriRequest) {
        return requestToResultMap.get(uriRequest);
    }

    public synchronized void putCachedResult(String uriRequest, JSONObject resultObject) {
        requestToResultMap.put(uriRequest, resultObject);
    }

    public synchronized int getCacheSize() {
        return requestToResultMap.size();
    }
}

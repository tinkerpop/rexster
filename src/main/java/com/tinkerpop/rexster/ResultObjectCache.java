package com.tinkerpop.rexster;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class ResultObjectCache {

    private static final Logger logger = Logger.getLogger(RexsterApplication.class);
    public static int maxSize = 1000;
    private static final Map<String, JSONObject> requestToResultMap = new LinkedHashMap<String, JSONObject>() {
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return this.size() > maxSize;
        }
    };

    public ResultObjectCache() {
        logger.info("Cache constructed with a maximum size of " + ResultObjectCache.maxSize);
    }

    public ResultObjectCache(final Properties properties) {
        ResultObjectCache.maxSize = new Integer(properties.getProperty(Tokens.REXSTER_CACHE_MAXSIZE_PATH));
        logger.info("Cache constructed with a maximum size of " + ResultObjectCache.maxSize);
    }

    public synchronized JSONObject getCachedResult(final String uriRequest) {
        return requestToResultMap.get(uriRequest);
    }

    public synchronized void putCachedResult(final String uriRequest, final JSONObject resultObject) {
        requestToResultMap.put(uriRequest, resultObject);
    }

    public synchronized int getCacheSize() {
        return requestToResultMap.size();
    }
}

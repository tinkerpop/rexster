package com.tinkerpop.rexster;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class MapResultObjectCache implements ResultObjectCache {
    private static final Logger logger = Logger.getLogger(MapResultObjectCache.class);
    public static int maxSize = 1000;
    private static final Map<String, JSONObject> requestToResultMap = new LinkedHashMap<String, JSONObject>() {
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return this.size() > maxSize;
        }
    };

    public MapResultObjectCache() {
        logger.info("Cache constructed with a maximum size of " + MapResultObjectCache.maxSize);
    }

    public MapResultObjectCache(final Properties properties) {

        Object configuredMaxSizeString = properties.get(Tokens.REXSTER_CACHE_MAXSIZE_PATH);
        try {
            if (configuredMaxSizeString != null) {
                MapResultObjectCache.maxSize = Integer.parseInt(configuredMaxSizeString.toString());
            }
        } catch (NumberFormatException nfe) {
            logger.warn("Cache configuration for " + Tokens.REXSTER_CACHE_MAXSIZE_PATH + " does not contain a valid integer value.", nfe);
        }

        logger.info("Cache constructed with a maximum size of " + MapResultObjectCache.maxSize);
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

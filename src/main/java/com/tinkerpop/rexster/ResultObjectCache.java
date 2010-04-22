package com.tinkerpop.rexster;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public class ResultObjectCache {

    protected static Logger logger = Logger.getLogger(ResultObjectCache.class);
    public static int maxSize = 1000;
    private static final Map<String, JSONObject> requestToResultMap = new LinkedHashMap<String, JSONObject>() {
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return this.size() > maxSize;
        }
    };

    static {
        try {
            Properties properties = new Properties();
            properties.load(RexsterApplication.class.getResourceAsStream("rexster.properties"));
            ResultObjectCache.maxSize = new Integer(properties.getProperty(RexsterTokens.REXSTER_CACHE_MAXSIZE));
            logger.info("Cache constructed with a maximum size of " + ResultObjectCache.maxSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static JSONObject getCachedResult(String uriRequest) {
        return requestToResultMap.get(uriRequest);
    }

    public static void addCachedResult(String uriRequest, JSONObject resultObject) {
        requestToResultMap.put(uriRequest, resultObject);
    }

    public static int getCacheSize() {
        return requestToResultMap.size();
    }
}

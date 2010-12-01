package com.tinkerpop.rexster;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public interface ResultObjectCache {

    public JSONObject getCachedResult(final String uriRequest) ;

    public void putCachedResult(final String uriRequest, final JSONObject resultObject);

    public int getCacheSize();
}

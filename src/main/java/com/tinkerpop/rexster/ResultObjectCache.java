package com.tinkerpop.rexster;

import org.codehaus.jettison.json.JSONObject;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public interface ResultObjectCache {

    public JSONObject getCachedResult(final String uriRequest);

    public void putCachedResult(final String uriRequest, final JSONObject resultObject);

    public int getCacheSize();
}

package com.tinkerpop.rexster.traversals;

import com.tinkerpop.rexster.Tokens;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class AbstractScoreTraversal extends AbstractTraversal {

    protected Float score = Float.NaN;
    
    protected boolean isResultInCache() {
    	boolean inCache = false;
    	JSONObject tempResultObject = this.resultObjectCache.getCachedResult(this.cacheRequestURI);
        if (null != tempResultObject) {
            this.score = (Float) tempResultObject.opt(Tokens.SCORE);
            this.success = true;
        }
        
        return inCache;
    }

    protected void postQuery(boolean resultInCache) throws JSONException {
        if (this.success) {
            this.resultObject.put(Tokens.SCORE, this.score);
            this.cacheCurrentResultObjectState();
        }
        super.postQuery(resultInCache);
    }
}

package com.tinkerpop.rexster.traversals;

import com.tinkerpop.rexster.Tokens;
import org.json.simple.JSONObject;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class AbstractScoreTraversal extends AbstractTraversal {

    protected Float score = Float.NaN;

    protected void preQuery() {
        super.preQuery();
        if (this.allowCached) {
            JSONObject tempResultObject = this.resultObjectCache.getCachedResult(this.cacheRequestURI);
            if (null != tempResultObject) {
                this.score = (Float) tempResultObject.get(Tokens.SCORE);
                this.success = true;
                this.usingCachedResult = true;
            }
        }
    }

    protected void postQuery() {
        if (this.success) {
            this.resultObject.put(Tokens.SCORE, this.score);
        }
        this.cacheCurrentResultObjectState();
        super.postQuery();
    }
}

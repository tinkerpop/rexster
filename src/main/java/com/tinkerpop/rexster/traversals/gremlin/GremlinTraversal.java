package com.tinkerpop.rexster.traversals.gremlin;

import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.gremlin.jsr223.GremlinScriptEngine;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.traversals.AbstractTraversal;
import com.tinkerpop.rexster.traversals.ElementJSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class GremlinTraversal extends AbstractTraversal {

    private ScriptEngine engine = new GremlinScriptEngine();

    private static final String GREMLIN = "gremlin";
    private static final String GRAPH_VARIABLE = "g";
    private static final String WILDCARD = "*";
    private static final String SCRIPT = "script";
    private static final String RETURN_KEYS = "return_keys";
    protected List<String> returnKeys = null;


    public String getTraversalName() {
        return GREMLIN;
    }

    public void traverse() {

        try {
            if (this.requestObject.has(SCRIPT)) {
                String script = this.requestObject.opt(SCRIPT).toString();

                engine.getBindings(ScriptContext.ENGINE_SCOPE).put(GRAPH_VARIABLE, graph);

                JSONArray results = new JSONArray();
                Object result = engine.eval(script);
                if (result instanceof Iterable) {
                    for (Object o : (Iterable) result) {
                        results.put(prepareOutput(o));
                    }
                } else if (result instanceof Iterator) {
                    Iterator itty = (Iterator) result;
                    while (itty.hasNext()) {
                        results.put(prepareOutput(itty.next()));
                    }
                } else {
                    results.put(prepareOutput(result));
                }


                try {
                    this.resultObject.put(Tokens.RESULTS, results);
                } catch (Exception ex) {
                }

                this.success = true;
                this.cacheCurrentResultObjectState();
            } else {
                this.success = false;
                this.message = "no script provided";
            }
        }

        catch (Exception e)

        {
            this.success = false;
            this.message = e.getMessage();
        }

    }

    private Object prepareOutput(Object object) throws JSONException {
        if (object instanceof Element) {
            if (null == this.returnKeys)
                return new ElementJSONObject((Element) object, this.showTypes);
            else
                return new ElementJSONObject((Element) object, this.returnKeys, this.showTypes);
        } else if (object instanceof Number || object instanceof Boolean) {
            return object;
        } else {
            return object.toString();
        }
    }

    public void preQuery() {
        super.preQuery();

        if (this.requestObject.has(RETURN_KEYS)) {
            this.returnKeys = (List<String>) this.requestObject.opt(RETURN_KEYS);
            if (this.returnKeys.size() == 1 && this.returnKeys.get(0).equals(WILDCARD))
                this.returnKeys = null;
        }

    }

    protected boolean isResultInCache() {
        boolean inCache = false;
        JSONObject tempResultObject = this.resultObjectCache.getCachedResult(this.cacheRequestURI);
        if (tempResultObject != null) {
            try {
                this.resultObject.putOpt(Tokens.RESULTS, tempResultObject.opt(Tokens.RESULTS));
            } catch (Exception ex) {
            }
            this.success = true;
        }

        return inCache;
    }

    public void addApiToResultObject() {

        try {
            Map<String, Object> api = new HashMap<String, Object>();
            JSONObject parameters = new JSONObject(super.getParameters());

            parameters.put(SCRIPT, "the Gremlin script to be evaluated");
            parameters.put(RETURN_KEYS, "the element property keys to return (default is to return all element properties)");

            api.put(Tokens.DESCRIPTION, "evaluate an ad-hoc Gremlin script");
            api.put(Tokens.PARAMETERS, parameters);

            this.resultObject.put(Tokens.API, api);

        } catch (JSONException ex) {
            // can't really happen given the hardcoded values
        }
    }
}


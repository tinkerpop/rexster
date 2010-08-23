package com.tinkerpop.rexster.traversals;

import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.rexster.Tokens;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class GremlinTraversal extends AbstractTraversal {

    private ScriptEngine engine = new ScriptEngineManager().getEngineByName(GREMLIN);

    private static final String GREMLIN = "gremlin";
    private static final String ROOT_VARIABLE = "$_";
    private static final String GRAPH_VARIABLE = "$_g";
    private static final String WILDCARD = "*";
    private static final String ROOT = "root";
    private static final String SCRIPT = "script";
    private static final String RETURN_KEYS = "return_keys";
    protected List<String> returnKeys = null;


    public String getTraversalName() {
        return GREMLIN;
    }

    public void traverse() {
        try {
            if (this.requestObject.containsKey(SCRIPT)) {
                String script = this.requestObject.get(SCRIPT).toString();
                if (this.requestObject.containsKey(ROOT)) {
                    Collection<Vertex> roots = getVertices(graph, (Map) this.requestObject.get(ROOT));
                    if (null != roots)
                        engine.getBindings(ScriptContext.ENGINE_SCOPE).put(ROOT_VARIABLE, roots);
                }

                engine.getBindings(ScriptContext.ENGINE_SCOPE).put(GRAPH_VARIABLE, graph);

                JSONArray results = new JSONArray();
                for (Object object : (List) engine.eval(script)) {
                    if (object instanceof Element) {
                        if (null == this.returnKeys)
                            results.add(new ElementJSONObject((Element) object));
                        else
                            results.add(new ElementJSONObject((Element) object, this.returnKeys));
                    } else if (object instanceof Number || object instanceof Boolean) {
                        results.add(object);
                    } else {
                        results.add(JSONValue.escape(object.toString()));
                    }
                }
                this.resultObject.put(Tokens.RESULTS, results);
                this.success = true;
                this.cacheCurrentResultObjectState();
            } else {
                this.success = false;
                this.message = "no script provided";
            }
        } catch (ScriptException e) {
            this.success = false;
            this.message = e.getMessage();
        }
    }

    public void preQuery() {
        super.preQuery();

        if (this.requestObject.containsKey(RETURN_KEYS)) {
            this.returnKeys = (List<String>) this.requestObject.get(RETURN_KEYS);
            if (this.returnKeys.size() == 1 && this.returnKeys.get(0).equals(WILDCARD))
                this.returnKeys = null;
        }

        if (this.allowCached) {
            JSONObject tempResultObject = this.resultObjectCache.getCachedResult(this.cacheRequestURI);
            if (tempResultObject != null) {
                this.resultObject.put(Tokens.RESULTS, tempResultObject.get(Tokens.RESULTS));
                this.success = true;
                this.usingCachedResult = true;
            }
        }
    }

    public void addApiToResultObject() {
        Map<String, Object> api = new HashMap<String, Object>();
        Map<String, Object> parameters = super.getParameters();
        parameters.put(SCRIPT, "the Gremlin script to be evaluated");
        parameters.put(RETURN_KEYS, "the element property keys to return (default is to return all element properties)");
        parameters.put(ROOT + ".<key>", "the elements to set $_ to, where <key> is the element property key");
        api.put(Tokens.DESCRIPTION, "evaluate an ad-hoc Gremlin script");
        api.put(Tokens.PARAMETERS, parameters);
        this.resultObject.put(Tokens.API, api);
    }
}


package com.tinkerpop.rexster.traversals;

import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.rexster.RexsterTokens;
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
    private static final String ROOT = "root";
    private static final String SCRIPT = "script";
    private static final String RESULTS = "results";

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
                    results.add(JSONValue.escape(object.toString()));
                }
                this.resultObject.put(RESULTS, results);
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
        if (this.allowCached) {
            JSONObject tempResultObject = this.resultObjectCache.getCachedResult(this.cacheRequestURI);
            if (tempResultObject != null) {
                this.resultObject.put(RESULTS, tempResultObject.get(RESULTS));
                this.success = true;
                this.usingCachedResult = true;
            }
        }
    }

    public void addApiToResultObject() {
        Map<String, Object> api = new HashMap<String, Object>();
        Map<String, Object> parameters = super.getParameters();
        parameters.put(SCRIPT, "the Gremlin script to be evaluated");
        parameters.put(ROOT + ".<key>", "the elements to set $_ to, where <key> is the element property key");
        api.put(RexsterTokens.DESCRIPTION, "evaluate an ad-hoc Gremlin script");
        api.put(RexsterTokens.PARAMETERS, parameters);
        this.resultObject.put(RexsterTokens.API, api);
    }
}


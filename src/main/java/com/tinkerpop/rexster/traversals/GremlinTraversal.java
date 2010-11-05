package com.tinkerpop.rexster.traversals;

import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.rexster.Tokens;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

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
            if (this.requestObject.has(SCRIPT)) {
                String script = this.requestObject.opt(SCRIPT).toString();
                if (this.requestObject.has(ROOT)) {
                    Collection<Vertex> roots = getVertices(graph, (JSONObject) this.requestObject.opt(ROOT));
                    if (null != roots)
                        engine.getBindings(ScriptContext.ENGINE_SCOPE).put(ROOT_VARIABLE, roots);
                }

                engine.getBindings(ScriptContext.ENGINE_SCOPE).put(GRAPH_VARIABLE, graph);

                JSONArray results = new JSONArray();
                for (Object object : (List) engine.eval(script)) {
                    if (object instanceof Element) {
                        if (null == this.returnKeys)
                            results.put(new ElementJSONObject((Element) object));
                        else
                            results.put(new ElementJSONObject((Element) object, this.returnKeys));
                    } else if (object instanceof Number || object instanceof Boolean) {
                        results.put(object);
                    } else {
                        results.put(object.toString());
                    }
                }
                
                try {
                	this.resultObject.put(Tokens.RESULTS, results);
                } catch (Exception ex) {}
                
                this.success = true;
                this.cacheCurrentResultObjectState();
            } else {
                this.success = false;
                this.message = "no script provided";
            }
        } catch (ScriptException e) {
            this.success = false;
            this.message = e.getMessage();
        } catch (JSONException ex) {
            this.success = false;
            this.message = ex.getMessage();
        }
    }

    public void preQuery() {
        super.preQuery();

        if (this.requestObject.has(RETURN_KEYS)) {
            this.returnKeys = (List<String>) this.requestObject.opt(RETURN_KEYS);
            if (this.returnKeys.size() == 1 && this.returnKeys.get(0).equals(WILDCARD))
                this.returnKeys = null;
        }

        if (this.allowCached) {
            JSONObject tempResultObject = this.resultObjectCache.getCachedResult(this.cacheRequestURI);
            if (tempResultObject != null) {
            	try {
            		this.resultObject.putOpt(Tokens.RESULTS, tempResultObject.opt(Tokens.RESULTS));
            	} catch (Exception ex) {}
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
        
        try {
        	this.resultObject.put(Tokens.API, api);
        } catch (Exception ex) {}
    }
}


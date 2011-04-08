package com.tinkerpop.rexster.extension;

import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.gremlin.jsr223.GremlinScriptEngine;
import com.tinkerpop.rexster.ResultObjectCache;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.traversals.ElementJSONObject;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.ws.rs.core.Response;
import java.util.*;

@ExtensionNaming(name = "gremlin", namespace = "tp")
@ExtensionDescriptor("Gremlin extension.")
public class GremlinExtension implements RexsterExtension{
    protected static Logger logger = Logger.getLogger(GremlinExtension.class);

    private ScriptEngine engine = new GremlinScriptEngine();

    private static final String GREMLIN = "gremlin";
    private static final String GRAPH_VARIABLE = "g";
    private static final String WILDCARD = "*";
    private static final String SCRIPT = "script";
    private static final String RETURN_KEYS = "return_keys";


    @ExtensionDefinition(extensionPoint = ExtensionPoint.EDGE)
    @ExtensionDescriptor("Gremlin extension for an edge.")
    public ExtensionResponse doGremlinWorkOnEdge(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                                 @RexsterContext Edge edge){
        return ExtensionResponse.override(Response.ok().build());
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.VERTEX)
    @ExtensionDescriptor("Gremlin extension for an vertex.")
    public ExtensionResponse doGremlinWorkOnVertex(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                                   @RexsterContext Vertex vertex){
        return ExtensionResponse.override(Response.ok().build());
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH)
    @ExtensionDescriptor("Gremlin extension for a graph.")
    public ExtensionResponse doGremlinWorkOnGraph(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                                  @RexsterContext Graph graph){

        String cacheRequestURI = this.createCacheRequestURI(rexsterResourceContext);
        boolean allowCached = rexsterResourceContext.getRequestObject().optBoolean(Tokens.ALLOW_CACHED, true);
        boolean showTypes = rexsterResourceContext.getRequestObject().optBoolean(Tokens.SHOW_TYPES, false);

        JSONObject requestObject = rexsterResourceContext.getRequestObject();
        JSONObject resultObject = rexsterResourceContext.getResultObject();

        List<String> returnKeys = null;
        if (requestObject.has(RETURN_KEYS)) {
        	JSONArray list = requestObject.optJSONArray(RETURN_KEYS);
            returnKeys = new ArrayList<String>();

            if (list != null) {
                for (int ix = 0; ix < list.length(); ix++) {
                	returnKeys.add(list.optString(ix));
                }
            } else {
            	returnKeys = null;
            }

            if (returnKeys != null && returnKeys.size() == 1
            		&& returnKeys.get(0).equals(WILDCARD)) {
                returnKeys = null;
            }
        }

        ResultObjectCache cache = rexsterResourceContext.getCache();
        JSONObject cachedResultObject = cache.getCachedResult(cacheRequestURI);

        // if the request does not want cached or the result is not in the cache
        // then go ahead and traverse
        if (!allowCached || cachedResultObject == null) {
            try {
                if (requestObject.has(SCRIPT)) {
                    String script = requestObject.opt(SCRIPT).toString();

                    engine.getBindings(ScriptContext.ENGINE_SCOPE).put(GRAPH_VARIABLE, graph);

                    JSONArray results = new JSONArray();
                    Object result = engine.eval(script);
                    if (result instanceof Iterable) {
                        for (Object o : (Iterable) result) {
                            results.put(prepareOutput(o, returnKeys, showTypes));
                        }
                    } else if (result instanceof Iterator) {
                        Iterator itty = (Iterator) result;
                        while (itty.hasNext()) {
                            results.put(prepareOutput(itty.next(), returnKeys, showTypes));
                        }
                    } else {
                        results.put(prepareOutput(result, returnKeys, showTypes));
                    }

                    try {
                        resultObject.put(Tokens.SUCCESS, true);
                        resultObject.put(Tokens.RESULTS, results);
                    } catch (Exception ex) {
                        // todo: not really much that could go wrong here.  bug in jettison?
                    }

                    this.cacheCurrentResultObjectState(rexsterResourceContext, cacheRequestURI, resultObject);
                } else {
                    returnErrorMessageInResult(resultObject, "no script provided");
                }

            } catch (Exception e) {
                returnErrorMessageInResult(resultObject, e.getMessage());
            }
        } else {
            // return cached results
            try {
                resultObject.putOpt(Tokens.RESULTS, cachedResultObject.opt(Tokens.RESULTS));
            } catch (Exception ex) {
                // todo: not really much that could go wrong here.  bug in jettison?
            }
        }

        return ExtensionResponse.override(Response.ok(resultObject).build());
    }

    private void returnErrorMessageInResult(JSONObject resultObject, String message) {
        try {
            resultObject.put(Tokens.SUCCESS, false);
            resultObject.put(Tokens.MESSAGE, message);
        } catch (Exception ex) {
            // todo: not really much that could go wrong here.  bug in jettison?
        }
    }

    private Map<String, Object> getParameters() {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens.ALLOW_CACHED, "allow a previously cached result to be provided (default is true)");
        parameters.put(Tokens.SHOW_TYPES, "displays the properties of the elements with their native data type (default is false)");
        return parameters;
    }

    private void addApiToResultObject(JSONObject resultObject) {

        try {
            Map<String, Object> api = new HashMap<String, Object>();
            JSONObject parameters = new JSONObject(this.getParameters());

            parameters.put(SCRIPT, "the Gremlin script to be evaluated");
            parameters.put(RETURN_KEYS, "the element property keys to return (default is to return all element properties)");

            api.put(Tokens.DESCRIPTION, "evaluate an ad-hoc Gremlin script");
            api.put(Tokens.PARAMETERS, parameters);

            resultObject.put(Tokens.API, api);

        } catch (JSONException ex) {
            // can't really happen given the hardcoded values
        }
    }

    private void cacheCurrentResultObjectState(RexsterResourceContext ctx, String cacheRequestURI, JSONObject resultObject) {
        ArrayList<String> keysToCopy = new ArrayList<String>();
        Iterator<String> keys = resultObject.keys();
        while (keys.hasNext()) {
            keysToCopy.add(keys.next());
        }

        String[] toCopy = new String[keysToCopy.size()];
        toCopy = keysToCopy.toArray(toCopy);

        try {
            JSONObject tempResultObject = new JSONObject(resultObject, toCopy);
            ctx.getCache().putCachedResult(cacheRequestURI, tempResultObject);
        } catch (JSONException ex) {
            // can't cache
            logger.warn("Could not cache result for: " + cacheRequestURI, ex);
        }

    }

    private Object prepareOutput(Object object, List<String> returnKeys, boolean showTypes) throws JSONException {
        if (object instanceof Element) {
            if (returnKeys == null) {
                return new ElementJSONObject((Element) object, showTypes);
            } else {
                return new ElementJSONObject((Element) object, returnKeys, showTypes);
            }
        } else if (object instanceof Number || object instanceof Boolean) {
            return object;
        } else {
            return object.toString();
        }
    }

    private String createCacheRequestURI(RexsterResourceContext ctx) {
        Map<String, String> queryParameters = ctx.getRequest().getParameterMap();

        List<Map.Entry<String, String>> list = new ArrayList<Map.Entry<String, String>>();
        for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
            if (entry.getKey() != Tokens.OFFSET_START && entry.getKey() != Tokens.OFFSET_END && entry.getKey() != Tokens.ALLOW_CACHED && entry.getKey() != Tokens.SHOW_TYPES) {
                list.add(entry);
            }
        }

        java.util.Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
            public int compare(Map.Entry<String, String> e, Map.Entry<String, String> e1) {
                return e.getKey().compareTo(e1.getKey());
            }
        });

        return ctx.getUriInfo().getBaseUri().toString() + list.toString();
    }
}

package com.tinkerpop.rexster.extension;

import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.gremlin.jsr223.GremlinScriptEngine;
import com.tinkerpop.rexster.ResultObjectCache;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.ElementJSONObject;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import java.util.*;

@ExtensionNaming(name = "gremlin", namespace = "tp")
public class GremlinExtension extends AbstractRexsterExtension{
    protected static Logger logger = Logger.getLogger(GremlinExtension.class);

    private ScriptEngine engine = new GremlinScriptEngine();

    private static final String GRAPH_VARIABLE = "g";
    private static final String VERTEX_VARIABLE = "v";
    private static final String EDGE_VARIABLE = "e";
    private static final String WILDCARD = "*";


    @ExtensionDefinition(extensionPoint = ExtensionPoint.EDGE)
    @ExtensionDescriptor("Gremlin extension for an edge.")
    public ExtensionResponse doGremlinWorkOnEdge(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                                 @RexsterContext Graph graph,
                                                 @RexsterContext Edge edge){
        return tryExecuteGremlinScript(rexsterResourceContext, graph, null, edge);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.VERTEX)
    @ExtensionDescriptor("Gremlin extension for a vertex.")
    public ExtensionResponse doGremlinWorkOnVertex(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                                   @RexsterContext Graph graph,
                                                   @RexsterContext Vertex vertex){
        return tryExecuteGremlinScript(rexsterResourceContext, graph, vertex, null);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH)
    @ExtensionDescriptor("Gremlin extension for a graph.")
    public ExtensionResponse doGremlinWorkOnGraph(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                                  @RexsterContext Graph graph){

        return tryExecuteGremlinScript(rexsterResourceContext, graph, null, null);
    }

    private ExtensionResponse tryExecuteGremlinScript(RexsterResourceContext rexsterResourceContext, Graph graph, Vertex vertex, Edge edge) {
        ExtensionResponse extensionResponse;
        String cacheRequestURI = this.createCacheRequestURI(rexsterResourceContext);
        boolean allowCached = rexsterResourceContext.getRequestObject().optBoolean(Tokens.ALLOW_CACHED, true);
        boolean showTypes = rexsterResourceContext.getRequestObject().optBoolean(Tokens.SHOW_TYPES, false);

        JSONObject requestObject = rexsterResourceContext.getRequestObject();

        // read the return keys from the request object
        List<String> returnKeys = getReturnKeys(requestObject);

        ResultObjectCache cache = rexsterResourceContext.getCache();
        JSONObject cachedResultObject = cache.getCachedResult(cacheRequestURI);

        // if the request does not want cached or the result is not in the cache
        // then go ahead and traverse
        if (!allowCached || cachedResultObject == null) {
            try {
                if (requestObject.has(SCRIPT)) {
                    String script = requestObject.opt(SCRIPT).toString();

                    engine.getBindings(ScriptContext.ENGINE_SCOPE).put(GRAPH_VARIABLE, graph);
                    if (vertex != null) {
                        engine.getBindings(ScriptContext.ENGINE_SCOPE).put(VERTEX_VARIABLE, vertex);
                    }

                    if (edge != null) {
                        engine.getBindings(ScriptContext.ENGINE_SCOPE).put(EDGE_VARIABLE, edge);
                    }

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

                    HashMap<String, Object> resultMap = new HashMap<String, Object>();
                    resultMap.put(Tokens.SUCCESS, true);
                    resultMap.put(Tokens.RESULTS, results);

                    JSONObject resultObject = new JSONObject(resultMap);
                    extensionResponse = ExtensionResponse.ok(resultObject);

                    this.cacheCurrentResultObjectState(rexsterResourceContext, cacheRequestURI, resultObject);

                } else {
                    extensionResponse = ExtensionResponse.error("no script provided", generateErrorJson());
                }

            } catch (Exception e) {
                extensionResponse = ExtensionResponse.error(e, generateErrorJson());
            }
        } else {
            // return cached results
            HashMap<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(Tokens.SUCCESS, true);
            resultMap.put(Tokens.RESULTS, cachedResultObject.opt(Tokens.RESULTS));

            JSONObject resultObject = new JSONObject(resultMap);
            extensionResponse = ExtensionResponse.ok(resultObject);
        }

        return extensionResponse;
    }

    protected JSONObject generateApiJson() {

        Map<String, Object> api = new HashMap<String, Object>();

        Map<String, Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put(Tokens.ALLOW_CACHED, "allow a previously cached result to be provided (default is true)");
        parameterMap.put(Tokens.SHOW_TYPES, "displays the properties of the elements with their native data type (default is false)");
        parameterMap.put(SCRIPT, "the Gremlin script to be evaluated");
        parameterMap.put(RETURN_KEYS, "the element property keys to return (default is to return all element properties)");

        JSONObject parameters = new JSONObject(parameterMap);

        api.put(Tokens.DESCRIPTION, "evaluate an ad-hoc Gremlin script");
        api.put(Tokens.PARAMETERS, parameters);

        return new JSONObject(api);
    }

    private List<String> getReturnKeys(JSONObject requestObject) {
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
        return returnKeys;
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

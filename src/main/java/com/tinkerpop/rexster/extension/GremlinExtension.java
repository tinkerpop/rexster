package com.tinkerpop.rexster.extension;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.util.json.GraphSONFactory;
import com.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngine;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.gremlin.converter.JSONResultConverter;
import com.tinkerpop.rexster.protocol.EngineController;
import com.tinkerpop.rexster.protocol.EngineHolder;
import com.tinkerpop.rexster.util.ElementHelper;
import com.tinkerpop.rexster.util.RequestObjectHelper;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.SimpleBindings;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@ExtensionNaming(namespace = "tp", name = "gremlin")
public class GremlinExtension extends AbstractRexsterExtension {
    protected static Logger logger = Logger.getLogger(GremlinExtension.class);

    private static final EngineController enginerController = EngineController.getInstance();
    private static final String GRAPH_VARIABLE = "g";
    private static final String VERTEX_VARIABLE = "v";
    private static final String EDGE_VARIABLE = "e";
    private static final String WILDCARD = "*";
    private static final String SCRIPT = "script";
    private static final String LANGUAGE = "language";
    private static final String PARAMS = "params";

    private static final String API_SHOW_TYPES = "displays the properties of the elements with their native data type (default is false)";
    private static final String API_SCRIPT = "the Gremlin script to be evaluated";
    private static final String API_RETURN_KEYS = "an array of element property keys to return (default is to return all element properties)";
    private static final String API_START_OFFSET = "start index for a paged set of data to be returned";
    private static final String API_END_OFFSET = "end index for a paged set of data to be returned";
    private static final String API_LANGUAGE = "the gremlin language flavor to use (default to groovy)";
    private static final String API_PARAMS = "a map of parameters to bind to the script engine";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.EDGE, method = HttpMethod.GET)
    @ExtensionDescriptor(description = "evaluate an ad-hoc Gremlin script for an edge.",
            api = {
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                    @ExtensionApi(parameterName = LANGUAGE, description = API_LANGUAGE),
                    @ExtensionApi(parameterName = PARAMS, description = API_PARAMS),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.RETURN_KEYS, description = API_RETURN_KEYS),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_START, description = API_START_OFFSET),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_END, description = API_END_OFFSET)
            })
    public ExtensionResponse evaluateGetOnEdge(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                               @RexsterContext Graph graph,
                                               @RexsterContext Edge edge,
                                               @ExtensionRequestParameter(name = SCRIPT, description = API_SCRIPT, parseToJson = false) String script) {
        return tryExecuteGremlinScript(rexsterResourceContext, graph, null, edge, script);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.EDGE, method = HttpMethod.POST)
    @ExtensionDescriptor(description = "evaluate an ad-hoc Gremlin script for an edge.",
            api = {
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                    @ExtensionApi(parameterName = LANGUAGE, description = API_LANGUAGE),
                    @ExtensionApi(parameterName = PARAMS, description = API_PARAMS),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.RETURN_KEYS, description = API_RETURN_KEYS),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_START, description = API_START_OFFSET),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_END, description = API_END_OFFSET)
            })
    public ExtensionResponse evaluatePostOnEdge(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                                @RexsterContext Graph graph,
                                                @RexsterContext Edge edge,
                                                @ExtensionRequestParameter(name = SCRIPT, description = API_SCRIPT, parseToJson = false) String script) {
        return tryExecuteGremlinScript(rexsterResourceContext, graph, null, edge, script);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.VERTEX, method = HttpMethod.GET)
    @ExtensionDescriptor(description = "evaluate an ad-hoc Gremlin script for a vertex.",
            api = {
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                    @ExtensionApi(parameterName = LANGUAGE, description = API_LANGUAGE),
                    @ExtensionApi(parameterName = PARAMS, description = API_PARAMS),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.RETURN_KEYS, description = API_RETURN_KEYS),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_START, description = API_START_OFFSET),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_END, description = API_END_OFFSET)
            })
    public ExtensionResponse evaluateGetOnVertex(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                                 @RexsterContext Graph graph,
                                                 @RexsterContext Vertex vertex,
                                                 @ExtensionRequestParameter(name = SCRIPT, description = API_SCRIPT, parseToJson = false) String script) {
        return tryExecuteGremlinScript(rexsterResourceContext, graph, vertex, null, script);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.VERTEX, method = HttpMethod.POST)
    @ExtensionDescriptor(description = "evaluate an ad-hoc Gremlin script for a vertex.",
            api = {
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                    @ExtensionApi(parameterName = LANGUAGE, description = API_LANGUAGE),
                    @ExtensionApi(parameterName = PARAMS, description = API_PARAMS),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.RETURN_KEYS, description = API_RETURN_KEYS),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_START, description = API_START_OFFSET),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_END, description = API_END_OFFSET)
            })
    public ExtensionResponse evaluatePostOnVertex(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                                  @RexsterContext Graph graph,
                                                  @RexsterContext Vertex vertex,
                                                  @ExtensionRequestParameter(name = SCRIPT, description = API_SCRIPT, parseToJson = false) String script) {
        return tryExecuteGremlinScript(rexsterResourceContext, graph, vertex, null, script);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.GET)
    @ExtensionDescriptor(description = "evaluate an ad-hoc Gremlin script for a graph.",
            api = {
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                    @ExtensionApi(parameterName = LANGUAGE, description = API_LANGUAGE),
                    @ExtensionApi(parameterName = PARAMS, description = API_PARAMS),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.RETURN_KEYS, description = API_RETURN_KEYS),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_START, description = API_START_OFFSET),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_END, description = API_END_OFFSET)
            })
    public ExtensionResponse evaluateGetOnGraph(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                                @RexsterContext Graph graph,
                                                @ExtensionRequestParameter(name = SCRIPT, description = API_SCRIPT, parseToJson = false) String script) {
        return tryExecuteGremlinScript(rexsterResourceContext, graph, null, null, script);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.POST)
    @ExtensionDescriptor(description = "evaluate an ad-hoc Gremlin script for a graph.",
            api = {
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                    @ExtensionApi(parameterName = LANGUAGE, description = API_LANGUAGE),
                    @ExtensionApi(parameterName = PARAMS, description = API_PARAMS),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.RETURN_KEYS, description = API_RETURN_KEYS),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_START, description = API_START_OFFSET),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_END, description = API_END_OFFSET)
            })
    public ExtensionResponse evaluatePostOnGraph(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                                 @RexsterContext Graph graph,
                                                 @ExtensionRequestParameter(name = SCRIPT, description = API_SCRIPT, parseToJson = false) String script) {
        return tryExecuteGremlinScript(rexsterResourceContext, graph, null, null, script);
    }

    private ExtensionResponse tryExecuteGremlinScript(RexsterResourceContext rexsterResourceContext,
                                                      Graph graph, Vertex vertex, Edge edge,
                                                      String script) {
        ExtensionResponse extensionResponse;

        final JSONObject requestObject = rexsterResourceContext.getRequestObject();

        final boolean showTypes = RequestObjectHelper.getShowTypes(requestObject);
        final long offsetStart = RequestObjectHelper.getStartOffset(requestObject);
        final long offsetEnd = RequestObjectHelper.getEndOffset(requestObject);

        // read the return keys from the request object
        final List<String> returnKeys = RequestObjectHelper.getReturnKeys(requestObject, WILDCARD);

        final String languageToExecuteWith = getLanguageToExecuteWith(requestObject);

        final Bindings bindings = new SimpleBindings();
        bindings.put(GRAPH_VARIABLE, graph);

        if (vertex != null) {
            bindings.put(VERTEX_VARIABLE, vertex);
        }

        if (edge != null) {
            bindings.put(EDGE_VARIABLE, edge);
        }

        // add all keys not defined by this request as bindings to the script engine
        placeParametersOnBinding(requestObject, bindings, showTypes);

        final ExtensionMethod extensionMethod = rexsterResourceContext.getExtensionMethod();

        try {
            if (!enginerController.isEngineAvailable(languageToExecuteWith)) {
                 return ExtensionResponse.error("language requested is not available on the server",
                         generateErrorJson(extensionMethod.getExtensionApiAsJson()));
            }

            if (script != null && !script.isEmpty()) {
                final EngineHolder engineHolder = enginerController.getEngineByLanguageName(languageToExecuteWith);
                Object result = engineHolder.getEngine().eval(script, bindings);
                JSONArray results = new JSONResultConverter(showTypes, offsetStart, offsetEnd, returnKeys).convert(result);

                HashMap<String, Object> resultMap = new HashMap<String, Object>();
                resultMap.put(Tokens.SUCCESS, true);
                resultMap.put(Tokens.RESULTS, results);

                JSONObject resultObject = new JSONObject(resultMap);
                extensionResponse = ExtensionResponse.ok(resultObject);

            } else {
                extensionResponse = ExtensionResponse.error(
                        "no script provided",
                        generateErrorJson(extensionMethod.getExtensionApiAsJson()));
            }

        } catch (Exception e) {
            extensionResponse = ExtensionResponse.error(e,
                    generateErrorJson(extensionMethod.getExtensionApiAsJson()));
        }

        return extensionResponse;
    }

    /*
    private static JSONObject getBindingsAsJson(final Bindings bindings) throws Exception{
        final HashMap<String, Object> bindingJsonValues = new HashMap<String, Object>();

        for (String key : bindings.keySet()) {
            if (!key.equals(Tokens.REXSTER) && !key.equals(LANGUAGE) && !key.equals(SCRIPT)
                    && !key.equals(GRAPH_VARIABLE) && !key.equals(EDGE_VARIABLE) && !key.equals(VERTEX_VARIABLE)) {
                bindingJsonValues.put(key, bindings.get(key));
            }
        }

        JSONObject bindingJson = null;
        if (!bindingJsonValues.isEmpty()) {
            bindingJson = new JSONObject(bindingJsonValues);
        }

        return bindingJson;
    }
    */

    private static void placeParametersOnBinding(final JSONObject requestObject, final Bindings bindings, final boolean parseTypes) {
        if (requestObject != null) {
            JSONObject paramMap = requestObject.optJSONObject(PARAMS);
            if (paramMap != null) {
                final Iterator keyIterator = paramMap.keys();
                while (keyIterator.hasNext()) {
                    final String key = (String) keyIterator.next();
                    bindings.put(key, ElementHelper.getTypedPropertyValue(paramMap.opt(key), parseTypes));
                }
            }
        }
    }

    private static String getLanguageToExecuteWith(JSONObject requestObject) {
        final String language = requestObject != null ? requestObject.optString(LANGUAGE) : null;
        String requestedLanguage = "groovy";
        if (language != null && !language.equals("")) {
            requestedLanguage = language;
        }

        return requestedLanguage;
    }
}

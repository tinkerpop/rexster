package com.tinkerpop.rexster.extension;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.gremlin.jsr223.GremlinScriptEngine;
import com.tinkerpop.rexster.ElementJSONObject;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.util.RequestObjectHelper;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.SimpleBindings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@ExtensionNaming(namespace = "tp", name = "gremlin")
public class GremlinExtension extends AbstractRexsterExtension {
    protected static Logger logger = Logger.getLogger(GremlinExtension.class);

    private final static ScriptEngine engine = new GremlinScriptEngine();
    private static final String GRAPH_VARIABLE = "g";
    private static final String VERTEX_VARIABLE = "v";
    private static final String EDGE_VARIABLE = "e";
    private static final String WILDCARD = "*";
    private static final String SCRIPT = "script";

    private static final String API_SHOW_TYPES = "displays the properties of the elements with their native data type (default is false)";
    private static final String API_SCRIPT = "the Gremlin script to be evaluated";
    private static final String API_RETURN_KEYS = "an array of element property keys to return (default is to return all element properties)";
    private static final String API_START_OFFSET = "start index for a paged set of data to be returned";
    private static final String API_END_OFFSET = "end index for a paged set of data to be returned";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.EDGE, method = HttpMethod.GET)
    @ExtensionDescriptor(description = "evaluate an ad-hoc Gremlin script for an edge.",
                         api = {
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.RETURN_KEYS, description = API_RETURN_KEYS),
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_START, description = API_START_OFFSET),
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_END, description = API_END_OFFSET)
                         })
    public ExtensionResponse evaluateGetOnEdge(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                            @RexsterContext Graph graph,
                                            @RexsterContext Edge edge,
                                            @ExtensionRequestParameter(name = SCRIPT, description = API_SCRIPT) String script) {
        return tryExecuteGremlinScript(rexsterResourceContext, graph, null, edge, script);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.EDGE, method = HttpMethod.POST)
    @ExtensionDescriptor(description = "evaluate an ad-hoc Gremlin script for an edge.",
                         api = {
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.RETURN_KEYS, description = API_RETURN_KEYS),
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_START, description = API_START_OFFSET),
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_END, description = API_END_OFFSET)
                         })
    public ExtensionResponse evaluatePostOnEdge(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                            @RexsterContext Graph graph,
                                            @RexsterContext Edge edge,
                                            @ExtensionRequestParameter(name = SCRIPT, description = API_SCRIPT) String script) {
        return tryExecuteGremlinScript(rexsterResourceContext, graph, null, edge, script);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.VERTEX, method = HttpMethod.GET)
    @ExtensionDescriptor(description = "evaluate an ad-hoc Gremlin script for a vertex.",
                         api = {
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.RETURN_KEYS, description = API_RETURN_KEYS),
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_START, description = API_START_OFFSET),
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_END, description = API_END_OFFSET)
                         })
    public ExtensionResponse evaluateGetOnVertex(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                              @RexsterContext Graph graph,
                                              @RexsterContext Vertex vertex,
                                              @ExtensionRequestParameter(name = SCRIPT, description = API_SCRIPT) String script) {
        return tryExecuteGremlinScript(rexsterResourceContext, graph, vertex, null, script);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.VERTEX, method = HttpMethod.POST)
    @ExtensionDescriptor(description = "evaluate an ad-hoc Gremlin script for a vertex.",
                         api = {
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.RETURN_KEYS, description = API_RETURN_KEYS),
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_START, description = API_START_OFFSET),
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_END, description = API_END_OFFSET)
                         })
    public ExtensionResponse evaluatePostOnVertex(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                              @RexsterContext Graph graph,
                                              @RexsterContext Vertex vertex,
                                              @ExtensionRequestParameter(name = SCRIPT, description = API_SCRIPT) String script) {
        return tryExecuteGremlinScript(rexsterResourceContext, graph, vertex, null, script);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.GET)
    @ExtensionDescriptor(description = "evaluate an ad-hoc Gremlin script for a graph.",
                         api = {
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.RETURN_KEYS, description = API_RETURN_KEYS),
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_START, description = API_START_OFFSET),
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_END, description = API_END_OFFSET)
                         })
    public ExtensionResponse evaluateGetOnGraph(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                             @RexsterContext Graph graph,
                                             @ExtensionRequestParameter(name = SCRIPT, description = API_SCRIPT) String script) {
        return tryExecuteGremlinScript(rexsterResourceContext, graph, null, null, script);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.POST)
    @ExtensionDescriptor(description = "evaluate an ad-hoc Gremlin script for a graph.",
                         api = {
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.RETURN_KEYS, description = API_RETURN_KEYS),
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_START, description = API_START_OFFSET),
                             @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.OFFSET_END, description = API_END_OFFSET)
                         })
    public ExtensionResponse evaluatePostOnGraph(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                             @RexsterContext Graph graph,
                                             @ExtensionRequestParameter(name = SCRIPT, description = API_SCRIPT) String script) {
        return tryExecuteGremlinScript(rexsterResourceContext, graph, null, null, script);
    }

    private ExtensionResponse tryExecuteGremlinScript(RexsterResourceContext rexsterResourceContext,
                                                      Graph graph, Vertex vertex, Edge edge,
                                                      String script) {
        ExtensionResponse extensionResponse;

        JSONObject requestObject = rexsterResourceContext.getRequestObject();

        boolean showTypes = RequestObjectHelper.getShowTypes(requestObject);
        long offsetStart = RequestObjectHelper.getStartOffset(requestObject);
        long offsetEnd = RequestObjectHelper.getEndOffset(requestObject);

        Bindings bindings = new SimpleBindings();
        bindings.put(GRAPH_VARIABLE, graph);
        bindings.put(VERTEX_VARIABLE, vertex);
        bindings.put(EDGE_VARIABLE, edge);

        // read the return keys from the request object
        List<String> returnKeys = RequestObjectHelper.getReturnKeys(requestObject, WILDCARD);

        ExtensionMethod extensionMethod = rexsterResourceContext.getExtensionMethod();

        try {
            if (script != null && !script.isEmpty()) {

                JSONArray results = new JSONArray();
                Object result = engine.eval(script, bindings);
                if (result == null) {
                    // for example a script like g.clear()
                    results = null;
                } else if (result instanceof Iterable) {
                    long counter = 0;
                    for (Object o : (Iterable) result) {
                        if (counter >= offsetStart && counter < offsetEnd) {
                            results.put(prepareOutput(o, returnKeys, showTypes));
                        }

                        if (counter >= offsetEnd) {
                            break;
                        }

                        counter++;
                    }
                } else if (result instanceof Iterator) {
                    Iterator itty = (Iterator) result;

                    long counter = 0;
                    while (itty.hasNext()) {
                        if (counter >= offsetStart && counter < offsetEnd) {
                            results.put(prepareOutput(itty.next(), returnKeys, showTypes));
                        }

                        if (counter >= offsetEnd) {
                            break;
                        }

                        counter++;
                    }
                } else {
                    results.put(prepareOutput(result, returnKeys, showTypes));
                }

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
}

package com.tinkerpop.rexster.extension;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.gremlin.converter.JSONResultConverter;
import com.tinkerpop.rexster.protocol.EngineController;
import com.tinkerpop.rexster.protocol.EngineHolder;
import com.tinkerpop.rexster.util.ElementHelper;
import com.tinkerpop.rexster.util.RequestObjectHelper;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@ExtensionNaming(namespace = GremlinExtension.EXTENSION_NAMESPACE, name = GremlinExtension.EXTENSION_NAME)
public class GremlinExtension extends AbstractRexsterExtension {
    protected static final Logger logger = Logger.getLogger(GremlinExtension.class);

    public static final String EXTENSION_NAMESPACE = "tp";
    public static final String EXTENSION_NAME = "gremlin";

    private static final Map<String, String> cachedScripts = new HashMap<String, String>();

    private static final EngineController engineController = EngineController.getInstance();
    private static final String GRAPH_VARIABLE = "g";
    private static final String VERTEX_VARIABLE = "v";
    private static final String EDGE_VARIABLE = "e";
    private static final String WILDCARD = "*";
    private static final String SCRIPT = "script";
    private static final String LANGUAGE = "language";
    private static final String PARAMS = "params";
    private static final String LOAD = "load";

    private static final String API_SHOW_TYPES = "displays the properties of the elements with their native data type (default is false)";
    private static final String API_SCRIPT = "the Gremlin script to be evaluated";
    private static final String API_RETURN_KEYS = "an array of element property keys to return (default is to return all element properties)";
    private static final String API_START_OFFSET = "start index for a paged set of data to be returned";
    private static final String API_END_OFFSET = "end index for a paged set of data to be returned";
    private static final String API_LANGUAGE = "the gremlin language flavor to use (default to groovy)";
    private static final String API_PARAMS = "a map of parameters to bind to the script engine";
    private static final String API_LOAD = "a list of 'stored procedures' to execute prior to the 'script' (if 'script' is not specified then the last script in this argument will return the values";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.EDGE, method = HttpMethod.GET)
    @ExtensionDescriptor(description = "evaluate an ad-hoc Gremlin script for an edge.",
            api = {
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                    @ExtensionApi(parameterName = LANGUAGE, description = API_LANGUAGE),
                    @ExtensionApi(parameterName = PARAMS, description = API_PARAMS),
                    @ExtensionApi(parameterName = LOAD, description = API_LOAD),
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
                    @ExtensionApi(parameterName = LOAD, description = API_LOAD),
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
                    @ExtensionApi(parameterName = LOAD, description = API_LOAD),
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
                    @ExtensionApi(parameterName = LOAD, description = API_LOAD),
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
                    @ExtensionApi(parameterName = LOAD, description = API_LOAD),
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
                    @ExtensionApi(parameterName = LOAD, description = API_LOAD),
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

        final GraphSONMode mode = showTypes ? GraphSONMode.EXTENDED : GraphSONMode.NORMAL;
        final Set<String> returnKeys = RequestObjectHelper.getReturnKeys(requestObject, WILDCARD);

        final String languageToExecuteWith = getLanguageToExecuteWith(requestObject);
        final Bindings bindings = createBindings(graph, vertex, edge);

        // add all keys not defined by this request as bindings to the script engine
        placeParametersOnBinding(requestObject, bindings, showTypes);

        // get the list of "stored procedures" to run
        final RexsterApplicationGraph rag = rexsterResourceContext.getRexsterApplicationGraph();

        final ExtensionMethod extensionMethod = rexsterResourceContext.getExtensionMethod();
        Map configurationMap = null;

        Iterator<String> scriptsToRun = null;
        try {
            final ExtensionConfiguration extensionConfiguration = rag != null ? rag.findExtensionConfiguration(EXTENSION_NAMESPACE, EXTENSION_NAME) : null;
            if (extensionConfiguration != null) {
                configurationMap = extensionConfiguration.tryGetMapFromConfiguration();
                scriptsToRun = getScriptsToRun(requestObject, configurationMap);
            }
        } catch (IOException ioe) {
            return ExtensionResponse.error(ioe,
                    generateErrorJson(extensionMethod.getExtensionApiAsJson()));
        }

        if ((script == null || script.isEmpty()) && scriptsToRun == null) {
            return ExtensionResponse.error(
                    "no scripts provided",
                    generateErrorJson(extensionMethod.getExtensionApiAsJson()));
        }

        try {
            if (!engineController.isEngineAvailable(languageToExecuteWith)) {
                return ExtensionResponse.error("language requested is not available on the server",
                        generateErrorJson(extensionMethod.getExtensionApiAsJson()));
            }

            final EngineHolder engineHolder = engineController.getEngineByLanguageName(languageToExecuteWith);

            // result is either the ad-hoc script on the query string or the last "stored procedure"
            Object result = null;
            if (scriptsToRun != null) {
                while (scriptsToRun.hasNext()) {
                    result = engineHolder.getEngine().eval(scriptsToRun.next(), bindings);
                }
            }

            if (isClientScriptAllowed(configurationMap) && script != null && !script.isEmpty()) {
                result = engineHolder.getEngine().eval(script, bindings);
            }

            final JSONArray results = new JSONResultConverter(mode, offsetStart, offsetEnd, returnKeys).convert(result);

            final HashMap<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(Tokens.SUCCESS, true);
            resultMap.put(Tokens.RESULTS, results);

            final JSONObject resultObject = new JSONObject(resultMap);
            extensionResponse = ExtensionResponse.ok(resultObject);

        } catch (Exception e) {
            logger.error(String.format("Gremlin Extension: %s", e.getMessage()), e);
            extensionResponse = ExtensionResponse.error(e,
                    generateErrorJson(extensionMethod.getExtensionApiAsJson()));
        }

        return extensionResponse;
    }

    private static Bindings createBindings(final Graph graph, final Vertex vertex, final Edge edge) {
        final Bindings bindings = new SimpleBindings();
        bindings.put(GRAPH_VARIABLE, graph);

        if (vertex != null) {
            bindings.put(VERTEX_VARIABLE, vertex);
        }

        if (edge != null) {
            bindings.put(EDGE_VARIABLE, edge);
        }
        return bindings;
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

    private static Iterator<String> getScriptsToRun(JSONObject requestObject, Map configuration) throws IOException {

        if (configuration == null) {
            logger.warn("No scripts are configured for the Gremlin Extension so 'load' query string parameter will be ignored");
            return null;
        }

        if (!configuration.containsKey("scripts")) {
            logger.warn("The configuration suppled for the Gremlin Extension does not contain a 'scripts' key so 'load' query string parameter will be ignored");
            return null;
        }

        boolean scriptsAreCached = areScriptsCached(configuration);

        String scriptLocation = (String) configuration.get("scripts");

        final JSONArray jsonArray = requestObject != null ? requestObject.optJSONArray(LOAD) : null;

        Iterator<String> scripts = null;
        if (jsonArray != null) {
            List<String> scriptList = new ArrayList<String>();
            for (int ix = 0; ix < jsonArray.length(); ix++) {
                final String locationAndScriptFile = scriptLocation + File.separator + jsonArray.optString(ix) + ".gremlin";

                String script = cachedScripts.get(locationAndScriptFile);
                if (script == null) {
                    script = readFile(locationAndScriptFile);

                    if (scriptsAreCached) {
                        cachedScripts.put(locationAndScriptFile, script);
                    }
                }
                scriptList.add(script);
            }

            scripts = scriptList.iterator();
        }

        return scripts;
    }

    private static String readFile(String fileName) throws IOException {

        StringWriter stringWriter = new StringWriter();
        IOUtils.copy(new FileInputStream(new File(fileName)), stringWriter);

        return stringWriter.toString();
    }

    private static boolean isClientScriptAllowed(Map configuration) {
        boolean allowClientScript = true;
        if (configuration != null && configuration.containsKey("allow-client-script")) {
            String configValue = (String) configuration.get("allow-client-script");
            allowClientScript = configValue.toLowerCase().equals("true") ? true : false;
        }

        return allowClientScript;
    }

    private static boolean areScriptsCached(Map configuration) {
        boolean cacheScripts = true;
        if (configuration != null && configuration.containsKey("cache-scripts")) {
            String configValue = (String) configuration.get("cache-scripts");
            cacheScripts = configValue.toLowerCase().equals("true") ? true : false;
        }

        return cacheScripts;
    }
}

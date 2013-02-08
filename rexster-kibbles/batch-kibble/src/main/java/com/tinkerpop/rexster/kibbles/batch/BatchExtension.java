package com.tinkerpop.rexster.kibbles.batch;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.IndexableGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONUtility;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.extension.AbstractRexsterExtension;
import com.tinkerpop.rexster.extension.ExtensionApi;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionMethod;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.HttpMethod;
import com.tinkerpop.rexster.extension.RexsterContext;
import com.tinkerpop.rexster.util.ElementHelper;
import com.tinkerpop.rexster.util.RequestObjectHelper;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This extension allows batch/transactional operations on a graph.
 */
@ExtensionNaming(namespace = BatchExtension.EXTENSION_NAMESPACE, name = BatchExtension.EXTENSION_NAME)
public class BatchExtension extends AbstractRexsterExtension {

    private static final Logger logger = Logger.getLogger(BatchExtension.class);

    public static final String EXTENSION_NAMESPACE = "tp";
    public static final String EXTENSION_NAME = "batch";
    private static final String WILDCARD = "*";

    private static final String API_SHOW_TYPES = "displays the properties of the elements with their native data type (default is false)";
    private static final String API_VALUES = "a list of element identifiers or index values to retrieve from the graph";
    private static final String API_RETURN_KEYS = "an array of element property keys to return (default is to return all element properties)";
    private static final String API_TYPE = "specifies whether to retrieve by identifier or index (default is id)" ;
    private static final String API_KEY = "specifies the index key";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.GET, path = "vertices")
    @ExtensionDescriptor(description = "get a set of vertices from the graph.",
            api = {
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.RETURN_KEYS, description = API_RETURN_KEYS),
                    @ExtensionApi(parameterName = "values", description = API_VALUES),
                    @ExtensionApi(parameterName = "type", description = API_TYPE),
                    @ExtensionApi(parameterName = "key", description = API_KEY)
            })
    public ExtensionResponse getVertices(@RexsterContext final RexsterResourceContext context,
                                         @RexsterContext final Graph graph) {

        final JSONObject requestObject = context.getRequestObject();
        final JSONArray values = requestObject.optJSONArray("values");
        final String type = requestObject.optString("type", "id");
        final String key = requestObject.optString("key");

        final ExtensionResponse error = checkParameters(context, values, type, key);
        if (error != null) {
            return error;
        }

        final boolean showTypes = RequestObjectHelper.getShowTypes(requestObject);
        final GraphSONMode mode = showTypes ? GraphSONMode.EXTENDED : GraphSONMode.NORMAL;
        final Set<String> returnKeys = RequestObjectHelper.getReturnKeys(requestObject, WILDCARD);

        try {

            final JSONArray jsonArray = new JSONArray();

            if (type.equals("id")) {
                for (int ix = 0; ix < values.length(); ix++) {
                    final Vertex vertexFound = graph.getVertex(ElementHelper.getTypedPropertyValue(values.optString(ix)));
                    if (vertexFound != null) {
                        jsonArray.put(GraphSONUtility.jsonFromElement(vertexFound, returnKeys, mode));
                    }
                }
            } else if (type.equals("index")) {
                Index idx = ((IndexableGraph)graph).getIndex(key, Vertex.class);

                for (int ix = 0; ix < values.length(); ix++) {
                    CloseableIterable<Vertex> verticesFound = idx.get(key, ElementHelper.getTypedPropertyValue(values.optString(ix)));
                    for (Vertex vertex : verticesFound) {
                        jsonArray.put(GraphSONUtility.jsonFromElement(vertex, returnKeys, mode));
                    }
                    verticesFound.close();
                }
            } else if (type.equals("keyindex")) {
                for (int ix = 0; ix < values.length(); ix++) {
                    Iterable<Vertex> verticesFound = graph.getVertices(key, ElementHelper.getTypedPropertyValue(values.optString(ix)));
                    for (Vertex vertex : verticesFound) {
                        jsonArray.put(GraphSONUtility.jsonFromElement(vertex, returnKeys, mode));
                    }
                }
            }

            final HashMap<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(Tokens.SUCCESS, true);
            resultMap.put(Tokens.RESULTS, jsonArray);

            final JSONObject resultObject = new JSONObject(resultMap);
            return ExtensionResponse.ok(resultObject);

        } catch (Exception mqe) {
            logger.error(mqe);
            return ExtensionResponse.error(
                    "Error retrieving batch of vertices [" + values + "]", generateErrorJson());
        }

    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.GET, path = "edges")
    @ExtensionDescriptor(description = "get a set of edges from the graph.",
            api = {
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.RETURN_KEYS, description = API_RETURN_KEYS),
                    @ExtensionApi(parameterName = "values", description = API_VALUES),
                    @ExtensionApi(parameterName = "type", description = API_TYPE),
                    @ExtensionApi(parameterName = "key", description = API_KEY)
            })
    public ExtensionResponse getEdges(@RexsterContext final RexsterResourceContext context,
                                      @RexsterContext final Graph graph) {

        final JSONObject requestObject = context.getRequestObject();
        final JSONArray values = requestObject.optJSONArray("values");
        final String type = requestObject.optString("type", "id");
        final String key = requestObject.optString("key");

        final ExtensionResponse error = checkParameters(context, values, type, key);
        if (error != null) {
            return error;
        }

        final boolean showTypes = RequestObjectHelper.getShowTypes(requestObject);
        final GraphSONMode mode = showTypes ? GraphSONMode.EXTENDED : GraphSONMode.NORMAL;
        final Set<String> returnKeys = RequestObjectHelper.getReturnKeys(requestObject, WILDCARD);

        try {

            final JSONArray jsonArray = new JSONArray();

            if (type.equals("id")) {
                for (int ix = 0; ix < values.length(); ix++) {
                    final Edge edgeFound = graph.getEdge(ElementHelper.getTypedPropertyValue(values.optString(ix)));
                    if (edgeFound != null) {
                        jsonArray.put(GraphSONUtility.jsonFromElement(edgeFound, returnKeys, mode));
                    }
                }
            } else if (type.equals("index")) {
                Index idx = ((IndexableGraph)graph).getIndex(key, Edge.class);

                for (int ix = 0; ix < values.length(); ix++) {
                    CloseableIterable<Edge> edgesFound = idx.get(key, ElementHelper.getTypedPropertyValue(values.optString(ix)));
                    for (Edge edge : edgesFound) {
                        jsonArray.put(GraphSONUtility.jsonFromElement(edge, returnKeys, mode));
                    }
                    edgesFound.close();
                }
            } else if (type.equals("keyindex")) {
                for (int ix = 0; ix < values.length(); ix++) {
                    Iterable<Edge> edgesFound = graph.getEdges(key, ElementHelper.getTypedPropertyValue(values.optString(ix)));
                    for (Edge edge : edgesFound) {
                        jsonArray.put(GraphSONUtility.jsonFromElement(edge, returnKeys, mode));
                    }
                }
            }

            final HashMap<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(Tokens.SUCCESS, true);
            resultMap.put(Tokens.RESULTS, jsonArray);

            final JSONObject resultObject = new JSONObject(resultMap);
            return ExtensionResponse.ok(resultObject);

        } catch (Exception mqe) {
            logger.error(mqe);
            return ExtensionResponse.error(
                    "Error retrieving batch of edges [" + values + "]", generateErrorJson());
        }

    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.POST, path = "tx", autoCommitTransaction = true)
    @ExtensionDescriptor(description = "post a transaction to the graph.")
    public ExtensionResponse postTx(@RexsterContext RexsterResourceContext context,
                                    @RexsterContext Graph graph,
                                    @RexsterContext RexsterApplicationGraph rag) {

        final JSONObject transactionJson = context.getRequestObject();
        if (transactionJson == null) {
            final ExtensionMethod extMethod = context.getExtensionMethod();
            return ExtensionResponse.error(
                    "no transaction JSON posted",
                    null,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    null,
                    generateErrorJson(extMethod.getExtensionApiAsJson()));
        }

        try {
            final JSONArray txArray = transactionJson.optJSONArray("tx");
            String currentAction;
            for (int ix = 0; ix < txArray.length(); ix++) {
                final JSONObject txElement = txArray.optJSONObject(ix);
                currentAction = txElement.optString("_action");
                if (currentAction.equals("create")) {
                    create(txElement, graph);
                } else if (currentAction.equals("update")) {
                    update(txElement, graph);
                } else if (currentAction.equals("delete")) {
                    delete(txElement, graph);
                }
            }

            final Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(Tokens.SUCCESS, true);
            resultMap.put("txProcessed", txArray.length());

            return ExtensionResponse.ok(new JSONObject(resultMap));

        } catch (IllegalArgumentException iae) {
            logger.error(iae);

            final ExtensionMethod extMethod = context.getExtensionMethod();
            return ExtensionResponse.error(
                    iae.getMessage(),
                    null,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    null,
                    generateErrorJson(extMethod.getExtensionApiAsJson()));
        } catch (Exception ex) {
            logger.error(ex);
            return ExtensionResponse.error(
                    "Error executing transaction: " + ex.getMessage(), generateErrorJson());
        }
    }

    private void create(final JSONObject elementAsJson, final Graph graph) throws Exception {
        final String id = elementAsJson.optString(Tokens._ID);
        final String elementType = elementAsJson.optString(Tokens._TYPE);

        if (elementType == null) {
            throw new IllegalArgumentException("each element in the transaction must have an " + Tokens._TYPE + " key");
        }

        if (!elementType.equals(Tokens.VERTEX) && !elementType.equals(Tokens.EDGE)) {
            throw new IllegalArgumentException("the " + Tokens._TYPE + " element in the transaction must be either " + Tokens.VERTEX + " or " + Tokens.EDGE);
        }

        Element graphElementCreated = null;
        if (elementType.equals(Tokens.VERTEX)) {
            graphElementCreated = graph.getVertex(id);

            if (graphElementCreated != null) {
                throw new Exception("Vertex with id " + id + " already exists.");
            }

            graphElementCreated = graph.addVertex(id);

        } else if (elementType.equals(Tokens.EDGE)) {

            String inV = null;
            Object temp = elementAsJson.opt(Tokens._IN_V);
            if (null != temp)
                inV = temp.toString();
            String outV = null;
            temp = elementAsJson.opt(Tokens._OUT_V);
            if (null != temp)
                outV = temp.toString();
            String label = null;
            temp = elementAsJson.opt(Tokens._LABEL);
            if (null != temp)
                label = temp.toString();

            if (outV == null || inV == null || outV.isEmpty() || inV.isEmpty()) {
                throw new IllegalArgumentException("an edge must specify a " + Tokens._IN_V + " and " + Tokens._OUT_V);
            }

            graphElementCreated = graph.getEdge(id);

            if (graphElementCreated != null) {
                throw new Exception("Edge with id " + id + " already exists.");
            }

            // there is no edge but the in/out vertex params and label are present so
            // validate that the vertexes are present before creating the edge
            final Vertex out = graph.getVertex(outV);
            final Vertex in = graph.getVertex(inV);
            if (out != null && in != null) {
                // in/out vertexes are found so edge can be created
                graphElementCreated = graph.addEdge(id, out, in, label);
            } else {
                throw new Exception("the " + Tokens._IN_V + " or " + Tokens._OUT_V + " vertices could not be found.");
            }

        }

        if (graphElementCreated != null) {
            Iterator keys = elementAsJson.keys();
            while (keys.hasNext()) {
                String key = keys.next().toString();
                if (!key.startsWith(Tokens.UNDERSCORE)) {
                    graphElementCreated.setProperty(key, ElementHelper.getTypedPropertyValue(elementAsJson.getString(key)));
                }
            }
        }
    }

    private void update(final JSONObject elementAsJson, final Graph graph) throws Exception {
        final String id = elementAsJson.optString(Tokens._ID);
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("each element in the transaction must have an " + Tokens._ID + " key");
        }

        final String elementType = elementAsJson.optString(Tokens._TYPE);

        if (elementType == null) {
            throw new IllegalArgumentException("each element in the transaction must have an " + Tokens._TYPE + " key");
        }

        if (!elementType.equals(Tokens.VERTEX) && !elementType.equals(Tokens.EDGE)) {
            throw new IllegalArgumentException("the " + Tokens._TYPE + " element in the transaction must be either " + Tokens.VERTEX + " or " + Tokens.EDGE);
        }

        Element graphElementUpdated = null;
        if (elementType.equals(Tokens.VERTEX)) {
            graphElementUpdated = graph.getVertex(id);
        } else if (elementType.equals(Tokens.EDGE)) {
            graphElementUpdated = graph.getEdge(id);
        }

        if (graphElementUpdated != null) {
            Iterator keys = elementAsJson.keys();
            while (keys.hasNext()) {
                String key = keys.next().toString();
                if (!key.startsWith(Tokens.UNDERSCORE)) {
                    graphElementUpdated.setProperty(key, ElementHelper.getTypedPropertyValue(elementAsJson.getString(key)));
                }
            }
        }
    }

    private void delete(final JSONObject elementAsJson, final Graph graph) throws Exception {
        final String id = elementAsJson.optString(Tokens._ID);
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("each element in the transaction must have an " + Tokens._ID + " key");
        }

        final String elementType = elementAsJson.optString(Tokens._TYPE);

        if (elementType == null) {
            throw new IllegalArgumentException("each element in the transaction must have an " + Tokens._TYPE + " key");
        }

        if (!elementType.equals(Tokens.VERTEX) && !elementType.equals(Tokens.EDGE)) {
            throw new IllegalArgumentException("the " + Tokens._TYPE + " element in the transaction must be either " + Tokens.VERTEX + " or " + Tokens.EDGE);
        }

        final JSONArray keysToDelete = elementAsJson.optJSONArray("_keys");

        Element graphElementDeleted = null;
        if (elementType.equals(Tokens.VERTEX)) {
            graphElementDeleted = graph.getVertex(id);
        } else if (elementType.equals(Tokens.EDGE)) {
            graphElementDeleted = graph.getEdge(id);
        }

        if (graphElementDeleted != null) {
            if (keysToDelete != null && keysToDelete.length() > 0) {
                // just delete keys from the element
                for (int ix = 0; ix < keysToDelete.length(); ix++) {
                    graphElementDeleted.removeProperty(keysToDelete.optString(ix));
                }
            } else {
                // delete the whole element
                if (elementType.equals(Tokens.VERTEX)) {
                    graph.removeVertex((Vertex) graphElementDeleted);
                } else if (elementType.equals(Tokens.EDGE)) {
                    graph.removeEdge((Edge) graphElementDeleted);
                }
            }
        }
    }

    private ExtensionResponse checkParameters(RexsterResourceContext context, JSONArray values, String type, String key) {
        final ExtensionMethod extMethod = context.getExtensionMethod();
        String errorMessage = null;

        if (values == null || values.length() == 0) {
            errorMessage = "the values parameter cannot be empty";
        }  else if ((type.equals("index") || type.equals("keyindex")) && key.isEmpty()) {
            errorMessage = "the key parameter cannot be empty";
        }

        return (errorMessage != null)
            ? ExtensionResponse.error(
                    errorMessage,
                    null,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    null,
                    extMethod != null ? generateErrorJson(extMethod.getExtensionApiAsJson()) : null)
            : null;
    }
}
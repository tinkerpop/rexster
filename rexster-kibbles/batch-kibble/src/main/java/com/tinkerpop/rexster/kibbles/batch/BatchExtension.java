package com.tinkerpop.rexster.kibbles.batch;


import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.util.io.graphson.GraphSONFactory;
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
import java.util.List;
import java.util.Map;

/**
 * This extension allows batch/transactional operations on a graph.
 */
@ExtensionNaming(namespace = BatchExtension.EXTENSION_NAMESPACE, name = BatchExtension.EXTENSION_NAME)
public class BatchExtension extends AbstractRexsterExtension {

    private static Logger logger = Logger.getLogger(BatchExtension.class);

    public static final String EXTENSION_NAMESPACE = "tp";
    public static final String EXTENSION_NAME = "batch";
    private static final String WILDCARD = "*";

    private static final String API_SHOW_TYPES = "displays the properties of the elements with their native data type (default is false)";
    private static final String API_IDENTIFIERS = "an array of element identifiers to retrieve from the graph";
    private static final String API_RETURN_KEYS = "an array of element property keys to return (default is to return all element properties)";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.GET, path = "vertices")
    @ExtensionDescriptor(description = "get a set of vertices from the graph.",
            api = {
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.RETURN_KEYS, description = API_RETURN_KEYS),
                    @ExtensionApi(parameterName = "idList", description = API_IDENTIFIERS)
            })
    public ExtensionResponse getVertices(@RexsterContext RexsterResourceContext context,
                                         @RexsterContext Graph graph) {

        JSONObject requestObject = context.getRequestObject();
        JSONArray idList = requestObject.optJSONArray("idList");
        if (idList == null || idList.length() == 0) {
            ExtensionMethod extMethod = context.getExtensionMethod();
            return ExtensionResponse.error(
                    "the idList parameter cannot be empty",
                    null,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    null,
                    generateErrorJson(extMethod.getExtensionApiAsJson()));
        }

        boolean showDataTypes = RequestObjectHelper.getShowTypes(requestObject);
        List<String> returnKeys = RequestObjectHelper.getReturnKeys(requestObject, WILDCARD);

        try {

            JSONArray jsonArray = new JSONArray();

            for (int ix = 0; ix < idList.length(); ix++) {
                Vertex vertexFound = graph.getVertex(idList.optString(ix));
                if (vertexFound != null) {
                    jsonArray.put(GraphSONFactory.createJSONElement(vertexFound, returnKeys, showDataTypes));
                }
            }

            HashMap<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(Tokens.SUCCESS, true);
            resultMap.put(Tokens.RESULTS, jsonArray);

            JSONObject resultObject = new JSONObject(resultMap);
            return ExtensionResponse.ok(resultObject);

        } catch (Exception mqe) {
            logger.error(mqe);
            return ExtensionResponse.error(
                    "Error retrieving batch of vertices [" + idList + "]", generateErrorJson());
        }

    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.GET, path = "edges")
    @ExtensionDescriptor(description = "get a set of edges from the graph.",
            api = {
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.RETURN_KEYS, description = API_RETURN_KEYS),
                    @ExtensionApi(parameterName = "idList", description = API_IDENTIFIERS)
            })
    public ExtensionResponse getEdges(@RexsterContext RexsterResourceContext context,
                                      @RexsterContext Graph graph) {

        JSONObject requestObject = context.getRequestObject();
        JSONArray idList = requestObject.optJSONArray("idList");
        if (idList == null || idList.length() == 0) {
            ExtensionMethod extMethod = context.getExtensionMethod();
            return ExtensionResponse.error(
                    "the idList parameter cannot be empty",
                    null,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    null,
                    generateErrorJson(extMethod.getExtensionApiAsJson()));
        }

        boolean showDataTypes = RequestObjectHelper.getShowTypes(requestObject);
        List<String> returnKeys = RequestObjectHelper.getReturnKeys(requestObject, WILDCARD);

        try {

            JSONArray jsonArray = new JSONArray();

            for (int ix = 0; ix < idList.length(); ix++) {
                Edge edgeFound = graph.getEdge(idList.optString(ix));
                if (edgeFound != null) {
                    jsonArray.put(GraphSONFactory.createJSONElement(edgeFound, returnKeys, showDataTypes));
                }
            }

            HashMap<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(Tokens.SUCCESS, true);
            resultMap.put(Tokens.RESULTS, jsonArray);

            JSONObject resultObject = new JSONObject(resultMap);
            return ExtensionResponse.ok(resultObject);

        } catch (Exception mqe) {
            logger.error(mqe);
            return ExtensionResponse.error(
                    "Error retrieving batch of edges [" + idList + "]", generateErrorJson());
        }

    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.POST, path = "tx")
    @ExtensionDescriptor(description = "post a transaction to the graph.")
    public ExtensionResponse postTx(@RexsterContext RexsterResourceContext context,
                                    @RexsterContext Graph graph,
                                    @RexsterContext RexsterApplicationGraph rag) {

        JSONObject transactionJson = context.getRequestObject();
        if (transactionJson == null) {
            ExtensionMethod extMethod = context.getExtensionMethod();
            return ExtensionResponse.error(
                    "no transaction JSON posted",
                    null,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    null,
                    generateErrorJson(extMethod.getExtensionApiAsJson()));
        }

        try {
            rag.tryStartTransaction();

            JSONArray txArray = transactionJson.optJSONArray("tx");
            String currentAction;
            for (int ix = 0; ix < txArray.length(); ix++) {
                JSONObject txElement = txArray.optJSONObject(ix);
                currentAction = txElement.optString("_action");
                if (currentAction.equals("create")) {
                    create(txElement, graph);
                } else if (currentAction.equals("update")) {
                    update(txElement, graph);
                } else if (currentAction.equals("delete")) {
                    delete(txElement, graph);
                }
            }

            rag.tryStopTransactionSuccess();

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(Tokens.SUCCESS, true);
            resultMap.put("txProcessed", txArray.length());

            return ExtensionResponse.ok(new JSONObject(resultMap));

        } catch (IllegalArgumentException iae) {
            rag.tryStopTransactionFailure();

            logger.error(iae);

            ExtensionMethod extMethod = context.getExtensionMethod();
            return ExtensionResponse.error(
                    iae.getMessage(),
                    null,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    null,
                    generateErrorJson(extMethod.getExtensionApiAsJson()));
        } catch (Exception ex) {
            rag.tryStopTransactionFailure();

            logger.error(ex);
            return ExtensionResponse.error(
                    "Error executing transaction: " + ex.getMessage(), generateErrorJson());
        }
    }

    private void create(JSONObject elementAsJson, Graph graph) throws Exception {
        String id = elementAsJson.optString(Tokens._ID);
        String elementType = elementAsJson.optString(Tokens._TYPE);

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

    private void update(JSONObject elementAsJson, Graph graph) throws Exception {
        String id = elementAsJson.optString(Tokens._ID);
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("each element in the transaction must have an " + Tokens._ID + " key");
        }

        String elementType = elementAsJson.optString(Tokens._TYPE);

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

    private void delete(JSONObject elementAsJson, Graph graph) throws Exception {
        String id = elementAsJson.optString(Tokens._ID);
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("each element in the transaction must have an " + Tokens._ID + " key");
        }

        String elementType = elementAsJson.optString(Tokens._TYPE);

        if (elementType == null) {
            throw new IllegalArgumentException("each element in the transaction must have an " + Tokens._TYPE + " key");
        }

        if (!elementType.equals(Tokens.VERTEX) && !elementType.equals(Tokens.EDGE)) {
            throw new IllegalArgumentException("the " + Tokens._TYPE + " element in the transaction must be either " + Tokens.VERTEX + " or " + Tokens.EDGE);
        }

        JSONArray keysToDelete = elementAsJson.optJSONArray("_keys");

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
}

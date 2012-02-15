package com.tinkerpop.rexster.kibbles.sparql;


import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.impls.sail.SailGraph;
import com.tinkerpop.blueprints.pgm.util.io.graphson.GraphSONFactory;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.extension.AbstractRexsterExtension;
import com.tinkerpop.rexster.extension.ExtensionApi;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionMethod;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionRequestParameter;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.RexsterContext;
import com.tinkerpop.rexster.util.RequestObjectHelper;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The SPARQL Extension allows execution of SPARQL queries to Sail implementations.
 */
@ExtensionNaming(namespace = SparqlExtension.EXTENSION_NAMESPACE, name = SparqlExtension.EXTENSION_NAME)
public class SparqlExtension extends AbstractRexsterExtension {

    private static Logger logger = Logger.getLogger(SparqlExtension.class);

    public static final String EXTENSION_NAMESPACE = "tp";
    public static final String EXTENSION_NAME = "sparql";
    private static final String WILDCARD = "*";

    private static final String API_SHOW_TYPES = "displays the properties of the elements with their native data type (default is false)";
    private static final String API_QUERY = "the SPARQL query to be evaluated";
    private static final String API_RETURN_KEYS = "an array of element property keys to return (default is to return all element properties)";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH)
    @ExtensionDescriptor(description = "execute SPARQL queries against a SAIL graph.",
            api = {
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.RETURN_KEYS, description = API_RETURN_KEYS)
            })
    public ExtensionResponse evaluateSparql(@RexsterContext RexsterResourceContext context,
                                            @RexsterContext Graph graph,
                                            @ExtensionRequestParameter(name = "query", description = API_QUERY) String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            ExtensionMethod extMethod = context.getExtensionMethod();
            return ExtensionResponse.error(
                    "the query parameter cannot be empty",
                    null,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    null,
                    generateErrorJson(extMethod.getExtensionApiAsJson()));
        }

        JSONObject requestObject = context.getRequestObject();
        boolean showDataTypes = RequestObjectHelper.getShowTypes(requestObject);
        List<String> returnKeys = RequestObjectHelper.getReturnKeys(requestObject, WILDCARD);

        if (!(graph instanceof SailGraph)) {
            ExtensionMethod extMethod = context.getExtensionMethod();
            return ExtensionResponse.error(
                    "the graph to which this extension is applied is not a SailGraph implementation",
                    null,
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    null,
                    generateErrorJson(extMethod.getExtensionApiAsJson()));
        }

        try {

            SailGraph sailGraph = (SailGraph) graph;
            List<Map<String, Vertex>> sparqlResults = sailGraph.executeSparql(queryString);

            JSONArray jsonArray = new JSONArray();

            for (Map<String, Vertex> map : sparqlResults) {
                Map<String, JSONObject> mapOfJson = new HashMap<String, JSONObject>();
                for (String key : map.keySet()) {
                    mapOfJson.put(key, GraphSONFactory.createJSONElement(map.get(key), returnKeys, showDataTypes));
                }

                jsonArray.put(new JSONObject(mapOfJson));
            }

            HashMap<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(Tokens.SUCCESS, true);
            resultMap.put(Tokens.RESULTS, jsonArray);

            JSONObject resultObject = new JSONObject(resultMap);
            return ExtensionResponse.ok(resultObject);

        } catch (Exception mqe) {
            logger.error(mqe);
            return ExtensionResponse.error(
                    "Error executing SPARQL query [" + queryString + "]", generateErrorJson());
        }

    }
}

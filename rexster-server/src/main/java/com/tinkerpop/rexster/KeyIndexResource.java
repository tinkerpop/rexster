package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.rexster.extension.HttpMethod;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;

/**
 * Key index resource.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@Path("/graphs/{graphname}/keyindices")
public class KeyIndexResource extends AbstractSubResource {
    private static final Logger logger = Logger.getLogger(EdgeResource.class);

    public KeyIndexResource() {
        super(null);
    }

    public KeyIndexResource(final UriInfo ui, final HttpServletRequest req, final RexsterApplication ra) {
        super(ra);
        this.httpServletRequest = req;
        this.uriInfo = ui;
    }

    @OPTIONS
    public Response optionsKeyIndices() {
        return buildOptionsResponse(HttpMethod.GET.toString());
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    public Response getKeyIndices(@PathParam("graphname") final String graphName) {
        final KeyIndexableGraph graph = this.getKeyIndexableGraph(graphName);
        
        try {
            final JSONArray keyVertexArray = new JSONArray();
            for (String key : graph.getIndexedKeys(Vertex.class)) {
                keyVertexArray.put(key);
            }

            final JSONArray keyEdgeArray = new JSONArray();
            for (String key : graph.getIndexedKeys(Edge.class)) {
                keyEdgeArray.put(key);
            }

            this.resultObject.put(Tokens.KEYS, new JSONObject(new HashMap() {{
                put(Tokens.VERTEX, keyVertexArray);
                put(Tokens.EDGE, keyEdgeArray);
            }}));
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

        } catch (JSONException ex) {
            logger.error(ex);

            final JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();

    }

    @OPTIONS
    @Path("/{clazz}")
    public Response optionsIndexKeys() {
        return buildOptionsResponse(HttpMethod.GET.toString());
    }

    @GET
    @Path("/{clazz}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    public Response getIndexKeys(@PathParam("graphname") final String graphName, @PathParam("clazz") final String clazz) {
        final Class<? extends Element> keyClass;
        if (clazz.equals(Tokens.VERTEX)) {
            keyClass = Vertex.class;
        } else if (clazz.equals(Tokens.EDGE)) {
            keyClass = Edge.class;
        } else {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }
        
        final KeyIndexableGraph graph = this.getKeyIndexableGraph(graphName);

        try {
            final JSONArray keyArray = new JSONArray();
            for (String key : graph.getIndexedKeys(keyClass)) {
                keyArray.put(key);
            }

            this.resultObject.put(Tokens.RESULTS, keyArray);
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

        } catch (JSONException ex) {
            logger.error(ex);

            final JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();

    }
    
    @OPTIONS
    @Path("/{clazz}/{keyName}")
    public Response optionsIndexKey() {
        return buildOptionsResponse(HttpMethod.DELETE.toString(),
                HttpMethod.POST.toString());
    }

    @DELETE
    @Path("{clazz}/{keyName}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    public Response deleteIndexKey(@PathParam("graphname") final String graphName, @PathParam("clazz") final String clazz, 
                                   @PathParam("keyName") final String keyName) {
        final Class keyClass;
        if (clazz.equals(Tokens.VERTEX)) {
            keyClass = Vertex.class;
        } else if (clazz.equals(Tokens.EDGE)) {
            keyClass = Edge.class;
        } else {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        if (keyName == null || keyName.isEmpty()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
        final KeyIndexableGraph graph = this.getKeyIndexableGraph(graphName);
        
        try {
            graph.dropKeyIndex(keyName, keyClass);

            rag.tryStopTransactionSuccess();

            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());
        } catch (JSONException ex) {
            logger.error(ex);

            rag.tryStopTransactionFailure();

            final JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();

    }

    @POST
    @Path("/{clazz}/{keyName}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    public Response postIndexKey(@PathParam("graphname") final String graphName, @PathParam("clazz") final String clazz,
                                 @PathParam("keyName") final String keyName) {

        final Class keyClass;
        if (clazz.equals(Tokens.VERTEX)) {
            keyClass = Vertex.class;
        } else if (clazz.equals(Tokens.EDGE)) {
            keyClass = Edge.class;
        } else {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        if (keyName == null || keyName.isEmpty()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }

        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
        final KeyIndexableGraph graph = this.getKeyIndexableGraph(graphName);

        try {
            graph.createKeyIndex(keyName, keyClass);

            rag.tryStopTransactionSuccess();

            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());
        } catch (JSONException ex) {
            logger.error(ex);

            rag.tryStopTransactionFailure();

            final JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    private KeyIndexableGraph getKeyIndexableGraph(final String graphName) {

        final Graph graph = this.getRexsterApplicationGraph(graphName).getUnwrappedGraph();
        final KeyIndexableGraph idxGraph = graph instanceof KeyIndexableGraph ? (KeyIndexableGraph) graph : null;

        if (idxGraph == null) {
            final JSONObject error = this.generateErrorObject("The requested graph is not of type IndexableGraph.");
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return idxGraph;
    }

}

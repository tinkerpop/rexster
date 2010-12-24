package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.rexster.traversals.ElementJSONObject;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.util.Iterator;
import java.util.List;

@Path("/{graphname}/vertices")
@Produces(MediaType.APPLICATION_JSON)
public class VertexResource extends AbstractSubResource {

    private static Logger logger = Logger.getLogger(VertexResource.class);

    public VertexResource(@PathParam("graphname") String graphName, @Context UriInfo ui, @Context HttpServletRequest req) {
        super(graphName, ui, req, null);
    }
    
    public VertexResource(String graphName, UriInfo ui, HttpServletRequest req, RexsterApplicationProvider rap) {
        super(graphName, ui, req, rap);
    }
    
    /**
     * GET http://host/graph/vertices
     * graph.getVertices();
     */
    @GET
    public Response getVertices() {
        Long start = this.getStartOffset();
        Long end = this.getEndOffset();

        try {
            long counter = 0l;
            final JSONArray vertexArray = new JSONArray();

            for (Vertex vertex : this.rag.getGraph().getVertices()) {
                if (counter >= start && counter < end) {
                    vertexArray.put(new ElementJSONObject(vertex, this.getReturnKeys(), this.hasShowTypes()));
                }
                counter++;
            }

            this.resultObject.put(Tokens.RESULTS, vertexArray);
            this.resultObject.put(Tokens.TOTAL_SIZE, counter);
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

        } catch (JSONException ex) {
            logger.error(ex);
            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    /**
     * GET http://host/graph/vertices/id
     * graph.getVertex(id);
     */
    @GET
    @Path("/{id}")
    public Response getSingleVertex(@PathParam("id") String id) {
        Vertex vertex = this.rag.getGraph().getVertex(id);
        if (null != vertex) {
            try {
                this.resultObject.put(Tokens.RESULTS, new ElementJSONObject(vertex, this.getReturnKeys(), this.hasShowTypes()));
                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());
            } catch (JSONException ex) {
                logger.error(ex);

                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
        } else {
            String msg = "Could not find vertex [" + id + "] on graph [" + this.rag.getGraphName() + "]";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
        }


        return Response.ok(this.resultObject).build();
    }

    /**
     * GET http://host/graph/vertices/id/direction
     * graph.getVertex(id).get{Direction}Edges();
     */
    @GET
    @Path("/{id}/{direction}")
    public Response getVertexEdges(@PathParam("id") String vertexId, @PathParam("direction") String direction) {
        try {
            Long start = this.getStartOffset();
            Long end = this.getEndOffset();

            long counter = 0l;
            Vertex vertex = this.rag.getGraph().getVertex(vertexId);
            JSONArray edgeArray = new JSONArray();

            if (null != vertex) {
                JSONObject tempRequest = this.getNonRexsterRequest();
                if (direction.equals(Tokens.OUT_E) || direction.equals(Tokens.BOTH_E)) {
                    for (Edge edge : vertex.getOutEdges()) {
                        if (this.hasPropertyValues(edge, tempRequest)) {
                            if (counter >= start && counter < end) {
                                edgeArray.put(new ElementJSONObject(edge, this.getReturnKeys(), this.hasShowTypes()));
                            }
                            counter++;
                        }
                    }
                }
                if (direction.equals(Tokens.IN_E) || direction.equals(Tokens.BOTH_E)) {
                    for (Edge edge : vertex.getInEdges()) {
                        if (this.hasPropertyValues(edge, tempRequest)) {
                            if (counter >= start && counter < end) {
                                edgeArray.put(new ElementJSONObject(edge, this.getReturnKeys(), this.hasShowTypes()));
                            }
                            counter++;
                        }
                    }
                }
            } else {
                String msg = "Could not find vertex [" + vertexId + "] on graph [" + this.rag.getGraphName() + "]";
                logger.info(msg);

                JSONObject error = generateErrorObject(msg);
                throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
            }

            this.resultObject.put(Tokens.RESULTS, edgeArray);
            this.resultObject.put(Tokens.TOTAL_SIZE, counter);
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

        } catch (JSONException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    /**
     * POST http://host/graph/vertices
     * graph.addVertex(null);
     */
    @POST
    public Response postNullVertex() {
        return this.postVertex(null);
    }

    /**
     * POST http://host/graph/vertices/id?key=value
     * Vertex v = graph.addVertex(id);
     * v.setProperty(key,value);
     */
    @POST
    @Path("/{id}")
    public Response postVertex(@PathParam("id") String id) {

        try {
            Graph graph = this.rag.getGraph();
            Vertex vertex = graph.getVertex(id);
            if (null == vertex) {
                vertex = graph.addVertex(id);
            } else {
                if (!this.hasElementProperties(this.requestObject)) {
                    JSONObject error = generateErrorObjectJsonFail(new Exception("Vertex with id " + id + " already exists"));
                    throw new WebApplicationException(Response.status(Status.CONFLICT).entity(error).build());
                }
            }

            Iterator keys = this.requestObject.keys();
            while (keys.hasNext()) {
                String key = keys.next().toString();
                if (!key.startsWith(Tokens.UNDERSCORE)) {
                    vertex.setProperty(key, this.getTypedPropertyValue(this.requestObject.getString(key)));
                }
            }

            this.resultObject.put(Tokens.RESULTS, new ElementJSONObject(vertex, this.getReturnKeys(), this.hasShowTypes()));
            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    /**
     * DELETE http://host/graph/vertices/id
     * graph.removeVertex(graph.getVertex(id));
     * <p/>
     * DELETE http://host/graph/vertices/id?key1&key2
     * Vertex v = graph.getVertex(id);
     * v.removeProperty(key1);
     * v.removeProperty(key2);
     */
    @DELETE
    @Path("/{id}")
    public Response deleteVertex(@PathParam("id") String id) {
        try {
            final List<String> keys = this.getNonRexsterRequestKeys();
            final Graph graph = this.rag.getGraph();
            final Vertex vertex = graph.getVertex(id);
            if (null != vertex) {
                if (keys.size() > 0) {
                    // delete vertex properites
                    for (final String key : keys) {
                        vertex.removeProperty(key);
                    }
                } else {
                    // delete vertex
                    graph.removeVertex(vertex);
                }
            } else {
                final String msg = "Could not find vertex [" + id + "] on graph [" + this.rag.getGraphName() + "]";
                logger.info(msg);

                JSONObject error = generateErrorObject(msg);
                throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
            }
            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();

    }

    /* TODO: WITHOUT CONCURRENT MODIFICATION ERRORS
    @DELETE
    public Response deleteAllVertices() {
        final Graph graph = this.rag.getGraph();
        Iterator<Vertex> itty = graph.getVertices().iterator();
        while(itty.hasNext()) {
            graph.removeVertex(itty.next());
        }

        try {
            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();

    }*/
}

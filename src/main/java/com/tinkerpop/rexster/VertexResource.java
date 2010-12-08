package com.tinkerpop.rexster;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Index;
import com.tinkerpop.blueprints.pgm.IndexableGraph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.rexster.traversals.ElementJSONObject;

@Path("/{graphname}/vertices")
@Produces(MediaType.APPLICATION_JSON)
public class VertexResource extends AbstractSubResource {
	
	private static Logger logger = Logger.getLogger(VertexResource.class);

    public VertexResource(@PathParam("graphname") String graphName, @Context UriInfo ui, @Context HttpServletRequest req) {
    	super(graphName, ui, req);
    }
    
    @GET
    @Path("/{id}/{direction}")
    public Response getVertexEdges(@PathParam("id") String vertexId, @PathParam("direction") String direction) {
        try {
            Long start = this.getStartOffset();
            if (null == start)
                start = 0l;
            Long end = this.getEndOffset();
            if (null == end)
                end = Long.MAX_VALUE;

            long counter = 0l;
            Vertex vertex = this.rag.getGraph().getVertex(vertexId);
            JSONArray edgeArray = new JSONArray();

            if (null != vertex) {
                JSONObject tempRequest = this.getNonRexsterRequest();
                if (direction.equals(Tokens.OUT_E) || direction.equals(Tokens.BOTH_E)) {
                    for (Edge edge : vertex.getOutEdges()) {
                        if (this.hasPropertyValues(edge, tempRequest)) {
                            if (counter >= start && counter < end) {
                                edgeArray.put(new ElementJSONObject(
                                		edge, this.getReturnKeys(), this.hasShowTypes()));
                            }
                            counter++;
                        }
                    }
                }
                if (direction.equals(Tokens.IN_E) || direction.equals(Tokens.BOTH_E)) {
                    for (Edge edge : vertex.getInEdges()) {
                        if (this.hasPropertyValues(edge, tempRequest)) {
                            if (counter >= start && counter < end) {
                                edgeArray.put(new ElementJSONObject(
                                		edge, this.getReturnKeys(), this.hasShowTypes()));
                            }
                            counter++;
                        }
                    }
                }
            } else {
                String msg = "Could not find vertex [" + vertexId + "] on graph [" + this.rag.getGraphName() + "].";
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

    @GET
    @Path("/{id}")
    public Response getSingleVertex(@PathParam("id") String id) {
        Vertex vertex = this.rag.getGraph().getVertex(id);
        if (null != vertex) {
            try {
                this.resultObject.put(Tokens.RESULTS, new ElementJSONObject(
                		vertex, this.getReturnKeys(), this.hasShowTypes()));
                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());
            } catch (JSONException ex) {
                logger.error(ex);

                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
        } else {
            String msg = "Could not find vertex [" + id + "] on graph [" + this.rag.getGraphName() + "].";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
        }


        return Response.ok(this.resultObject).build();
    }


    @GET
    public Response getVertices() {
        Long start = this.getStartOffset();
        if (null == start)
            start = 0l;
        Long end = this.getEndOffset();
        if (null == end)
            end = Long.MAX_VALUE;

        try {
            long counter = 0l;
            JSONArray vertexArray = new JSONArray();
            String key = null;
            Iterator keys = this.getNonRexsterRequest().keys();
            while (keys.hasNext()) {
                key = keys.next().toString();
                break;
            }
            Iterable<? extends Element> itty;
            if (null != key) {
                itty = ((IndexableGraph) this.rag.getGraph()).getIndex(Index.VERTICES, Vertex.class).get(key, this.requestObject.get(key));
            } else {
                itty = this.rag.getGraph().getVertices();
            }

            if (null != itty) {
                for (Element element : itty) {
                    if (counter >= start && counter < end) {
                        vertexArray.put(new ElementJSONObject(
                        		element, this.getReturnKeys(), this.hasShowTypes()));
                    }
                    counter++;
                }
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
    
    @POST
    public Response postNull(){
    	return this.postVertex(null);
    }

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

            this.resultObject.put(Tokens.RESULTS, new ElementJSONObject(
            		vertex, this.getReturnKeys(), this.hasShowTypes()));
            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteVertex(@PathParam("id") String id) {
        // TODO: delete individual properties

        final Graph graph = this.rag.getGraph();
        final Vertex vertex = graph.getVertex(id);
        if (null != vertex) {
            graph.removeVertex(vertex);
        } else {
            String msg = "Could not find vertex [" + id + "] on graph [" + this.rag.getGraphName() + "] for deletion.";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
        }

        try {
            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();

    }
}

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
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.rexster.traversals.ElementJSONObject;

@Path("/{graphname}/edges")
@Produces(MediaType.APPLICATION_JSON)
public class EdgeResource extends AbstractSubResource {
	
	private static Logger logger = Logger.getLogger(EdgeResource.class);

    public EdgeResource(@PathParam("graphname") String graphName, @Context UriInfo ui, @Context HttpServletRequest req) {
    	super(graphName, ui, req);
    }
    
    @GET
    public Response getAllEdges() {

        Long start = this.getStartOffset();
        if (null == start)
            start = 0l;
        Long end = this.getEndOffset();
        if (null == end)
            end = Long.MAX_VALUE;

        long counter = 0l;

        try {
            JSONArray edgeArray = new JSONArray();
            for (Edge edge : this.rag.getGraph().getEdges()) {
                if (counter >= start && counter < end) {
                    edgeArray.put(new ElementJSONObject(edge, this.getReturnKeys()));
                }
                counter++;
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
    public Response getSingleEdge(@PathParam("id") String id) {
        final Edge edge = this.rag.getGraph().getEdge(id);

        if (null != edge) {
            try {
                this.resultObject.put(Tokens.RESULTS, new ElementJSONObject(edge, this.getReturnKeys()));
                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());
            } catch (JSONException ex) {
                logger.error(ex);

                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
        } else {
            String msg = "Could not find edge [" + id + "] on graph [" + this.rag.getGraphName() + "]";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }
    
    @POST
    public Response postNull(){
    	return this.postEdge(null);
    }

    @POST
    @Path("/{id}")
    public Response postEdge(@PathParam("id") String id) {

        final Graph graph = this.rag.getGraph();
        String inV = null;
        Object temp = this.requestObject.opt(Tokens._IN_V);
        if (null != temp)
            inV = temp.toString();
        String outV = null;
        temp = this.requestObject.opt(Tokens._OUT_V);
        if (null != temp)
            outV = temp.toString();
        String label = null;
        temp = this.requestObject.opt(Tokens._LABEL);
        if (null != temp)
            label = temp.toString();

        Edge edge = graph.getEdge(id);
        if (null == edge && null != outV && null != inV && null != label) {
            final Vertex out = graph.getVertex(outV);
            final Vertex in = graph.getVertex(inV);
            if (null != out && null != in)
                edge = graph.addEdge(id, out, in, label);
        } else if (edge != null) {
            if (!this.hasElementProperties(this.requestObject)) {
                JSONObject error = generateErrorObjectJsonFail(new Exception("Edge with id " + id + " already exists"));
                throw new WebApplicationException(Response.status(Status.CONFLICT).entity(error).build());
            }
        }

        try {
            if (edge != null) {
                Iterator keys = this.requestObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next().toString();
                    if (!key.startsWith(Tokens.UNDERSCORE))
                        edge.setProperty(key, this.requestObject.get(key));
                }
                this.resultObject.put(Tokens.RESULTS, new ElementJSONObject(edge, this.getReturnKeys()));
            }

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
    public Response deleteEdge(@PathParam("id") String id) {
        // TODO: delete individual properties

        final Graph graph = this.rag.getGraph();
        final Edge edge = graph.getEdge(id);
        if (null != edge) {
            graph.removeEdge(edge);
        } else {
            String msg = "Could not find edge [" + id + "] on graph [" + this.rag.getGraphName() + "] for deletion.";
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

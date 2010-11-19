package com.tinkerpop.rexster;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.rexster.traversals.Traversal;

@Path("/{graphname}")
@Produces(MediaType.APPLICATION_JSON)
public class GraphResource extends BaseResource {

    private static Logger logger = Logger.getLogger(GraphResource.class);

    public GraphResource(@PathParam("graphname") String graphName, @Context UriInfo ui, @Context HttpServletRequest req) {
        super();

        this.rag = WebServer.GetRexsterApplication().getApplicationGraph(graphName);
        if (this.rag == null) {

            logger.info("Request for a non-configured graph [" + graphName + "]");

            JSONObject error = generateErrorObject("Graph [" + graphName + "] could not be found.");
            throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
        }

        try {
            this.resultObject.put(Tokens.VERSION, RexsterApplication.getVersion());
            Map<String, String> queryParameters = req.getParameterMap();
            this.buildRequestObject(queryParameters);

            this.request = req;
            this.uriInfo = ui;
        } catch (JSONException ex) {

            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }
    }

    @GET
    public Response getGraph() {

        try {

            // graph should be ready to go at this point.  checks in the
            // constructor ensure that the rag is not null.
            Graph graph = this.rag.getGraph();

            this.resultObject.put("name", rag.getGraphName());
            this.resultObject.put("graph", graph.toString());

            JSONArray queriesArray = new JSONArray();
            for (Map.Entry<String, Class<? extends Traversal>> traversal : this.rag.getLoadedTraversals().entrySet()) {
                queriesArray.put(traversal.getKey());
            }

            this.resultObject.put("traversals", queriesArray);

            this.resultObject.put("query_time", this.sh.stopWatch());
            this.resultObject.put("up_time", this.getTimeAlive());
            this.resultObject.put("version", RexsterApplication.getVersion());

        } catch (JSONException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }
    
}

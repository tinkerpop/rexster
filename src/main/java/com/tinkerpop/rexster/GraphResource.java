package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

@Path("/{graphname}")
@Produces(MediaType.APPLICATION_JSON)
public class GraphResource extends AbstractSubResource {

    private static Logger logger = Logger.getLogger(GraphResource.class);

    public GraphResource() {
        super(null);
    }

    public GraphResource(UriInfo ui, HttpServletRequest req, RexsterApplicationProvider rap) {
        super(rap);
        this.httpServletRequest = req;
        this.uriInfo = ui;
    }

    /**
     * GET http://host/graph
     * graph.toString();
     */
    @GET
    public Response getGraph(@PathParam("graphname") String graphName) {
        try {

            // graph should be ready to go at this point.  checks in the
            // constructor ensure that the rag is not null.
            Graph graph = this.getRexsterApplicationGraph(graphName).getGraph();

            this.resultObject.put("name", graphName);
            this.resultObject.put("graph", graph.toString());
            this.resultObject.put("type", graph.getClass().getName());
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());
            this.resultObject.put("up_time", this.getTimeAlive());
            this.resultObject.put("version", RexsterApplication.getVersion());

        } catch (JSONException ex) {
            logger.error(ex);
            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
        }

        return this.addHeaders(Response.ok(this.resultObject)).build();
    }

    /**
     * DELETE http://host/graph
     * graph.clear()
     *
     * @return Query time
     */
    @DELETE
    public Response deleteGraph(@PathParam("graphname") String graphName) {
        Graph graph = this.getRexsterApplicationGraph(graphName).getGraph();
        graph.clear();

        try {
            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException ex) {
            logger.error(ex);
            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
        }

        return this.addHeaders(Response.ok(this.resultObject)).build();

    }


}

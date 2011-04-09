package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.readonly.ReadOnlyGraph;

import com.tinkerpop.rexster.extension.*;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.lang.reflect.Method;

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
            
            boolean isReadOnly = false;
            String graphType = graph.getClass().getName();
            if (graph instanceof ReadOnlyGraph) {
            	// readonly graphs must unwrap to the underlying graph implementation
            	graphType = ((ReadOnlyGraph) graph).getRawGraph().getClass().getName();
            	isReadOnly = true;
            }
            
            this.resultObject.put("read_only", isReadOnly);
            this.resultObject.put("type", graphType);
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

    @GET
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!traversals).+}")
    public Response getGraphExtension(@PathParam("graphname") String graphName) {

        ExtensionResponse extResponse = null;
        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(this.uriInfo);

        if (!extensionSegmentSet.isValidFormat()) {
            logger.error("Tried to parse the extension segments but they appear invalid: " + extensionSegmentSet);
            JSONObject error = generateErrorObject(
                    "The [" + extensionSegmentSet + "] extension appears invalid for [" + graphName + "]");
            throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
        }

        // determine if the namespace and extension are enabled for this graph
        RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);

        if (rag.isExtensionAllowed(extensionSegmentSet)) {

            // namespace was allowed so try to run the extension
            try {

                // look for the extension as loaded through serviceloader
                RexsterExtension rexsterExtension = findExtension(extensionSegmentSet);

                if (rexsterExtension == null) {
                    // extension was not found for some reason

                    logger.error("The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "].  Check com.tinkerpop.rexster.extension.RexsterExtension file in META-INF.services.");
                    JSONObject error = generateErrorObject(
                            "The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "]");
                    throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
                }

                // look up the method on the extension that needs to be called.
                Method methodToCall = findExtensionMethod(rexsterExtension, ExtensionPoint.GRAPH, extensionSegmentSet.getExtensionMethod());

                if (methodToCall == null) {
                    // extension method was not found for some reason
                    logger.error("The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "].  Check com.tinkerpop.rexster.extension.RexsterExtension file in META-INF.services.");
                    JSONObject error = generateErrorObject(
                            "The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "]");
                    throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
                }

                // found the method...time to do work
                Object returnValue = invokeExtension(graphName, rexsterExtension, methodToCall);
                if (returnValue instanceof ExtensionResponse) {
                    extResponse = (ExtensionResponse) returnValue;

                    if (extResponse.isErrorResponse()) {
                        // an error was raised within the extension.  pass it back out as an error.
                        logger.error("The [" + extensionSegmentSet + "] extension raised an error response.");
                        throw new WebApplicationException(this.addHeaders(Response.fromResponse(extResponse.getJerseyResponse())).build());
                    }
                } else {
                    // extension method was not found for some reason
                    logger.error("The [" + extensionSegmentSet + "] extension does not return an ExtensionResponse.");
                    JSONObject error = generateErrorObject(
                            "The [" + extensionSegmentSet + "] extension does not return an ExtensionResponse.");
                    throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
                }

            } catch (Exception ex) {
                logger.error(ex);
                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
            }
        } else {
            // namespace was not allowed
            logger.error("The [" + extensionSegmentSet + "] extension was not configured for [" + graphName + "]");
            JSONObject error = generateErrorObject(
                    "The [" + extensionSegmentSet + "] extension was not configured for [" + graphName + "]");
            throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
        }

        return this.addHeaders(Response.fromResponse(extResponse.getJerseyResponse())).build();
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

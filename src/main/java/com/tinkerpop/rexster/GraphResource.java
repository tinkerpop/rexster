package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.readonly.ReadOnlyGraph;
import com.tinkerpop.rexster.extension.*;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

@Path("/{graphname}")
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGraph(@PathParam("graphname") String graphName) {
        Graph graph = this.getRexsterApplicationGraph(graphName).getGraph();

        try {

            this.resultObject.put("name", graphName);
            this.resultObject.put("graph", graph.toString());

            boolean isReadOnly = false;
            String graphType = graph.getClass().getName();
            if (graph instanceof ReadOnlyGraph) {
                // readonly graphs must unwrap to the underlying graph implementation
                graphType = ((ReadOnlyGraph) graph).getRawGraph().getClass().getName();
                isReadOnly = true;
            }

            this.resultObject.put(Tokens.READ_ONLY, isReadOnly);
            this.resultObject.put("type", graphType);
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());
            this.resultObject.put(Tokens.UP_TIME, this.getTimeAlive());
            this.resultObject.put("version", RexsterApplication.getVersion());

            JSONArray extensionsList = getExtensionHypermedia(graphName, ExtensionPoint.GRAPH);
            if (extensionsList != null) {
                this.resultObject.put(Tokens.EXTENSIONS, extensionsList);
            }

        } catch (JSONException ex) {
            logger.error(ex);
            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    @POST
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!prefixes).+}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getGraphExtension(@PathParam("graphname") String graphName, MultivaluedMap<String, String> formParams) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.buildRequestObject(formParams);
        return this.getGraphExtension(graphName);
    }

    @POST
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!prefixes).+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getGraphExtension(@PathParam("graphname") String graphName, JSONObject json) {
        this.setRequestObject(json);
        return this.getGraphExtension(graphName);
    }

    @GET
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!prefixes).+}")
    public Response getGraphExtension(@PathParam("graphname") String graphName) {

        ExtensionResponse extResponse;
        ExtensionMethod methodToCall;
        ExtensionSegmentSet extensionSegmentSet = parseUriForExtensionSegment(graphName, ExtensionPoint.GRAPH);

        RexsterApplicationGraph rag = null;
        try {
            rag = this.getRexsterApplicationGraph(graphName);
        } catch (WebApplicationException wae) {
            // kinda stinks.  checking for a NOT FOUND which means that the graph does not exist
            // which means we should try to find an extension worthwhile.  If it is something
            // other than not-found then we need to bubble it up.
            if (wae.getResponse().getStatus() != Status.NOT_FOUND.getStatusCode()) {
                throw wae;
            }

            rag = null;
        }

        if ((rag != null && rag.isExtensionAllowed(extensionSegmentSet))
                && (rag == null && isBaseExtensionAllowed(extensionSegmentSet))) {

            Object returnValue = null;

            // namespace was allowed so try to run the extension
            try {

                // look for the extension as loaded through serviceloader
                RexsterExtension rexsterExtension = findExtension(extensionSegmentSet);

                if (rexsterExtension == null) {
                    // extension was not found for some reason
                    logger.error("The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "].  Check com.tinkerpop.rexster.extension.RexsterExtension file in META-INF.services.");
                    JSONObject error = generateErrorObject(
                            "The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "]");
                    throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
                }

                ExtensionPoint extensionPointCalledInRequest = ExtensionPoint.GRAPH;
                if (rag == null) {
                    extensionPointCalledInRequest = ExtensionPoint.BASE;
                }

                methodToCall = findExtensionMethod(rexsterExtension, extensionPointCalledInRequest, extensionSegmentSet.getExtensionMethod());

                if (methodToCall == null) {
                    // extension method was not found for some reason
                    logger.error("The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "].  Check com.tinkerpop.rexster.extension.RexsterExtension file in META-INF.services.");
                    JSONObject error = generateErrorObject(
                            "The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "]");
                    throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
                }

                // found the method...time to do work
                returnValue = invokeExtension(graphName, rexsterExtension, methodToCall);

            } catch (WebApplicationException wae) {
                // already logged this...just throw it  up.
                throw wae;
            } catch (Exception ex) {
                logger.error("Dynamic invocation of the [" + extensionSegmentSet + "] extension failed.", ex);
                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }

            if (returnValue instanceof ExtensionResponse) {
                extResponse = (ExtensionResponse) returnValue;

                if (extResponse.isErrorResponse()) {
                    // an error was raised within the extension.  pass it back out as an error.
                    logger.warn("The [" + extensionSegmentSet + "] extension raised an error response.");
                    throw new WebApplicationException(Response.fromResponse(extResponse.getJerseyResponse()).build());
                }
            } else {
                // extension method is not returning the correct type...needs to be an ExtensionResponse
                logger.error("The [" + extensionSegmentSet + "] extension does not return an ExtensionResponse.");
                JSONObject error = generateErrorObject(
                        "The [" + extensionSegmentSet + "] extension does not return an ExtensionResponse.");
                throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }

        } else {
            // namespace was not allowed
            logger.error("The [" + extensionSegmentSet + "] extension was not configured for [" + graphName + "]");
            JSONObject error = generateErrorObject(
                    "The [" + extensionSegmentSet + "] extension was not configured for [" + graphName + "]");
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        String mediaType = MediaType.APPLICATION_JSON;
        if (methodToCall != null) {
            mediaType = methodToCall.getExtensionDefinition().produces();
            extResponse = tryAppendRexsterAttributesIfJson(extResponse, methodToCall, mediaType);
        }

        return Response.fromResponse(extResponse.getJerseyResponse()).type(mediaType).build();
    }

    /**
     * DELETE http://host/graph
     * graph.clear()
     *
     * @return Query time
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGraph(@PathParam("graphname") String graphName) {
        Graph graph = this.getRexsterApplicationGraph(graphName).getGraph();
        graph.clear();

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

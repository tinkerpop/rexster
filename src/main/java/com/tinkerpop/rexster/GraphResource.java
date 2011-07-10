package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.readonly.ReadOnlyGraph;
import com.tinkerpop.rexster.extension.*;
import com.tinkerpop.rexster.extension.HttpMethod;
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
import java.util.List;
import java.util.ServiceConfigurationError;

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

    @HEAD
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!prefixes).+}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response headGraphExtension(@PathParam("graphname") String graphName, MultivaluedMap<String, String> formParams) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.buildRequestObject(formParams);
        return this.executeGraphExtension(graphName, HttpMethod.HEAD);
    }

    @HEAD
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!prefixes).+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response headGraphExtension(@PathParam("graphname") String graphName, JSONObject json) {
        this.setRequestObject(json);
        return this.executeGraphExtension(graphName, HttpMethod.HEAD);
    }

    @HEAD
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!prefixes).+}")
    public Response headGraphExtension(@PathParam("graphname") String graphName) {
        return this.executeGraphExtension(graphName, HttpMethod.HEAD);
    }

    @PUT
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!prefixes).+}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response putGraphExtension(@PathParam("graphname") String graphName, MultivaluedMap<String, String> formParams) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.buildRequestObject(formParams);
        return this.executeGraphExtension(graphName, HttpMethod.PUT);
    }

    @PUT
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!prefixes).+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putGraphExtension(@PathParam("graphname") String graphName, JSONObject json) {
        this.setRequestObject(json);
        return this.executeGraphExtension(graphName, HttpMethod.PUT);
    }

    @PUT
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!prefixes).+}")
    public Response putGraphExtension(@PathParam("graphname") String graphName) {
        return this.executeGraphExtension(graphName, HttpMethod.PUT);
    }

    @OPTIONS
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!prefixes).+}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response optionsGraphExtension(@PathParam("graphname") String graphName, MultivaluedMap<String, String> formParams) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.buildRequestObject(formParams);
        return this.executeGraphExtension(graphName, HttpMethod.OPTIONS);
    }

    @OPTIONS
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!prefixes).+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response optionsGraphExtension(@PathParam("graphname") String graphName, JSONObject json) {
        this.setRequestObject(json);
        return this.executeGraphExtension(graphName, HttpMethod.OPTIONS);
    }

    @OPTIONS
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!prefixes).+}")
    public Response optionsGraphExtension(@PathParam("graphname") String graphName) {
        return this.executeGraphExtension(graphName, HttpMethod.OPTIONS);
    }

    @DELETE
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!prefixes).+}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response deleteGraphExtension(@PathParam("graphname") String graphName, MultivaluedMap<String, String> formParams) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.buildRequestObject(formParams);
        return this.executeGraphExtension(graphName, HttpMethod.DELETE);
    }

    @DELETE
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!prefixes).+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteGraphExtension(@PathParam("graphname") String graphName, JSONObject json) {
        this.setRequestObject(json);
        return this.executeGraphExtension(graphName, HttpMethod.DELETE);
    }

    @DELETE
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!prefixes).+}")
    public Response deleteGraphExtension(@PathParam("graphname") String graphName) {
        return this.executeGraphExtension(graphName, HttpMethod.DELETE);
    }

    @POST
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!prefixes).+}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postGraphExtension(@PathParam("graphname") String graphName, MultivaluedMap<String, String> formParams) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.buildRequestObject(formParams);
        return this.executeGraphExtension(graphName, HttpMethod.POST);
    }

    @POST
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!prefixes).+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postGraphExtension(@PathParam("graphname") String graphName, JSONObject json) {
        this.setRequestObject(json);
        return this.executeGraphExtension(graphName, HttpMethod.POST);
    }

    @POST
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!prefixes).+}")
    public Response postGraphExtension(@PathParam("graphname") String graphName) {
        return this.executeGraphExtension(graphName, HttpMethod.POST);
    }

    @GET
    @Path("{extension: (?!vertices)(?!edges)(?!indices)(?!prefixes).+}")
    public Response getGraphExtension(@PathParam("graphname") String graphName) {
        return this.executeGraphExtension(graphName, HttpMethod.GET);
    }

    private Response executeGraphExtension(String graphName, HttpMethod httpMethodRequested) {

        ExtensionResponse extResponse;
        ExtensionMethod methodToCall;
        ExtensionSegmentSet extensionSegmentSet = parseUriForExtensionSegment(graphName, ExtensionPoint.GRAPH);

        // determine if the namespace and extension are enabled for this graph
        RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);

        if (rag.isExtensionAllowed(extensionSegmentSet)) {

            Object returnValue = null;

            // namespace was allowed so try to run the extension
            try {

                // look for the extension as loaded through serviceloader
                List<RexsterExtension> rexsterExtensions = null;
                try {
                    rexsterExtensions = findExtensionClasses(extensionSegmentSet);
                } catch (ServiceConfigurationError sce) {
                    logger.error("ServiceLoader could not find a class referenced in com.tinkerpop.rexster.extension.RexsterExtension.");
                    JSONObject error = generateErrorObject(
                            "Class specified in com.tinkerpop.rexster.extension.RexsterExtension could not be found.",
                            sce);
                    throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
                }

                if (rexsterExtensions == null && rexsterExtensions.size() == 0) {
                    // extension was not found for some reason
                    logger.error("The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "].  Check com.tinkerpop.rexster.extension.RexsterExtension file in META-INF.services.");
                    JSONObject error = generateErrorObject(
                            "The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "]");
                    throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
                }

                // look up the method on the extension that needs to be called.
                methodToCall = findExtensionMethod(rexsterExtensions, ExtensionPoint.GRAPH, extensionSegmentSet.getExtensionMethod(), httpMethodRequested);

                if (methodToCall == null) {
                    // extension method was not found for some reason
                    logger.error("The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "] with a HTTP method of [" + httpMethodRequested.name() + "].  Check com.tinkerpop.rexster.extension.RexsterExtension file in META-INF.services.");
                    JSONObject error = generateErrorObject(
                            "The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "] with a HTTP method of [" + httpMethodRequested.name() + "]");
                    throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
                }

                // found the method...time to do work
                returnValue = invokeExtension(graphName, methodToCall);

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

        try {
            graph.clear();
        } catch (Exception ex) {
            logger.error(ex);
            JSONObject error = generateErrorObject(ex.getMessage());
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
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
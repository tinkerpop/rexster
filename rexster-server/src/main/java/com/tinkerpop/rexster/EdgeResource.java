package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONUtility;
import com.tinkerpop.rexster.extension.ExtensionMethod;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.ExtensionSegmentSet;
import com.tinkerpop.rexster.extension.HttpMethod;
import com.tinkerpop.rexster.extension.RexsterExtension;
import com.tinkerpop.rexster.server.RexsterApplication;
import com.tinkerpop.rexster.util.ElementHelper;
import com.tinkerpop.rexster.util.RequestObjectHelper;
import com.codahale.metrics.annotation.Timed;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.Set;

/**
 * Resource for edges.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@Path("/graphs/{graphname}/edges")
public class EdgeResource extends AbstractSubResource {

    private static Logger logger = Logger.getLogger(EdgeResource.class);

    public EdgeResource() {
        super(null);
    }

    public EdgeResource(final UriInfo ui, final HttpServletRequest req, final RexsterApplication ra) {
        super(ra);
        this.httpServletRequest = req;
        this.uriInfo = ui;
    }

    @OPTIONS
    public Response optionsAllEdges() {
        return buildOptionsResponse(HttpMethod.GET.toString(),
                HttpMethod.POST.toString());
    }

    /**
     * GET http://host/graph/edges
     * graph.getEdges();
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON})
    @Timed(name = "http.rest.edges.collection.get", absolute = true)
    public Response getAllEdges(@PathParam("graphname") final String graphName) {
        return this.getAllEdges(graphName, false);
    }

    @GET
    @Produces({RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Timed(name = "http.rest.edges.collection.get", absolute = true)
    public Response getAllEdgesRexsterTypedJson(@PathParam("graphname") final String graphName) {
        return this.getAllEdges(graphName, true);
    }

    private Response getAllEdges(final String graphName, final boolean showTypes) {

        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
        final Graph graph = rag.getGraph();

        final JSONObject theRequestObject = this.getRequestObject();
        final Long start = RequestObjectHelper.getStartOffset(theRequestObject);
        final Long end = RequestObjectHelper.getEndOffset(theRequestObject);
        final GraphSONMode mode = showTypes ? GraphSONMode.EXTENDED : GraphSONMode.NORMAL;
        final Set<String> returnKeys = RequestObjectHelper.getReturnKeys(this.getRequestObject());

        String key = null;
        Object value = null;

        Object temp = theRequestObject.opt(Tokens.KEY);
        if (null != temp)
            key = temp.toString();

        temp = theRequestObject.opt(Tokens.VALUE);
        if (null != temp)
            value = ElementHelper.getTypedPropertyValue(temp.toString());

        final boolean filtered = key != null && value != null;
        
        boolean wasInSection = false;
        long counter = 0l;
        try {
            final JSONArray edgeArray = new JSONArray();
            final Iterable<Edge> edges = filtered ? graph.getEdges(key, value) : graph.getEdges();
            for (Edge edge : edges) {
                if (counter >= start && counter < end) {
                    wasInSection = true;
                    edgeArray.put(GraphSONUtility.jsonFromElement(edge, returnKeys, mode));
                } else if (wasInSection) {
                    break;
                }
                counter++;
            }

            this.resultObject.put(Tokens.RESULTS, edgeArray);
            this.resultObject.put(Tokens.TOTAL_SIZE, edgeArray.length());
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

        } catch (JSONException ex) {
            logger.error(ex);

            final JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } finally {
            rag.tryCommit();
        }

        return Response.ok(this.resultObject).build();

    }

    @OPTIONS
    @Path("/{id}")
    public Response optionsSingleEdge() {
        return buildOptionsResponse();
    }

    /**
     * GET http://host/graph/edges/id
     * graph.getEdge(id);
     */
    @GET
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    @Timed(name = "http.rest.edges.object.get", absolute = true)
    public Response getSingleEdge(@PathParam("graphname") final String graphName, @PathParam("id") final String id) {
        return getSingleEdge(graphName, id, false, false);
    }

    @GET
    @Path("/{id}")
    @Produces({RexsterMediaType.APPLICATION_REXSTER_JSON})
    @Timed(name = "http.rest.edges.object.get", absolute = true)
    public Response getSingleEdgeRexsterJson(@PathParam("graphname") final String graphName, @PathParam("id") final String id) {
        return getSingleEdge(graphName, id, false, true);
    }

    @GET
    @Path("/{id}")
    @Produces({RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Timed(name = "http.rest.edges.object.get", absolute = true)
    public Response getSingleEdgeRexsterTypedJson(@PathParam("graphname") final String graphName, @PathParam("id") final String id) {
        return getSingleEdge(graphName, id, true, true);
    }

    private Response getSingleEdge(final String graphName, final String id, final boolean showTypes, final boolean showHypermedia) {
        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);

        try {
            final Edge edge = rag.getGraph().getEdge(id);

            if (null != edge) {
                final JSONObject theRequestObject = this.getRequestObject();
                final GraphSONMode mode = showTypes ? GraphSONMode.EXTENDED : GraphSONMode.NORMAL;
                final Set<String> returnKeys = RequestObjectHelper.getReturnKeys(theRequestObject);

                this.resultObject.put(Tokens.RESULTS, GraphSONUtility.jsonFromElement(edge, returnKeys, mode));
                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

                if (showHypermedia) {
                    final JSONArray extensionsList = rag.getExtensionHypermedia(ExtensionPoint.EDGE, this.getUriPath());
                    if (extensionsList != null) {
                        this.resultObject.put(Tokens.EXTENSIONS, extensionsList);
                    }
                }
            } else {
                final String msg = "Edge with id [" + id + "] cannot be found.";
                logger.info(msg);

                final JSONObject error = generateErrorObject(msg);
                throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
            }
        } catch (JSONException ex) {
            logger.error(ex);

            final JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } finally {
            rag.tryCommit();
        }

        return Response.ok(this.resultObject).build();
    }

    @HEAD
    @Path("/{id}/{extension: .+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response headEdgeExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id, final JSONObject json) {
        this.setRequestObject(json);
        return this.executeEdgeExtension(graphName, id, HttpMethod.HEAD);
    }

    @HEAD
    @Path("/{id}/{extension: .+}")
    public Response headEdgeExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id) {
        return this.executeEdgeExtension(graphName, id, HttpMethod.HEAD);
    }

    @PUT
    @Path("/{id}/{extension: .+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed(name = "http.rest.edges.object.put", absolute = true)
    public Response putEdgeExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id, final JSONObject json) {
        this.setRequestObject(json);
        return this.executeEdgeExtension(graphName, id, HttpMethod.PUT);
    }

    @PUT
    @Path("/{id}/{extension: .+}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Timed(name = "http.rest.edges.object.put", absolute = true)
    public Response putEdgeExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id, final MultivaluedMap<String, String> formParams) {
        this.setRequestObject(formParams);
        return this.executeEdgeExtension(graphName, id, HttpMethod.PUT);
    }

    @PUT
    @Path("/{id}/{extension: .+}")
    @Timed(name = "http.rest.edges.object.put", absolute = true)
    public Response putEdgeExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id) {
        return this.executeEdgeExtension(graphName, id, HttpMethod.PUT);
    }

    @OPTIONS
    @Path("/{id}/{extension: .+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response optionsEdgeExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id, final JSONObject json) {
        this.setRequestObject(json);
        return this.executeEdgeExtension(graphName, id, HttpMethod.OPTIONS);
    }

    @OPTIONS
    @Path("/{id}/{extension: .+}")
    public Response optionsEdgeExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id) {
        return this.executeEdgeExtension(graphName, id, HttpMethod.OPTIONS);
    }

    @DELETE
    @Path("/{id}/{extension: .+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed(name = "http.rest.edges.object.delete", absolute = true)
    public Response deleteEdgeExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id, final JSONObject json) {
        this.setRequestObject(json);
        return this.executeEdgeExtension(graphName, id, HttpMethod.DELETE);
    }

    @DELETE
    @Path("/{id}/{extension: .+}")
    @Timed(name = "http.rest.edges.object.delete", absolute = true)
    public Response deleteEdgeExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id) {
        return this.executeEdgeExtension(graphName, id, HttpMethod.DELETE);
    }

    @POST
    @Path("/{id}/{extension: .+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed(name = "http.rest.edges.object.post", absolute = true)
    public Response postEdgeExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id, final JSONObject json) {
        this.setRequestObject(json);
        return this.executeEdgeExtension(graphName, id, HttpMethod.POST);
    }

    @POST
    @Path("/{id}/{extension: .+}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Timed(name = "http.rest.edges.object.post", absolute = true)
    public Response postEdgeExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id, final MultivaluedMap<String, String> formParams) {
        this.setRequestObject(formParams);
        return this.executeEdgeExtension(graphName, id, HttpMethod.POST);
    }

    @POST
    @Path("/{id}/{extension: .+}")
    @Timed(name = "http.rest.edges.object.post", absolute = true)
    public Response postEdgeExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id) {
        return this.executeEdgeExtension(graphName, id, HttpMethod.POST);
    }

    @GET
    @Path("/{id}/{extension: .+}")
    @Timed(name = "http.rest.edges.object.get", absolute = true)
    public Response getEdgeExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id) {
        return this.executeEdgeExtension(graphName, id, HttpMethod.GET);
    }

    private Response executeEdgeExtension(final String graphName, final String id, final HttpMethod httpMethodRequested) {

        final Edge edge = this.getRexsterApplicationGraph(graphName).getGraph().getEdge(id);

        ExtensionResponse extResponse;
        ExtensionMethod methodToCall;
        final ExtensionSegmentSet extensionSegmentSet = parseUriForExtensionSegment(graphName, ExtensionPoint.EDGE);

        // determine if the namespace and extension are enabled for this graph
        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);

        if (rag.isExtensionAllowed(extensionSegmentSet)) {

            final Object returnValue;

            // namespace was allowed so try to run the extension
            try {

                // look for the extension as loaded through serviceloader
                final List<RexsterExtension> rexsterExtensions;
                try {
                    rexsterExtensions = findExtensionClasses(extensionSegmentSet);
                } catch (ServiceConfigurationError sce) {
                    logger.error("ServiceLoader could not find a class referenced in com.tinkerpop.rexster.extension.RexsterExtension.");
                    final JSONObject error = generateErrorObject(
                            "Class specified in com.tinkerpop.rexster.extension.RexsterExtension could not be found.",
                            sce);
                    throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
                }

                if (rexsterExtensions == null || rexsterExtensions.size() == 0) {
                    // extension was not found for some reason
                    logger.error("The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "].  Check com.tinkerpop.rexster.extension.RexsterExtension file in META-INF.services.");
                    final JSONObject error = generateErrorObject(
                            "The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "]");
                    throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
                }

                // look up the method on the extension that needs to be called.
                methodToCall = findExtensionMethod(rexsterExtensions, ExtensionPoint.EDGE, extensionSegmentSet.getExtensionMethod(), httpMethodRequested);

                if (methodToCall == null) {
                    // extension method was not found for some reason
                    if (httpMethodRequested == HttpMethod.OPTIONS) {
                        // intercept the options call and return the standard business
                        // no need to stop the transaction here
                        return buildOptionsResponse();
                    }

                    logger.error("The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "] with a HTTP method of [" + httpMethodRequested.name() + "].  Check com.tinkerpop.rexster.extension.RexsterExtension file in META-INF.services.");
                    final JSONObject error = generateErrorObject(
                            "The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "] with a HTTP method of [" + httpMethodRequested.name() + "]");
                    throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
                }

                // found the method...time to do work
                returnValue = invokeExtension(rag, methodToCall, edge);

            } catch (WebApplicationException wae) {
                // already logged this...just throw it  up.
                rag.tryRollback();
                throw wae;
            } catch (Exception ex) {
                logger.error("Dynamic invocation of the [" + extensionSegmentSet + "] extension failed.", ex);

                if (ex.getCause() != null) {
                    final Throwable cause = ex.getCause();
                    logger.error("It would be smart to trap this this exception within the extension and supply a good response to the user:" + cause.getMessage(), cause);
                }

                rag.tryRollback();

                final JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }

            if (returnValue instanceof ExtensionResponse) {
                extResponse = (ExtensionResponse) returnValue;

                if (extResponse.isErrorResponse()) {
                    // an error was raised within the extension.  pass it back out as an error.
                    logger.warn("The [" + extensionSegmentSet + "] extension raised an error response.");

                    if (methodToCall.getExtensionDefinition().autoCommitTransaction()) {
                        rag.tryRollback();
                    }

                    throw new WebApplicationException(Response.fromResponse(extResponse.getJerseyResponse()).build());
                }

                if (methodToCall.getExtensionDefinition().autoCommitTransaction()) {
                    rag.tryCommit();
                }

            } else {
                // extension method is not returning the correct type...needs to be an ExtensionResponse
                logger.error("The [" + extensionSegmentSet + "] extension does not return an ExtensionResponse.");
                final JSONObject error = generateErrorObject(
                        "The [" + extensionSegmentSet + "] extension does not return an ExtensionResponse.");
                throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }

        } else {
            // namespace was not allowed
            logger.error("The [" + extensionSegmentSet + "] extension was not configured for [" + graphName + "]");
            final JSONObject error = generateErrorObject(
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
     * POST http://host/graph/edges
     * graph.addEdge(null);
     */
    @POST
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Consumes({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON})
    @Timed(name = "http.rest.edges.object.post", absolute = true)
    public Response postNullEdgeConsumesJson(@Context final Request request,
                                             @PathParam("graphname") final String graphName, final JSONObject json) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.setRequestObject(json);
        Variant v = request.selectVariant(producesVariantList);
        return this.postEdge(graphName, null, false, v);
    }

    /**
     * POST http://host/graph/edges
     * graph.addEdge(null);
     */
    @POST
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Consumes({RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Timed(name = "http.rest.edges.object.post", absolute = true)
    public Response postNullEdgeConsumesTypedJson(@Context final Request request,
                                                  @PathParam("graphname") final String graphName, final JSONObject json) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.setRequestObject(json);
        Variant v = request.selectVariant(producesVariantList);
        return this.postEdge(graphName, null, true, v);
    }

    /**
     * POST http://host/graph/edge?_inV=id1&_outV=id2&label=string&key=value
     * Edge e = graph.addEdge(null,graph.getVertex(id1),graph.getVertex(id2),label);
     * e.setProperty(key,value);
     */
    @POST
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Timed(name = "http.rest.edges.object.post", absolute = true)
    public Response postNullEdgeConsumesUri(@Context final Request request, @PathParam("graphname") final String graphName) {
        Variant v = request.selectVariant(producesVariantList);
        return this.postEdge(graphName, null, true, v);
    }

    /**
     * POST http://host/graph/edge/id
     * Edge e = graph.addEdge(id,graph.getVertex(id1),graph.getVertex(id2),label);
     * e.setProperty(key,value);
     */
    @POST
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Consumes({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON})
    @Timed(name = "http.rest.edges.object.post", absolute = true)
    public Response postEdgeConsumesJson(@Context final Request request,
                                         @PathParam("graphname") final String graphName,
                                         @PathParam("id") final String id, final JSONObject json) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.setRequestObject(json);
        Variant v = request.selectVariant(producesVariantList);
        return this.postEdge(graphName, id, false, v);
    }

    /**
     * POST http://host/graph/edge/id?_inV=id1&_outV=id2&label=string&key=value
     * Edge e = graph.addEdge(id,graph.getVertex(id1),graph.getVertex(id2),label);
     * e.setProperty(key,value);
     */
    @POST
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Consumes({RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Timed(name = "http.rest.edges.object.post", absolute = true)
    public Response postEdgeConsumesTypedJson(@Context final Request request,
                                              @PathParam("graphname") final String graphName,
                                              @PathParam("id") final String id, final JSONObject json) {
        this.setRequestObject(json);
        Variant v = request.selectVariant(producesVariantList);
        return postEdge(graphName, id, true, v);
    }

    /**
     * POST http://host/graph/edge/id?_inV=id1&_outV=id2&label=string&key=value
     * Edge e = graph.addEdge(id,graph.getVertex(id1),graph.getVertex(id2),label);
     * e.setProperty(key,value);
     */
    @POST
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Timed(name = "http.rest.edges.object.post", absolute = true)
    public Response postEdgeConsumesUri(@Context final Request request,
                                        @PathParam("graphname") final String graphName,
                                        @PathParam("id") final String id) {
        Variant v = request.selectVariant(producesVariantList);
        return postEdge(graphName, id, true, v);
    }

    private Response postEdge(final @PathParam("graphname") String graphName, final @PathParam("id") String id,
                              final boolean parseTypes, final Variant variant) {

        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
        final Graph graph = rag.getGraph();

        final JSONObject theRequestObject = this.getRequestObject();

        final MediaType produces = variant.getMediaType();

        final boolean showTypes = produces.equals(RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON_TYPE);
        final boolean showHypermedia = produces.equals(RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON_TYPE)
                || produces.equals(RexsterMediaType.APPLICATION_REXSTER_JSON_TYPE);

        String inV = null;
        Object temp = theRequestObject.opt(Tokens._IN_V);
        if (null != temp)
            inV = temp.toString();
        String outV = null;
        temp = theRequestObject.opt(Tokens._OUT_V);
        if (null != temp)
            outV = temp.toString();
        String label = null;
        temp = theRequestObject.opt(Tokens._LABEL);
        if (null != temp)
            label = temp.toString();

        try {
            // blueprints throws IllegalArgumentException if the id is null
            Edge edge = id == null ? null : graph.getEdge(id);
            if (null == edge && null != outV && null != inV && null != label) {
                // there is no edge but the in/out vertex params and label are present so
                // validate that the vertexes are present before creating the edge
                final Vertex out = graph.getVertex(outV);
                final Vertex in = graph.getVertex(inV);
                if (null != out && null != in) {
                    // in/out vertexes are found so edge can be created
                    edge = graph.addEdge(id, out, in, label);
                } else {
                    final JSONObject error = generateErrorObjectJsonFail(new Exception("One or both of the vertices for the edge does not exist in the graph."));
                    throw new WebApplicationException(Response.status(Status.CONFLICT).entity(error).build());
                }

            } else if (edge != null) {
                if (!RequestObjectHelper.hasElementProperties(theRequestObject)) {
                    // if the edge exists there better be some properties to assign
                    // this really isn't a BAD_REQUEST, but CONFLICT isn't much better...bah
                    final JSONObject error = generateErrorObjectJsonFail(new Exception("Edge with id " + id + " already exists"));
                    throw new WebApplicationException(Response.status(Status.CONFLICT).entity(error).build());
                }
            }

            try {
                if (edge != null) {
                    final Iterator keys = theRequestObject.keys();
                    while (keys.hasNext()) {
                        final String key = keys.next().toString();
                        if (!key.startsWith(Tokens.UNDERSCORE)) {
                            edge.setProperty(key, ElementHelper.getTypedPropertyValue(theRequestObject.get(key), parseTypes));
                        }
                    }

                    final GraphSONMode mode = showTypes ? GraphSONMode.EXTENDED : GraphSONMode.NORMAL;
                    final Set<String> returnKeys = RequestObjectHelper.getReturnKeys(theRequestObject);
                    final JSONObject elementJson = GraphSONUtility.jsonFromElement(edge, returnKeys, mode);

                    rag.tryCommit();

                    // some graph implementations close scope at the close of the transaction so we generate the
                    // JSON before the transaction but set the id after for graphs that don't generate the id
                    // until the transaction is committed
                    elementJson.put(Tokens._ID, edge.getId());

                    this.resultObject.put(Tokens.RESULTS, elementJson);

                } else {
                    // edge could not be found.  likely an error condition on the request
                    final JSONObject error = generateErrorObjectJsonFail(new Exception("Edge cannot be found or created.  Please check the format of the request."));
                    throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
                }

                if (showHypermedia) {
                    final JSONArray extensionsList = rag.getExtensionHypermedia(ExtensionPoint.EDGE, this.getUriPath());
                    if (extensionsList != null) {
                        this.resultObject.put(Tokens.EXTENSIONS, extensionsList);
                    }
                }

                this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
            } catch (JSONException ex) {
                logger.error(ex);

                final JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }


        } catch (WebApplicationException wae) {
            rag.tryRollback();
            throw wae;
        } catch (Exception ex) {
            rag.tryRollback();
            JSONObject error = generateErrorObject("Transaction failed on POST of edge.", ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    /**
     * PUT http://host/graph/edge/id
     * Edge e = graph.addEdge(id,graph.getVertex(id1),graph.getVertex(id2),label);
     * e.setProperty(key,value);
     */
    @PUT
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Consumes({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON})
    @Timed(name = "http.rest.edges.object.put", absolute = true)
    public Response putEdgeConsumesJson(@Context Request request, @PathParam("graphname") String graphName, @PathParam("id") String id, JSONObject json) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.setRequestObject(json);
        Variant v = request.selectVariant(producesVariantList);
        return this.putEdge(graphName, id, false, v);
    }

    /**
     * PUT http://host/graph/edge/id?key=value
     * remove all properties
     * e.setProperty(key,value);
     */
    @PUT
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Consumes({RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Timed(name = "http.rest.edges.object.put", absolute = true)
    public Response putEdgeConsumesTypedJson(@Context Request request, @PathParam("graphname") String graphName, @PathParam("id") String id, JSONObject json) {
        this.setRequestObject(json);
        Variant v = request.selectVariant(producesVariantList);
        return this.putEdge(graphName, id, true, v);
    }

    /**
     * PUT http://host/graph/edge/id?key=value
     * remove all properties
     * e.setProperty(key,value);
     */
    @PUT
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Timed(name = "http.rest.edges.object.put", absolute = true)
    public Response putEdgeOnUri(@Context Request request, @PathParam("graphname") String graphName, @PathParam("id") String id) {
        Variant v = request.selectVariant(producesVariantList);
        return this.putEdge(graphName, id, true, v);
    }

    private Response putEdge(final String graphName, final String id, final boolean parseTypes, final Variant variant) {

        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
        final Graph graph = rag.getGraph();

        final MediaType produces = variant.getMediaType();

        final boolean showTypes = produces.equals(RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON_TYPE);
        final boolean showHypermedia = produces.equals(RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON_TYPE)
                || produces.equals(RexsterMediaType.APPLICATION_REXSTER_JSON_TYPE);

        try {
            final Edge edge = graph.getEdge(id);
            if (edge == null) {
                String msg = "Edge with id [" + id + "] cannot be found.";
                logger.info(msg);
                JSONObject error = generateErrorObject(msg);
                throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
            }

            // remove all properties as this is a replace operation
            com.tinkerpop.blueprints.util.ElementHelper.removeProperties(new ArrayList<Element>() {{
                add(edge);
            }});

            final JSONObject theRequestObject = this.getRequestObject();
            final GraphSONMode mode = showTypes ? GraphSONMode.EXTENDED : GraphSONMode.NORMAL;
            final Set<String> returnKeys = RequestObjectHelper.getReturnKeys(theRequestObject);

            final Iterator keys = theRequestObject.keys();
            while (keys.hasNext()) {
                final String key = keys.next().toString();
                if (!key.startsWith(Tokens.UNDERSCORE)) {
                    edge.setProperty(key, ElementHelper.getTypedPropertyValue(theRequestObject.get(key), parseTypes));
                }
            }

            final JSONObject elementJson = GraphSONUtility.jsonFromElement(edge, returnKeys, mode);

            rag.tryCommit();

            // some graph implementations close scope at the close of the transaction so we generate the
            // JSON before the transaction but set the id after for graphs that don't generate the id
            // until the transaction is committed
            elementJson.put(Tokens._ID, edge.getId());

            this.resultObject.put(Tokens.RESULTS, elementJson);

            if (showHypermedia) {
                final JSONArray extensionsList = rag.getExtensionHypermedia(ExtensionPoint.EDGE, this.getUriPath());
                if (extensionsList != null) {
                    this.resultObject.put(Tokens.EXTENSIONS, extensionsList);
                }
            }

            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());

        } catch (WebApplicationException wae) {
            rag.tryRollback();
            throw wae;
        } catch (Exception ex) {
            rag.tryRollback();

            logger.error(ex);

            final JSONObject error = generateErrorObject(ex.getMessage(), ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    /**
     * DELETE http://host/graph/edge/id
     * graph.removeEdge(graph.getEdge(id));
     * <p/>
     * DELETE http://host/graph/edge/id?key1&key2
     * Edge e = graph.getEdge(id);
     * e.removeProperty(key1);
     * e.removeProperty(key2);
     */
    @DELETE
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Timed(name = "http.rest.edges.object.delete", absolute = true)
    public Response deleteEdge(@PathParam("graphname") String graphName, @PathParam("id") String id) {

        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
        final Graph graph = rag.getGraph();

        try {
            final List<String> keys = this.getNonRexsterRequestKeys();
            final Edge edge = graph.getEdge(id);
            if (null != edge) {
                if (keys.size() > 0) {
                    // delete edge properties
                    for (final String key : keys) {
                        edge.removeProperty(key);
                    }
                } else {
                    // delete edge
                    graph.removeEdge(edge);
                }
            } else {
                final String msg = "Edge with id [" + id + "] cannot be found.";
                logger.info(msg);
                final JSONObject error = generateErrorObject(msg);
                throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
            }

            rag.tryCommit();

            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException ex) {
            logger.error(ex);

            rag.tryRollback();

            final JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } catch (WebApplicationException wae) {
            rag.tryRollback();
            throw wae;
        } catch (Exception ex) {
            rag.tryRollback();
            final JSONObject error = generateErrorObject("Transaction failed on DELETE of edge.", ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();

    }
}

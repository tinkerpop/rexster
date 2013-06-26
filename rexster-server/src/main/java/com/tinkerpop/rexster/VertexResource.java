package com.tinkerpop.rexster;

import com.codahale.metrics.annotation.Timed;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.VertexQuery;
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
 * Vertex resource.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@Path("/graphs/{graphname}/vertices")
public class VertexResource extends AbstractSubResource {

    private static Logger logger = Logger.getLogger(VertexResource.class);

    public VertexResource() {
        super(null);
    }

    public VertexResource(final UriInfo ui, final HttpServletRequest req, final RexsterApplication ra) {
        super(ra);
        this.httpServletRequest = req;
        this.uriInfo = ui;
    }

    @OPTIONS
    public Response optionsVertices() {
        return buildOptionsResponse(HttpMethod.GET.toString(),
                HttpMethod.POST.toString());
    }

    /**
     * GET http://host/graph/vertices
     * graph.getVertices();
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON})
    @Timed(name = "http.rest.vertices.collection.get", absolute = true)
    public Response getVertices(@PathParam("graphname") final String graphName) {
        return getVertices(graphName, false);
    }

    @GET
    @Produces({RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Timed(name = "http.rest.vertices.collection.get", absolute = true)
    public Response getVerticesRexsterJson(@PathParam("graphname") final String graphName) {
        return getVertices(graphName, true);
    }

    private Response getVertices(final String graphName, final boolean showTypes) {
        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
        final Graph graph = rag.getGraph();

        final GraphSONMode mode = showTypes ? GraphSONMode.EXTENDED : GraphSONMode.NORMAL;

        final JSONObject theRequestObject = this.getRequestObject();
        final Long start = RequestObjectHelper.getStartOffset(theRequestObject);
        final Long end = RequestObjectHelper.getEndOffset(theRequestObject);
        final Set<String> returnKeys = RequestObjectHelper.getReturnKeys(theRequestObject);

        String key = null;
        Object value = null;

        Object temp = theRequestObject.opt(Tokens.KEY);
        if (null != temp)
            key = temp.toString();

        temp = theRequestObject.opt(Tokens.VALUE);
        if (null != temp)
            value = ElementHelper.getTypedPropertyValue(temp.toString());

        final boolean filtered = key != null && value != null;

        try {
            long counter = 0l;
            final JSONArray vertexArray = new JSONArray();
            boolean wasInSection = false;

            final Iterable<Vertex> vertices = filtered ? graph.getVertices(key, value) : graph.getVertices();
            for (Vertex vertex : vertices) {
                if (counter >= start && counter < end) {
                    wasInSection = true;
                    vertexArray.put(GraphSONUtility.jsonFromElement(vertex, returnKeys, mode));
                } else if (wasInSection) {
                    break;
                }
                counter++;
            }

            this.resultObject.put(Tokens.RESULTS, vertexArray);
            this.resultObject.put(Tokens.TOTAL_SIZE, vertexArray.length());
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

        } catch (JSONException ex) {
            logger.error(ex);
            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } catch (RuntimeException re) {
            logger.error(re);

            JSONObject error = generateErrorObject(re.getMessage(), re);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } finally {
            rag.tryCommit();
        }

        return Response.ok(this.resultObject).build();
    }

    @OPTIONS
    @Path("/{id}")
    public Response optionsSingleVertex() {
        return buildOptionsResponse();
    }

    /**
     * GET http://host/graph/vertices/id
     * graph.getVertex(id);
     */
    @GET
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    @Timed(name = "http.rest.vertices.object.get", absolute = true)
    public Response getSingleVertex(@PathParam("graphname") final String graphName, @PathParam("id") final String id) {
        return getSingleVertex(graphName, id, false, false);
    }

    @GET
    @Path("/{id}")
    @Produces({RexsterMediaType.APPLICATION_REXSTER_JSON})
    @Timed(name = "http.rest.vertices.object.get", absolute = true)
    public Response getSingleVertexRexsterJson(@PathParam("graphname") final String graphName, @PathParam("id") final String id) {
        return getSingleVertex(graphName, id, false, true);
    }

    @GET
    @Path("/{id}")
    @Produces({RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Timed(name = "http.rest.vertices.object.get", absolute = true)
    public Response getSingleVertexRexsterTypedJson(@PathParam("graphname") final String graphName, @PathParam("id") final String id) {
        return getSingleVertex(graphName, id, true, true);
    }

    private Response getSingleVertex(final String graphName, final String id, final boolean showTypes, final boolean showHypermedia) {
        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
        try {

            final Vertex vertex = rag.getGraph().getVertex(id);
            if (null != vertex) {

                final JSONObject theRequestObject = this.getRequestObject();
                final GraphSONMode mode = showTypes ? GraphSONMode.EXTENDED : GraphSONMode.NORMAL;
                final Set<String> returnKeys = RequestObjectHelper.getReturnKeys(theRequestObject);

                this.resultObject.put(Tokens.RESULTS, GraphSONUtility.jsonFromElement(vertex, returnKeys, mode));
                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

                if (showHypermedia) {
                    JSONArray extensionsList = rag.getExtensionHypermedia(ExtensionPoint.VERTEX, this.getUriPath());
                    if (extensionsList != null) {
                        this.resultObject.put(Tokens.EXTENSIONS, extensionsList);
                    }
                }

            } else {
                final String msg = "Vertex with [" + id + "] cannot be found.";
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
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in)(?!query).+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response headVertexExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id, final JSONObject json) {
        this.setRequestObject(json);
        return this.executeVertexExtension(graphName, id, HttpMethod.HEAD);
    }

    @HEAD
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in)(?!query).+}")
    public Response headVertexExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id) {
        return this.executeVertexExtension(graphName, id, HttpMethod.HEAD);
    }

    @PUT
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in)(?!query).+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed(name = "http.rest.vertices.extension.put", absolute = true)
    public Response putVertexExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id, final JSONObject json) {
        this.setRequestObject(json);
        return this.executeVertexExtension(graphName, id, HttpMethod.PUT);
    }

    @PUT
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in)(?!query).+}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Timed(name = "http.rest.vertices.extension.put", absolute = true)
    public Response putVertexExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id, final MultivaluedMap<String, String> formParams) {
        this.setRequestObject(formParams);
        return this.executeVertexExtension(graphName, id, HttpMethod.PUT);
    }

    @PUT
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in)(?!query).+}")
    @Timed(name = "http.rest.vertices.extension.put", absolute = true)
    public Response putVertexExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id) {
        return this.executeVertexExtension(graphName, id, HttpMethod.PUT);
    }

    @OPTIONS
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in)(?!query).+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response optionsVertexExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id, final JSONObject json) {
        this.setRequestObject(json);
        return this.executeVertexExtension(graphName, id, HttpMethod.OPTIONS);
    }

    @OPTIONS
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in)(?!query).+}")
    public Response optionsVertexExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id) {
        return this.executeVertexExtension(graphName, id, HttpMethod.OPTIONS);
    }

    @DELETE
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in)(?!query).+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed(name = "http.rest.vertices.extension.delete", absolute = true)
    public Response deleteVertexExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id, final JSONObject json) {
        this.setRequestObject(json);
        return this.executeVertexExtension(graphName, id, HttpMethod.DELETE);
    }

    @DELETE
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in)(?!query).+}")
    @Timed(name = "http.rest.vertices.extension.delete", absolute = true)
    public Response deleteVertexExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id) {
        return this.executeVertexExtension(graphName, id, HttpMethod.DELETE);
    }

    @POST
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in)(?!query).+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed(name = "http.rest.vertices.extension.post", absolute = true)
    public Response postVertexExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id, final JSONObject json) {
        this.setRequestObject(json);
        return this.executeVertexExtension(graphName, id, HttpMethod.POST);
    }

    @POST
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in)(?!query).+}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Timed(name = "http.rest.vertices.extension.post", absolute = true)
    public Response postVertexExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id, final MultivaluedMap<String, String> formParams) {
        this.setRequestObject(formParams);
        return this.executeVertexExtension(graphName, id, HttpMethod.POST);
    }

    @POST
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in)(?!query).+}")
    @Timed(name = "http.rest.vertices.extension.post", absolute = true)
    public Response postVertexExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id) {
        return this.executeVertexExtension(graphName, id, HttpMethod.POST);
    }

    @GET
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in)(?!query).+}")
    @Timed(name = "http.rest.vertices.extension.get", absolute = true)
    public Response getVertexExtension(@PathParam("graphname") final String graphName, @PathParam("id") final String id) {
        return this.executeVertexExtension(graphName, id, HttpMethod.GET);
    }

    private Response executeVertexExtension(final String graphName, final String id, final HttpMethod httpMethodRequested) {

        final Vertex vertex = this.getRexsterApplicationGraph(graphName).getGraph().getVertex(id);

        ExtensionResponse extResponse;
        ExtensionMethod methodToCall;
        final ExtensionSegmentSet extensionSegmentSet = parseUriForExtensionSegment(graphName, ExtensionPoint.VERTEX);

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
                methodToCall = findExtensionMethod(rexsterExtensions, ExtensionPoint.VERTEX, extensionSegmentSet.getExtensionMethod(), httpMethodRequested);

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
                returnValue = invokeExtension(rag, methodToCall, vertex);

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

                rag.tryRollback();

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

    @OPTIONS
    @Path("/{id}/{direction}")
    public Response optionsVertexEdges() {
        return buildOptionsResponse();
    }

    /**
     * GET http://host/graph/vertices/id/direction
     * graph.getVertex(id).get{Direction}Edges();
     */
    @GET
    @Path("/{id}/{direction}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON})
    @Timed(name = "http.rest.vertices.object.edges.collection.get", absolute = true)
    public Response getVertexEdges(@PathParam("graphname") String graphName, @PathParam("id") String vertexId, @PathParam("direction") String direction) {
        return this.getVertexEdges(graphName, vertexId, direction, false);
    }

    @GET
    @Path("/{id}/{direction}")
    @Produces({RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Timed(name = "http.rest.vertices.object.edges.collection.get", absolute = true)
    public Response getVertexEdgesRexsterTypedJson(@PathParam("graphname") String graphName, @PathParam("id") String vertexId, @PathParam("direction") String direction) {
        return this.getVertexEdges(graphName, vertexId, direction, true);
    }

    private Response getVertexEdges(String graphName, String vertexId, String direction, boolean showTypes) {
        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);

        try {
            final Vertex vertex = rag.getGraph().getVertex(vertexId);
            if (vertex == null) {
                final String msg = "Vertex with [" + vertexId + "] cannot be found.";
                logger.info(msg);

                final JSONObject error = generateErrorObject(msg);
                throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
            }

            final GraphSONMode mode = showTypes ? GraphSONMode.EXTENDED : GraphSONMode.NORMAL;
            final JSONObject theRequestObject = this.getRequestObject();
            final Long start = RequestObjectHelper.getStartOffset(theRequestObject);
            final Long end = RequestObjectHelper.getEndOffset(theRequestObject);
            final Set<String> returnKeys = RequestObjectHelper.getReturnKeys(theRequestObject);

            // accept either an array of labels or just one label
            final String[] labels = getLabelsFromRequest(theRequestObject);

            // break out the segment into the return and the direction
            final VertexQueryArguments queryArguments = new VertexQueryArguments(direction);

            // if this is a query and the _return is "count" then we don't bother to send back the
            // result array
            final boolean countOnly = queryArguments.isCountOnly();

            // what kind of data the calling client wants back (vertices, edges, count, vertex identifiers)
            final ReturnType returnType = queryArguments.getReturnType();

            // the query direction (both, out, in)
            final Direction queryDirection = queryArguments.getQueryDirection();

            long counter = 0l;
            final JSONArray elementArray = new JSONArray();

            VertexQuery query = vertex.query().direction(queryDirection);
            if (labels != null) {
                query = query.labels(labels);
            }

            // add .has() elements to query
            RequestObjectHelper.buildQueryProperties(theRequestObject, query);

            // need to include limits in addition to standard rexster paging as there is buffering going on via
            // rexstergraph that requires both ... meaning, i might do a vertex query with limit that exceeds my
            // buffer size for rexstergraph.
            final int limitTake = theRequestObject.has(Tokens._TAKE) ? theRequestObject.getInt(Tokens._TAKE) : Integer.MIN_VALUE;
            //final long limitSkip = theRequestObject.has(Tokens._SKIP) ? theRequestObject.getLong(Tokens._SKIP) : Long.MIN_VALUE;
            if (limitTake >= 0) {
                query = query.limit(limitTake);
            }

            if (returnType == ReturnType.VERTICES || returnType == ReturnType.VERTEX_IDS) {
                final Iterable<Vertex> vertexQueryResults = query.vertices();
                for (Vertex v : vertexQueryResults) {
                    if (counter >= start && counter < end) {
                        if (returnType.equals(ReturnType.VERTICES)) {
                            elementArray.put(GraphSONUtility.jsonFromElement(v, returnKeys, mode));
                        } else {
                            elementArray.put(v.getId());
                        }
                    }
                    counter++;
                }
            } else if (returnType == ReturnType.EDGES) {
                final Iterable<Edge> edgeQueryResults = query.edges();
                for (Edge e : edgeQueryResults) {
                    if (counter >= start && counter < end) {
                        elementArray.put(GraphSONUtility.jsonFromElement(e, returnKeys, mode));
                    }
                    counter++;
                }
            } else if (returnType == ReturnType.COUNT) {
                counter = query.count();
            } else {
                final JSONObject error = generateErrorObject(direction + " direction segment was invalid.");
                throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity(error).build());
            }

            if (!countOnly) {
                this.resultObject.put(Tokens.RESULTS, elementArray);
            }

            this.resultObject.put(Tokens.TOTAL_SIZE, counter);
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

        } catch (JSONException ex) {
            logger.error(ex);

            final JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (RuntimeException re) {
            logger.error(re);

            final JSONObject error = generateErrorObject(re.getMessage(), re);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } finally {
            rag.tryCommit();
        }

        return Response.ok(this.resultObject).build();
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Consumes({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON})
    @Timed(name = "http.rest.vertices.object.post", absolute = true)
    public Response postNullVertexRexsterConsumesJson(@Context final Request request, @PathParam("graphname") final String graphName, final JSONObject json) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.setRequestObject(json);
        final Variant v = request.selectVariant(producesVariantList);
        return this.postVertex(graphName, null, false, v);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Consumes({RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Timed(name = "http.rest.vertices.object.post", absolute = true)
    public Response postNullVertexRexsterConsumesTypedJson(@Context final Request request, @PathParam("graphname") final String graphName, final JSONObject json) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.setRequestObject(json);
        final Variant v = request.selectVariant(producesVariantList);
        return this.postVertex(graphName, null, true, v);
    }

    /**
     * POST http://host/graph/vertices
     * graph.addVertex(null);
     */
    @POST
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Timed(name = "http.rest.vertices.object.post", absolute = true)
    public Response postNullVertexOnUri(@Context final Request request, @PathParam("graphname") final String graphName) {
        final Variant v = request.selectVariant(producesVariantList);
        return this.postVertex(graphName, null, true, v);
    }

    /**
     * POST http://host/graph/vertices/id
     * Vertex v = graph.addVertex(id);
     * v.setProperty(key,value);
     */
    @POST
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Consumes({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON})
    @Timed(name = "http.rest.vertices.object.post", absolute = true)
    public Response postVertexConsumesJson(@Context final Request request, @PathParam("graphname") final String graphName, @PathParam("id") final String id, final JSONObject json) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.setRequestObject(json);
        final Variant v = request.selectVariant(producesVariantList);
        return this.postVertex(graphName, id, false, v);
    }

    @POST
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Consumes({RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Timed(name = "http.rest.vertices.object.post", absolute = true)
    public Response postVertexRexsterConsumesTypedJson(@Context final Request request, @PathParam("graphname") final String graphName, @PathParam("id") final String id, final JSONObject json) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.setRequestObject(json);
        final Variant v = request.selectVariant(producesVariantList);
        return this.postVertex(graphName, id, true, v);
    }

    /**
     * POST http://host/graph/vertices/id?key=value
     * Vertex v = graph.addVertex(id);
     * v.setProperty(key,value);
     */
    @POST
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Timed(name = "http.rest.vertices.object.post", absolute = true)
    public Response postVertexOnUri(@Context final Request request, @PathParam("graphname") final String graphName, @PathParam("id") final String id) {
        final Variant v = request.selectVariant(producesVariantList);
        return postVertex(graphName, id, true, v);
    }

    private Response postVertex(final String graphName, final String id, final boolean parseTypes, final Variant variant) {
        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
        final Graph graph = rag.getGraph();

        final MediaType produces = variant.getMediaType();

        final boolean showTypes = produces.equals(RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON_TYPE);
        final boolean showHypermedia = produces.equals(RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON_TYPE)
                || produces.equals(RexsterMediaType.APPLICATION_REXSTER_JSON_TYPE);

        try {
            // blueprints throws IllegalArgumentException if the id is null
            Vertex vertex = id == null ? null : graph.getVertex(id);

            final JSONObject theRequestObject = this.getRequestObject();

            if (null == vertex) {
                vertex = graph.addVertex(id);
            } else {
                if (!RequestObjectHelper.hasElementProperties(theRequestObject)) {
                    // if the edge exists there better be some properties to assign
                    // this really isn't a BAD_REQUEST, but CONFLICT isn't much better...bah
                    JSONObject error = generateErrorObjectJsonFail(new Exception("Vertex with id " + id + " already exists"));
                    throw new WebApplicationException(Response.status(Status.CONFLICT).entity(error).build());
                }
            }

            final Iterator keys = theRequestObject.keys();
            while (keys.hasNext()) {
                final String key = keys.next().toString();
                if (!key.startsWith(Tokens.UNDERSCORE)) {
                    vertex.setProperty(key, ElementHelper.getTypedPropertyValue(theRequestObject.get(key), parseTypes));
                }
            }

            final GraphSONMode mode = showTypes ? GraphSONMode.EXTENDED : GraphSONMode.NORMAL;
            final Set<String> returnKeys = RequestObjectHelper.getReturnKeys(theRequestObject);
            final JSONObject elementJson = GraphSONUtility.jsonFromElement(vertex, returnKeys, mode);

            rag.tryCommit();

            // some graph implementations close scope at the close of the transaction so we generate the
            // JSON before the transaction but set the id after for graphs that don't generate the id
            // until the transaction is committed
            elementJson.put(Tokens._ID, vertex.getId());

            this.resultObject.put(Tokens.RESULTS, elementJson);

            if (showHypermedia) {
                final JSONArray extensionsList = rag.getExtensionHypermedia(ExtensionPoint.VERTEX, this.getUriPath());
                if (extensionsList != null) {
                    this.resultObject.put(Tokens.EXTENSIONS, extensionsList);
                }
            }

            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException ex) {
            rag.tryRollback();

            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } catch (RuntimeException re) {
            rag.tryRollback();

            logger.error(re);

            JSONObject error = generateErrorObject(re.getMessage(), re);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    /**
     * PUT http://host/graph/vertices/id
     * Vertex v = graph.addVertex(id);
     * v.setProperty(key,value);
     */
    @PUT
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Consumes({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON})
    @Timed(name = "http.rest.vertices.object.put", absolute = true)
    public Response putVertexConsumesJson(@Context Request request, @PathParam("graphname") String graphName, @PathParam("id") String id, JSONObject json) {
        // initializes the request object with the data PUTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.setRequestObject(json);
        Variant v = request.selectVariant(producesVariantList);
        return this.putVertex(graphName, id, false, v);
    }

    @PUT
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Consumes(RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON)
    @Timed(name = "http.rest.vertices.object.put", absolute = true)
    public Response putVertexConsumesTypedJson(@Context Request request, @PathParam("graphname") String graphName, @PathParam("id") String id, JSONObject json) {
        // initializes the request object with the data PUTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.setRequestObject(json);
        Variant v = request.selectVariant(producesVariantList);
        return this.putVertex(graphName, id, true, v);
    }

    /**
     * PUT http://host/graph/vertices/id?key=value
     * remove all properties
     * v.setProperty(key,value);
     */
    @PUT
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Timed(name = "http.rest.vertices.object.put", absolute = true)
    public Response putVertexConsumesUri(@Context Request request, @PathParam("graphname") String graphName, @PathParam("id") String id) {
        Variant v = request.selectVariant(producesVariantList);
        return putVertex(graphName, id, true, v);
    }

    private Response putVertex(final String graphName, final String id, final boolean parseTypes, final Variant variant) {
        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
        final Graph graph = rag.getGraph();

        final MediaType produces = variant.getMediaType();
        final boolean showTypes = produces.equals(RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON_TYPE);
        final boolean showHypermedia = produces.equals(RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON_TYPE)
                || produces.equals(RexsterMediaType.APPLICATION_REXSTER_JSON_TYPE);

        try {
            final Vertex vertex = graph.getVertex(id);

            if (null == vertex) {
                final String msg = "Vertex with [" + id + "] cannot be found.";
                logger.info(msg);

                final JSONObject error = generateErrorObject(msg);
                throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
            }

            // remove all properties as this is a replace operation
            com.tinkerpop.blueprints.util.ElementHelper.removeProperties(new ArrayList<Element>() {{
                add(vertex);
            }});

            final JSONObject theRequestObject = this.getRequestObject();
            final Iterator keys = theRequestObject.keys();
            while (keys.hasNext()) {
                final String key = keys.next().toString();
                if (!key.startsWith(Tokens.UNDERSCORE)) {
                    vertex.setProperty(key, ElementHelper.getTypedPropertyValue(theRequestObject.get(key), parseTypes));
                }
            }

            rag.tryCommit();

            // some graph implementations close scope at the close of the transaction so this has to be
            // reconstituted
            final Vertex reconstitutedElement = graph.getVertex(vertex.getId());

            final GraphSONMode mode = showTypes ? GraphSONMode.EXTENDED : GraphSONMode.NORMAL;
            final Set<String> returnKeys = RequestObjectHelper.getReturnKeys(theRequestObject);
            this.resultObject.put(Tokens.RESULTS, GraphSONUtility.jsonFromElement(reconstitutedElement, returnKeys, mode));

            if (showHypermedia) {
                final JSONArray extensionsList = rag.getExtensionHypermedia(ExtensionPoint.VERTEX, this.getUriPath());
                if (extensionsList != null) {
                    this.resultObject.put(Tokens.EXTENSIONS, extensionsList);
                }
            }

            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException ex) {
            rag.tryRollback();

            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } catch (RuntimeException re) {
            rag.tryRollback();

            logger.error(re);

            JSONObject error = generateErrorObject(re.getMessage(), re);
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
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Timed(name = "http.rest.vertices.object.delete", absolute = true)
    public Response deleteVertex(@PathParam("graphname") String graphName, @PathParam("id") String id) {
        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
        final Graph graph = rag.getGraph();

        try {
            final List<String> keys = this.getNonRexsterRequestKeys();
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
                final String msg = "Vertex with [" + id + "] cannot be found.";
                logger.info(msg);

                JSONObject error = generateErrorObject(msg);
                throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
            }

            rag.tryCommit();
            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException ex) {

            rag.tryRollback();

            logger.error(ex);

            final JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } catch (RuntimeException re) {

            rag.tryRollback();

            logger.error(re);

            final JSONObject error = generateErrorObject(re.getMessage(), re);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();

    }

    private static String[] getLabelsFromRequest(JSONObject theRequestObject) {
        JSONArray labelSet = theRequestObject.optJSONArray(Tokens._LABEL);
        if (labelSet == null) {
            final String oneLabel = theRequestObject.optString(Tokens._LABEL);
            if (oneLabel != null && !oneLabel.isEmpty()) {
                labelSet = new JSONArray();
                labelSet.put(oneLabel);
            }
        }

        String[] labels = null;
        if (labelSet != null) {
            labels = new String[labelSet.length()];
            for (int ix = 0; ix < labelSet.length(); ix++) {
                labels[ix] = labelSet.optString(ix);
            }
        }
        return labels;
    }

    private enum ReturnType {VERTICES, EDGES, COUNT, VERTEX_IDS}

    private final class VertexQueryArguments {

        private final Direction queryDirection;
        private final ReturnType returnType;
        private final boolean countOnly;

        public VertexQueryArguments(String directionSegment) {
            if (directionSegment.equals(Tokens.OUT_E)) {
                returnType = ReturnType.EDGES;
                queryDirection = Direction.OUT;
                countOnly = false;
            } else if (directionSegment.equals(Tokens.IN_E)) {
                returnType = ReturnType.EDGES;
                queryDirection = Direction.IN;
                countOnly = false;
            } else if (directionSegment.equals(Tokens.BOTH_E)) {
                returnType = ReturnType.EDGES;
                queryDirection = Direction.BOTH;
                countOnly = false;
            } else if (directionSegment.equals(Tokens.OUT)) {
                returnType = ReturnType.VERTICES;
                queryDirection = Direction.OUT;
                countOnly = false;
            } else if (directionSegment.equals(Tokens.IN)) {
                returnType = ReturnType.VERTICES;
                queryDirection = Direction.IN;
                countOnly = false;
            } else if (directionSegment.equals(Tokens.BOTH)) {
                returnType = ReturnType.VERTICES;
                queryDirection = Direction.BOTH;
                countOnly = false;
            } else if (directionSegment.equals(Tokens.BOTH_COUNT)) {
                returnType = ReturnType.COUNT;
                queryDirection = Direction.BOTH;
                countOnly = true;
            } else if (directionSegment.equals(Tokens.IN_COUNT)) {
                returnType = ReturnType.COUNT;
                queryDirection = Direction.IN;
                countOnly = true;
            } else if (directionSegment.equals(Tokens.OUT_COUNT)) {
                returnType = ReturnType.COUNT;
                queryDirection = Direction.OUT;
                countOnly = true;
            } else if (directionSegment.equals(Tokens.BOTH_IDS)) {
                returnType = ReturnType.VERTEX_IDS;
                queryDirection = Direction.BOTH;
                countOnly = false;
            } else if (directionSegment.equals(Tokens.IN_IDS)) {
                returnType = ReturnType.VERTEX_IDS;
                queryDirection = Direction.IN;
                countOnly = false;
            } else if (directionSegment.equals(Tokens.OUT_IDS)) {
                returnType = ReturnType.VERTEX_IDS;
                queryDirection = Direction.OUT;
                countOnly = false;
            } else {
                final JSONObject error = generateErrorObject(directionSegment + " segment was invalid.");
                throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity(error).build());
            }
        }

        public Direction getQueryDirection() {
            return queryDirection;
        }

        public ReturnType getReturnType() {
            return returnType;
        }

        public boolean isCountOnly() {
            return countOnly;
        }

    }
}

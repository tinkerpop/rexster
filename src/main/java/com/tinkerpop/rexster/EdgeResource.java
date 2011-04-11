package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.ExtensionSegmentSet;
import com.tinkerpop.rexster.extension.RexsterExtension;
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
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

@Path("/{graphname}/edges")
@Produces(MediaType.APPLICATION_JSON)
public class EdgeResource extends AbstractSubResource {

    private static Logger logger = Logger.getLogger(EdgeResource.class);

    public EdgeResource() {
        super(null);
    }

    public EdgeResource(UriInfo ui, HttpServletRequest req, RexsterApplicationProvider rap) {
        super(rap);
        this.httpServletRequest = req;
        this.uriInfo = ui;
    }

    /**
     * GET http://host/graph/edges
     * graph.getEdges();
     */
    @GET
    public Response getAllEdges(@PathParam("graphname") String graphName) {

        Long start = this.getStartOffset();
        Long end = this.getEndOffset();

        long counter = 0l;

        try {
            JSONArray edgeArray = new JSONArray();
            for (Edge edge : this.getRexsterApplicationGraph(graphName).getGraph().getEdges()) {
                if (counter >= start && counter < end) {
                    edgeArray.put(new ElementJSONObject(edge, this.getReturnKeys(), this.hasShowTypes()));
                }
                counter++;
            }

            this.resultObject.put(Tokens.RESULTS, edgeArray);
            this.resultObject.put(Tokens.TOTAL_SIZE, counter);
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

        } catch (JSONException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
        }

        return this.addHeaders(Response.ok(this.resultObject)).build();

    }

    /**
     * GET http://host/graph/edges/id
     * graph.getEdge(id);
     */
    @GET
    @Path("/{id}")
    public Response getSingleEdge(@PathParam("graphname") String graphName, @PathParam("id") String id) {
        final Edge edge = this.getRexsterApplicationGraph(graphName).getGraph().getEdge(id);

        if (null != edge) {
            try {
                this.resultObject.put(Tokens.RESULTS, new ElementJSONObject(edge, this.getReturnKeys(), this.hasShowTypes()));
                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

                JSONArray extensionsList = getExtensionHypermedia(ExtensionPoint.EDGE);
                if (extensionsList != null) {
                    this.resultObject.put(Tokens.LINKS, extensionsList);
                }
            } catch (JSONException ex) {
                logger.error(ex);

                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
            }
        } else {
            String msg = "Could not find edge [" + id + "] on graph [" + this.getRexsterApplicationGraph(graphName).getGraphName() + "]";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(this.addHeaders(Response.status(Status.NOT_FOUND).entity(error)).build());
        }

        return this.addHeaders(Response.ok(this.resultObject)).build();
    }

    @POST
    @Path("/{id}/{extension: .+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getGraphExtension(@PathParam("graphname") String graphName, @PathParam("id") String id, JSONObject json) {
        this.setRequestObject(json);
        return this.getGraphExtension(graphName, id);
    }

    @GET
    @Path("/{id}/{extension: .+}")
    public Response getGraphExtension(@PathParam("graphname") String graphName, @PathParam("id") String id) {

        final Edge edge = this.getRexsterApplicationGraph(graphName).getGraph().getEdge(id);

        ExtensionResponse extResponse;
        ExtensionSegmentSet extensionSegmentSet = parseUriForExtensionSegment(graphName, ExtensionPoint.EDGE);

        // determine if the namespace and extension are enabled for this graph
        RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);

        if (rag.isExtensionAllowed(extensionSegmentSet)) {

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
                    throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
                }

                // look up the method on the extension that needs to be called.
                Method methodToCall = findExtensionMethod(rexsterExtension, ExtensionPoint.EDGE, extensionSegmentSet.getExtensionMethod());

                if (methodToCall == null) {
                    // extension method was not found for some reason
                    logger.error("The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "].  Check com.tinkerpop.rexster.extension.RexsterExtension file in META-INF.services.");
                    JSONObject error = generateErrorObject(
                            "The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "]");
                    throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
                }

                // found the method...time to do work
                returnValue = invokeExtension(graphName, rexsterExtension, methodToCall, edge);

            } catch (Exception ex) {
                logger.error(ex);
                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
            }

            if (returnValue instanceof ExtensionResponse) {
                extResponse = (ExtensionResponse) returnValue;

                if (extResponse.isErrorResponse()) {
                    // an error was raised within the extension.  pass it back out as an error.
                    logger.error("The [" + extensionSegmentSet + "] extension raised an error response.");
                    throw new WebApplicationException(this.addHeaders(Response.fromResponse(extResponse.getJerseyResponse())).build());
                }
            } else {
                // extension method is not returning the correct type...needs to be an ExtensionResponse
                logger.error("The [" + extensionSegmentSet + "] extension does not return an ExtensionResponse.");
                JSONObject error = generateErrorObject(
                        "The [" + extensionSegmentSet + "] extension does not return an ExtensionResponse.");
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
     * POST http://host/graph/edge?_inV=id1&_outV=id2&label=string&key=value
     * Edge e = graph.addEdge(null,graph.getVertex(id1),graph.getVertex(id2),label);
     * e.setProperty(key,value);
     */
    @POST
    public Response postNullEdge(@PathParam("graphname") String graphName) {
        return this.postEdge(graphName, null);
    }

    /**
     * POST http://host/graph/edge/id?_inV=id1&_outV=id2&label=string&key=value
     * Edge e = graph.addEdge(id,graph.getVertex(id1),graph.getVertex(id2),label);
     * e.setProperty(key,value);
     */
    @POST
    @Path("/{id}")
    public Response postEdge(@PathParam("graphname") String graphName, @PathParam("id") String id) {

        final Graph graph = this.getRexsterApplicationGraph(graphName).getGraph();
        String inV = null;
        Object temp = this.getRequestObject().opt(Tokens._IN_V);
        if (null != temp)
            inV = temp.toString();
        String outV = null;
        temp = this.getRequestObject().opt(Tokens._OUT_V);
        if (null != temp)
            outV = temp.toString();
        String label = null;
        temp = this.getRequestObject().opt(Tokens._LABEL);
        if (null != temp)
            label = temp.toString();

        Edge edge = graph.getEdge(id);
        if (null == edge && null != outV && null != inV && null != label) {
            // there is no edge but the in/out vertex params and label are present so
            // validate that the vertexes are present before creating the edge
            final Vertex out = graph.getVertex(outV);
            final Vertex in = graph.getVertex(inV);
            if (null != out && null != in) {
                // in/out vertexes are found so edge can be created
                edge = graph.addEdge(id, out, in, label);
            }

        } else if (edge != null) {
            if (!this.hasElementProperties(this.getRequestObject())) {
                JSONObject error = generateErrorObjectJsonFail(new Exception("Edge with id " + id + " already exists"));
                throw new WebApplicationException(this.addHeaders(Response.status(Status.CONFLICT).entity(error)).build());
            }
        }

        try {
            if (edge != null) {
                Iterator keys = this.getRequestObject().keys();
                while (keys.hasNext()) {
                    String key = keys.next().toString();
                    if (!key.startsWith(Tokens.UNDERSCORE)) {
                        edge.setProperty(key, this.getTypedPropertyValue(this.getRequestObject().getString(key)));
                    }
                }
                this.resultObject.put(Tokens.RESULTS, new ElementJSONObject(edge, this.getReturnKeys(), this.hasShowTypes()));
            } else {
                // edge could not be found.  likely an error condition on the request
                JSONObject error = generateErrorObjectJsonFail(new Exception("Edge cannot be found or created.  Please check the format of the request."));
                throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
            }

            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
        }

        return this.addHeaders(Response.ok(this.resultObject)).build();
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
    public Response deleteEdge(@PathParam("graphname") String graphName, @PathParam("id") String id) {

        try {
            final List<String> keys = this.getNonRexsterRequestKeys();
            final Graph graph = this.getRexsterApplicationGraph(graphName).getGraph();
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
                String msg = "Could not find edge [" + id + "] on graph [" + this.getRexsterApplicationGraph(graphName).getGraphName() + "]";
                logger.info(msg);
                JSONObject error = generateErrorObject(msg);
                throw new WebApplicationException(this.addHeaders(Response.status(Status.NOT_FOUND).entity(error)).build());
            }


            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException ex) {
            logger.error(ex);
            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
        }

        return this.addHeaders(Response.ok(this.resultObject)).build();

    }

    /*@DELETE
    TODO: WITHOUT CONCURRNT MODIFICATION ERRORS
    public Response deleteAllEdges() {

        final Graph graph = this.rag.getGraph();
        for (Edge edge : graph.getEdges()) {
            graph.removeEdge(edge);
        }

        try {
            this.resultObject.put(GremlinTokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();

    }*/
}

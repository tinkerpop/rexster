package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.*;
import com.tinkerpop.rexster.traversals.ElementJSONObject;
import com.tinkerpop.rexster.traversals.Traversal;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

            this.resultObject.put("name", "Rexster: A RESTful Graph Shell");
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

    @GET
    @Path("/traversal/{path: .+}")
    public Response getTraversal() {

        Class<? extends Traversal> traversalClass = null;
        String pattern = "";

        try {

            List<PathSegment> pathSegments = this.uriInfo.getPathSegments();

            // ignore the first two parts of the path as they are "graphname/traversal".
            // everything after that point represents the name of the traversal specified
            // by the getTraversalName() method on the Traversal interface
            for (int ix = 2; ix < pathSegments.size(); ix++) {
                pattern = pattern + "/" + pathSegments.get(ix).getPath();
            }

            // get the traversal class based on the pattern of the URI. strip the first
            // character of the pattern as the variable is initialized that way in the loop
            traversalClass = this.rag.getLoadedTraversals().get(pattern.substring(1));

            if (traversalClass != null) {
                Traversal traversal = (Traversal) traversalClass.newInstance();

                RexsterResourceContext ctx = new RexsterResourceContext();
                ctx.setRequest(this.request);
                ctx.setResultObject(this.resultObject);
                ctx.setUriInfo(this.uriInfo);
                ctx.setRexsterApplicationGraph(this.rag);
                ctx.setRequestObject(this.requestObject);

                traversal.evaluate(ctx);
            } else {
                logger.info("Request for a non-configured traversal [" + pattern + "]");

                JSONObject error = generateErrorObject("Graph [" + pattern + "] could not be found.");
                throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
            }

            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } catch (IllegalAccessException iae) {
            logger.error(iae);

            JSONObject error = generateErrorObject("Failed to instantiate the Traversal for [" + pattern + "].  No access to [" + traversalClass.getName() + "]", iae);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());

        } catch (InstantiationException ie) {
            logger.error(ie);

            JSONObject error = generateErrorObject("Failed to instantiate the Traversal for [" + pattern + "].  Expected a concrete class definition for [" + traversalClass.getName() + "]", ie);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    @GET
    @Path("/edges")
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
    @Path("/edges/{id}")
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
    @Path("/edges/{id}")
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
            if (!GraphResource.hasElementProperties(this.requestObject)) {
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
    @Path("/edges/{id}")
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

    @GET
    @Path("/vertices/{id}/{direction}")
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
                                edgeArray.put(new ElementJSONObject(edge, this.getReturnKeys()));
                            }
                            counter++;
                        }
                    }
                }
                if (direction.equals(Tokens.IN_E) || direction.equals(Tokens.BOTH_E)) {
                    for (Edge edge : vertex.getInEdges()) {
                        if (this.hasPropertyValues(edge, tempRequest)) {
                            if (counter >= start && counter < end) {
                                edgeArray.put(new ElementJSONObject(edge, this.getReturnKeys()));
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
    @Path("/vertices/{id}")
    public Response getSingleVertex(@PathParam("id") String id) {
        Vertex vertex = this.rag.getGraph().getVertex(id);
        if (null != vertex) {
            try {
                this.resultObject.put(Tokens.RESULTS, new ElementJSONObject(vertex, this.getReturnKeys()));
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
    @Path("/vertices")
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
                        vertexArray.put(new ElementJSONObject(element, this.getReturnKeys()));
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
    @Path("/vertices/{id}")
    public Response postVertex(@PathParam("id") String id) {

        try {
            Graph graph = this.rag.getGraph();
            Vertex vertex = graph.getVertex(id);
            if (null == vertex) {
                vertex = graph.addVertex(id);
            } else {
                if (!GraphResource.hasElementProperties(this.requestObject)) {
                    JSONObject error = generateErrorObjectJsonFail(new Exception("Vertex with id " + id + " already exists"));
                    throw new WebApplicationException(Response.status(Status.CONFLICT).entity(error).build());
                }
            }

            Iterator keys = this.requestObject.keys();
            while (keys.hasNext()) {
                String key = keys.next().toString();
                if (!key.startsWith(Tokens.UNDERSCORE)) {
                    vertex.setProperty(key, this.requestObject.get(key));
                }
            }

            this.resultObject.put(Tokens.RESULTS, new ElementJSONObject(vertex, this.getReturnKeys()));
            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    @DELETE
    @Path("/vertices/{id}")
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

    private static boolean hasElementProperties(JSONObject requestObject) {
        Iterator keys = requestObject.keys();
        while (keys.hasNext()) {
            String key = keys.next().toString();
            if (!key.startsWith(Tokens.UNDERSCORE)) {
                return true;
            }
        }
        return false;
    }
}

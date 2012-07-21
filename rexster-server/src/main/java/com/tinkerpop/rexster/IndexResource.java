package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.IndexableGraph;
import com.tinkerpop.blueprints.Parameter;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONUtility;
import com.tinkerpop.rexster.extension.HttpMethod;
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
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resource for graph indices.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@Path("/graphs/{graphname}/indices")
public class IndexResource extends AbstractSubResource {

    private static final Logger logger = Logger.getLogger(EdgeResource.class);

    public IndexResource() {
        super(null);
    }

    public IndexResource(final UriInfo ui, final HttpServletRequest req, final RexsterApplication ra) {
        super(ra);
        this.httpServletRequest = req;
        this.uriInfo = ui;
    }

    @OPTIONS
    public Response optionsAllIndices() {
        return buildOptionsResponse(HttpMethod.GET.toString());
    }

    /**
     * GET http://host/graph/indices
     * get.getIndices();
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    public Response getAllIndices(@PathParam("graphname") final String graphName) {
        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
        final Graph graph = rag.getGraph();

        final JSONObject theRequestObject = this.getRequestObject();

        final IndexableGraph idxGraph = graph instanceof IndexableGraph ? (IndexableGraph) graph : null;

        if (idxGraph == null) {
            final JSONObject error = this.generateErrorObject("The requested graph is not of type " + IndexableGraph.class.getName() + ".");
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        final Long start = RequestObjectHelper.getStartOffset(theRequestObject);
        final Long end = RequestObjectHelper.getEndOffset(theRequestObject);

        long counter = 0l;

        try {
            final JSONArray indexArray = new JSONArray();
            for (Index index : idxGraph.getIndices()) {
                if (counter >= start && counter < end) {
                    indexArray.put(createJSONObject(index));
                }
                counter++;
            }

            this.resultObject.put(Tokens.RESULTS, indexArray);
            this.resultObject.put(Tokens.TOTAL_SIZE, counter);
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

        } catch (JSONException ex) {
            logger.error(ex);

            final JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();

    }

    @OPTIONS
    @Path("/{indexName}")
    public Response optionsElementsFromIndex() {
        return buildOptionsResponse(HttpMethod.GET.toString(),
                HttpMethod.DELETE.toString(),
                HttpMethod.POST.toString(),
                HttpMethod.PUT.toString());
    }

    /**
     * GET http://host/graph/indices/indexName?key=key1&value=value1
     * Index index = graph.getIndex(indexName,...);
     * index.get(key,value);
     */
    @GET
    @Path("/{indexName}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON})
    public Response getElementsFromIndex(@PathParam("graphname") final String graphName, @PathParam("indexName") final String indexName) {
        return this.getElementsFromIndex(graphName, indexName, false);
    }

    @GET
    @Path("/{indexName}")
    @Produces({RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    public Response getElementsFromIndexRexsterTypedJson(@PathParam("graphname") final String graphName, @PathParam("indexName") final String indexName) {
        return this.getElementsFromIndex(graphName, indexName, true);
    }

    private Response getElementsFromIndex(final String graphName, final String indexName, final boolean showTypes) {
        final Index index = this.getIndexFromGraph(graphName, indexName);

        String key = null;
        Object value = null;

        final JSONObject theRequestObject = this.getRequestObject();

        Object temp = theRequestObject.opt(Tokens.KEY);
        if (null != temp)
            key = temp.toString();

        temp = theRequestObject.opt(Tokens.VALUE);
        if (null != temp)
            value = ElementHelper.getTypedPropertyValue(temp.toString());

        final Long start = RequestObjectHelper.getStartOffset(theRequestObject);
        final Long end = RequestObjectHelper.getEndOffset(theRequestObject);
        final List<String> returnKeys = RequestObjectHelper.getReturnKeys(theRequestObject);

        long counter = 0l;

        if (null != index && key != null && value != null) {
            try {

                final JSONArray elementArray = new JSONArray();
                for (Element element : (Iterable<Element>) index.get(key, value)) {
                    if (counter >= start && counter < end) {
                        elementArray.put(GraphSONUtility.jsonFromElement(element, returnKeys, showTypes));
                    }
                    counter++;
                }

                this.resultObject.put(Tokens.RESULTS, elementArray);
                this.resultObject.put(Tokens.TOTAL_SIZE, counter);
                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

            } catch (JSONException ex) {
                logger.error(ex);

                final JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
        } else if (null == index) {
            final String msg = "Could not find index [" + indexName + "] on graph [" + graphName + "]";
            logger.info(msg);

            final JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(error).build());
        } else {
            // return info about the index itself
            final HashMap map = new HashMap();
            map.put(Tokens.QUERY_TIME, this.sh.stopWatch());
            map.put(Tokens.RESULTS, createJSONObject(index));

            this.resultObject = new JSONObject(map);
        }

        return Response.ok(this.resultObject).build();
    }

    @OPTIONS
    @Path("/{indexName}/count")
    public Response optionsIndexCount() {
        return buildOptionsResponse(HttpMethod.GET.toString());
    }

    /**
     * GET http://host/graph/indices/indexName/count?key=?&value=?
     */
    @GET
    @Path("/{indexName}/count")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    public Response getIndexCount(@PathParam("graphname") final String graphName, @PathParam("indexName") final String indexName) {
        final Index index = this.getIndexFromGraph(graphName, indexName);

        String key = null;
        Object value = null;

        final JSONObject theRequestObject = this.getRequestObject();

        Object temp = theRequestObject.opt(Tokens.KEY);
        if (temp != null) {
            key = temp.toString();
        }

        temp = theRequestObject.opt(Tokens.VALUE);
        if (temp != null) {
            value = ElementHelper.getTypedPropertyValue(temp.toString());
        }

        if (index != null && key != null && value != null) {
            try {
                final long count = index.count(key, value);

                this.resultObject.put(Tokens.TOTAL_SIZE, count);
                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

            } catch (JSONException ex) {
                logger.error(ex);

                final JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
        } else if (null == index) {
            final String msg = "Could not find index [" + indexName + "] on graph [" + graphName + "]";
            logger.info(msg);

            final JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(error).build());
        } else {
            final String msg = "A key and value must be provided to lookup elements in an index";
            logger.info(msg);

            final JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    @DELETE
    @Path("/{indexName}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Consumes({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    public Response deleteIndex(@PathParam("graphname") final String graphName, @PathParam("indexName") final String indexName, final JSONObject json) {
        // initializes the request object with the data DELETEed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.setRequestObject(json);
        return this.deleteIndex(graphName, indexName);
    }

    /**
     * DELETE http://host/graph/indices/indexName
     * graph.dropIndex(indexName);
     * <p/>
     * DELETE http://host/graph/indices/indexName?key=key1&value=value1&id=id1
     * Index index = graph.getIndex(indexName,...)
     * index.remove(key, value, graph.getVertex(id1));
     */
    @DELETE
    @Path("/{indexName}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    public Response deleteIndex(@PathParam("graphname") final String graphName, @PathParam("indexName") final String indexName) {
        String key = null;
        Object value = null;
        String id = null;

        final JSONObject theRequestObject = this.getRequestObject();

        Object temp = theRequestObject.opt(Tokens.KEY);
        if (null != temp)
            key = temp.toString();
        temp = theRequestObject.opt(Tokens.VALUE);
        if (null != temp)
            value = ElementHelper.getTypedPropertyValue(temp.toString());
        temp = theRequestObject.opt(Tokens.ID);
        if (null != temp)
            id = temp.toString();

        final Index index = this.getIndexFromGraph(graphName, indexName);
        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
        final IndexableGraph graph = (IndexableGraph) rag.getGraph();

        if (null == index) {
            final String msg = "Could not find index [" + indexName + "] on graph [" + graphName + "]";
            logger.info(msg);

            final JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(error).build());
        }

        if (key == null && value == null && id == null) {
            try {
                graph.dropIndex(indexName);
                rag.tryStopTransactionSuccess();
            } catch (Exception ex) {
                logger.error(ex);

                rag.tryStopTransactionFailure();

                final JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
        } else if (null != index & key != null && value != null && id != null) {
            try {
                if (index.getIndexClass().equals(Vertex.class))
                    index.remove(key, value, graph.getVertex(id));
                else
                    index.remove(key, value, graph.getEdge(id));

                rag.tryStopTransactionSuccess();
                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

            } catch (JSONException ex) {
                logger.error(ex);

                rag.tryStopTransactionFailure();

                final JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
        } else {
            final String msg = "A key, value, id, and type (vertex/edge) must be provided to lookup elements in an index";
            logger.info(msg);

            final JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
        }

        return Response.ok(this.resultObject).build();

    }

    /**
     * POST http://host/graph/indices/indexName?key=key1&value=value1&class=vertex&id=id1
     * Index index = graph.getIndex(indexName,...);
     * index.put(key,value,graph.getVertex(id1));
     * <p/>
     * POST http://host/graph/indices/indexName?class=vertex&type=automatic&keys=[name,age]
     * graph.createIndex(indexName,Vertex.class,AUTOMATIC, {name, age})
     */
    @POST
    @Path("/{indexName}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Consumes({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    public Response postIndex(@PathParam("graphname") final String graphName, @PathParam("indexName") final String indexName, final JSONObject json) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.setRequestObject(json);
        return this.postIndex(graphName, indexName);
    }

    @POST
    @Path("/{indexName}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    public Response postIndex(@PathParam("graphname") final String graphName, @PathParam("indexName") final String indexName) {
        String clazz = null;
        Set<String> keys = null;
        Parameter<Object, Object>[] indexParameters = new Parameter[0];

        final JSONObject theRequestObject = this.getRequestObject();

        Object temp = theRequestObject.opt(Tokens.CLASS);
        if (temp != null)
            clazz = temp.toString();
        temp = theRequestObject.opt(Tokens.KEYS);
        if (temp != null) {
            try {
                final JSONArray ks;
                if (temp instanceof String) {
                    ks = new JSONArray();
                    ks.put(temp);
                } else {
                    ks = (JSONArray) temp;
                }

                keys = new HashSet<String>();
                for (int i = 0; i < ks.length(); i++) {
                    keys.add(ks.getString(i));
                }
            } catch (Exception e) {
                final JSONObject error = generateErrorObject("Index keys must be in an array: " + temp);
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
            }
        }

        temp = theRequestObject.opt("params");
        if (temp != null) {
            final JSONObject idxParamsJson = (JSONObject) temp;
            final ArrayList<Parameter<Object, Object>> idxParamsList = new ArrayList<Parameter<Object, Object>>();

            final Iterator idxParamKeys = idxParamsJson.keys();
            while (idxParamKeys.hasNext()) {
                final String nextIdxParamKey = (String) idxParamKeys.next();
                idxParamsList.add(new Parameter<Object, Object>(nextIdxParamKey, idxParamsJson.optString(nextIdxParamKey)));
            }

            indexParameters = new Parameter[idxParamsList.size()];
            idxParamsList.toArray(indexParameters);
        }

        final Index index = this.getIndexFromGraph(graphName, indexName);
        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
        final IndexableGraph graph = (IndexableGraph) rag.getGraph();

        if (null != index) {
            final String msg = "Index [" + indexName + "] on graph [" + graphName + "] already exists";
            logger.info(msg);

            final JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } else {
            // create an index
            if (null != clazz) {
                final Class c;
                if (clazz.equals(Tokens.VERTEX))
                    c = Vertex.class;
                else if (clazz.equals(Tokens.EDGE))
                    c = Edge.class;
                else {
                    final JSONObject error = generateErrorObject("Index class must be either vertex or edge");
                    throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
                }

                final Index newIndex;
                try {
                    newIndex = graph.createIndex(indexName, c, indexParameters);
                    rag.tryStopTransactionSuccess();
                } catch (Exception e) {
                    logger.info(e.getMessage());

                    rag.tryStopTransactionFailure();

                    final JSONObject error = generateErrorObject(e.getMessage());
                    throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
                }
                try {
                    this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());
                    this.resultObject.put(Tokens.RESULTS, createJSONObject(newIndex));
                } catch (JSONException ex) {
                    logger.error(ex);

                    final JSONObject error = generateErrorObjectJsonFail(ex);
                    throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
                }


            } else {
                final String msg = "Class (vertex/edge) must be provided to create a new index";
                logger.info(msg);

                final JSONObject error = generateErrorObject(msg);
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
            }
        }

        return Response.ok(this.resultObject).build();
    }

    /**
     * PUT http://host/graph/indices/indexName?key=key1&value=value1&class=vertex&id=id1
     */
    @PUT
    @Path("/{indexName}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Consumes({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    public Response putElementInIndex(@PathParam("graphname") final String graphName, @PathParam("indexName") final String indexName, final JSONObject json) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.setRequestObject(json);
        return this.putElementInIndex(graphName, indexName);
    }

    /**
     * PUT http://host/graph/indices/indexName?key=key1&value=value1&class=vertex&id=id1
     */
    @PUT
    @Path("/{indexName}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    public Response putElementInIndex(@PathParam("graphname") final String graphName, @PathParam("indexName") final String indexName) {
        final Index index = this.getIndexFromGraph(graphName, indexName);
        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
        final IndexableGraph graph = (IndexableGraph) rag.getGraph();

        if (index == null) {
            JSONObject error = generateErrorObject("Index not found.");
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(error).build());
        }

        String key = null;
        Object value = null;
        String id = null;

        final JSONObject theRequestObject = this.getRequestObject();

        Object temp = theRequestObject.opt(Tokens.KEY);
        if (null != temp)
            key = temp.toString();
        temp = theRequestObject.opt(Tokens.VALUE);
        if (null != temp)
            value = ElementHelper.getTypedPropertyValue(temp.toString());
        temp = theRequestObject.opt(Tokens.ID);
        if (null != temp)
            id = temp.toString();

        if (key != null && value != null && id != null) {
            try {
                if (index.getIndexClass().equals(Vertex.class)) {
                    index.put(key, value, graph.getVertex(id));
                    rag.tryStopTransactionSuccess();
                } else if (index.getIndexClass().equals(Edge.class)) {
                    index.put(key, value, graph.getEdge(id));
                    rag.tryStopTransactionSuccess();
                } else {
                    rag.tryStopTransactionFailure();
                    final JSONObject error = generateErrorObject("Index class must be either vertex or edge");
                    throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
                }

                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

            } catch (JSONException ex) {
                logger.error(ex);

                final JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
        } else {
            final String msg = "A key, value, and id must be provided to add elements to an index";
            logger.info(msg);

            final JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    private static JSONObject createJSONObject(final Index index) {
        final Map<String, String> mapIndex = new HashMap<String, String>();
        mapIndex.put("name", index.getIndexName());
        if (Vertex.class.isAssignableFrom(index.getIndexClass())) {
            mapIndex.put("class", "vertex");
        } else if (Edge.class.isAssignableFrom(index.getIndexClass())) {
            mapIndex.put("class", "edge");
        }

        return new JSONObject(mapIndex);
    }

    private Index getIndexFromGraph(final String graphName, final String name) {

        final Graph graph = this.getRexsterApplicationGraph(graphName).getGraph();
        final IndexableGraph idxGraph = graph instanceof IndexableGraph ? (IndexableGraph) graph : null;

        if (idxGraph == null) {
            final JSONObject error = this.generateErrorObject("The requested graph is not of type IndexableGraph.");
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        final Iterable<Index<? extends Element>> indices = idxGraph.getIndices();
        for (final Index index : indices) {
            if (index.getIndexName().equals(name)) {
                return index;
            }
        }

        return null;
    }

}
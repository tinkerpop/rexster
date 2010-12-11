package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.AutomaticIndex;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Index;
import com.tinkerpop.blueprints.pgm.IndexableGraph;
import com.tinkerpop.rexster.traversals.ElementJSONObject;
import com.tinkerpop.rexster.traversals.IndexJSONObject;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
@Path("/{graphname}/indices")
@Produces(MediaType.APPLICATION_JSON)
public class IndexResource extends AbstractSubResource {

    private static Logger logger = Logger.getLogger(EdgeResource.class);

    public IndexResource(@PathParam("graphname") String graphName, @Context UriInfo ui, @Context HttpServletRequest req) {
        super(graphName, ui, req);
    }

    @GET
    public Response getAllIndices() {

        Long start = this.getStartOffset();
        if (null == start)
            start = 0l;
        Long end = this.getEndOffset();
        if (null == end)
            end = Long.MAX_VALUE;

        long counter = 0l;

        try {
            JSONArray indexArray = new JSONArray();
            for (Index index : ((IndexableGraph) this.rag.getGraph()).getIndices()) {
                if (counter >= start && counter < end) {
                    indexArray.put(new IndexJSONObject(index));
                }
                counter++;
            }

            this.resultObject.put(Tokens.RESULTS, indexArray);
            this.resultObject.put(Tokens.TOTAL_SIZE, counter);
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

        } catch (JSONException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();

    }

    @GET
    @Path("/{indexName}")
    public Response getElementsFromIndex(@PathParam("indexName") String indexName) {
        Index index = this.getIndexFromGraph(indexName);

        String key = null;
        Object value = null;

        Object temp = this.requestObject.opt(Tokens.KEY);
        if (null != temp)
            key = temp.toString();

        temp = this.requestObject.opt(Tokens.VALUE);
        if (null != temp)
            value = getTypedPropertyValue(temp.toString());


        Long start = this.getStartOffset();
        if (null == start)
            start = 0l;
        Long end = this.getEndOffset();
        if (null == end)
            end = Long.MAX_VALUE;

        long counter = 0l;


        if (null != index & key != null && value != null) {
            try {
                JSONArray elementArray = new JSONArray();
                for (Element element : (Iterable<Element>) index.get(key, value)) {
                    if (counter >= start && counter < end) {
                        elementArray.put(new ElementJSONObject(element, this.getReturnKeys(), this.hasShowTypes()));
                    }
                    counter++;
                }

                this.resultObject.put(Tokens.RESULTS, elementArray);
                this.resultObject.put(Tokens.TOTAL_SIZE, counter);
                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());


            } catch (JSONException ex) {
                logger.error(ex);

                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
        } else if (null == index) {
            String msg = "Could not find index [" + indexName + "] on graph [" + this.rag.getGraphName() + "]";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(error).build());
        } else {
            String msg = "A key and value must be provided to lookup elements in an index";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    @GET
    @Path("/{indexName}/keys")
    public Response getAutomaticIndexKeys(@PathParam("indexName") String indexName) {
        Index index = this.getIndexFromGraph(indexName);

        if (null != index && index.getIndexType().equals(Index.Type.AUTOMATIC)) {
            try {
                JSONArray keyArray = new JSONArray();
                if (null == ((AutomaticIndex) index).getAutoIndexKeys()) {
                    keyArray.put((String) null);
                } else {
                    for (String key : ((Set<String>) ((AutomaticIndex) index).getAutoIndexKeys())) {
                        keyArray.put(key);
                    }
                }
                this.resultObject.put(Tokens.RESULTS, keyArray);
                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());
            } catch (JSONException ex) {
                logger.error(ex);

                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
        } else if (null == index) {
            String msg = "Could not find index [" + indexName + "] on graph [" + this.rag.getGraphName() + "]";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(error).build());
        } else {
            String msg = "A key and value must be provided to lookup elements in an index";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }


    @DELETE
    @Path("/{indexName}")
    public Response deleteIndex(@PathParam("indexName") String indexName) {
        Index index = this.getIndexFromGraph(indexName);
        IndexableGraph graph = (IndexableGraph) this.rag.getGraph();

        String key = null;
        Object value = null;
        String id = null;
        String type = null;

        Object temp = this.requestObject.opt(Tokens.KEY);
        if (null != temp)
            key = temp.toString();
        temp = this.requestObject.opt(Tokens.VALUE);
        if (null != temp)
            value = getTypedPropertyValue(temp.toString());
        temp = this.requestObject.opt(Tokens.ID);
        if (null != temp)
            id = temp.toString();
        temp = this.requestObject.opt(Tokens.TYPE);
        if (null != temp)
            type = temp.toString();

        if (key == null && value == null && id == null && type == null)
            graph.dropIndex(indexName);
        else if (null != index & key != null && value != null && type != null && id != null) {
            try {
                if (type.equals("vertex"))
                    index.remove(key, value, graph.getVertex(id));
                else
                    index.remove(key, value, graph.getEdge(id));

                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());


            } catch (JSONException ex) {
                logger.error(ex);

                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
        } else if (null == index) {
            String msg = "Could not find index [" + indexName + "] on graph [" + this.rag.getGraphName() + "]";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(error).build());
        } else {
            String msg = "A key, value, id, and type (vertex/edge) must be provided to lookup elements in an index";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
        }

        return Response.ok(this.resultObject).build();

    }

    @POST
    @Path("/{indexName}")
    public Response putElementInIndex(@PathParam("indexName") String indexName) {
        Index index = this.getIndexFromGraph(indexName);
        IndexableGraph graph = (IndexableGraph) this.rag.getGraph();

        String key = null;
        Object value = null;
        String id = null;
        String type = null;

        Object temp = this.requestObject.opt(Tokens.KEY);
        if (null != temp)
            key = temp.toString();
        temp = this.requestObject.opt(Tokens.VALUE);
        if (null != temp)
            value = getTypedPropertyValue(temp.toString());
        temp = this.requestObject.opt(Tokens.ID);
        if (null != temp)
            id = temp.toString();
        temp = this.requestObject.opt(Tokens.TYPE);
        if (null != temp)
            type = temp.toString();


        if (null != index & key != null && value != null && type != null && id != null) {
            try {
                if (type.equals("vertex"))
                    index.put(key, value, graph.getVertex(id));
                else
                    index.put(key, value, graph.getEdge(id));

                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());


            } catch (JSONException ex) {
                logger.error(ex);

                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
        } else if (null == index) {
            String msg = "Could not find index [" + indexName + "] on graph [" + this.rag.getGraphName() + "]";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(error).build());
        } else {
            String msg = "A key, value, id, and type (vertex/edge) must be provided to lookup elements in an index";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    private Index getIndexFromGraph(String name) {
        IndexableGraph graph = (IndexableGraph) this.rag.getGraph();
        for (Index index : graph.getIndices()) {
            if (index.getIndexName().equals(name))
                return index;
        }
        return null;
    }

}
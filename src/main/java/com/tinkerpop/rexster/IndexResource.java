package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.AutomaticIndex;
import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Index;
import com.tinkerpop.blueprints.pgm.IndexableGraph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.rexster.traversals.ElementJSONObject;
import com.tinkerpop.rexster.traversals.IndexJSONObject;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
@Path("/{graphname}/indices")
@Produces(MediaType.APPLICATION_JSON)
public class IndexResource extends AbstractSubResource {

    private static Logger logger = Logger.getLogger(EdgeResource.class);

    public IndexResource(@PathParam("graphname") String graphName, @Context UriInfo ui, @Context HttpServletRequest req) {
        super(graphName, ui, req, null);
    }
    
    public IndexResource(String graphName, UriInfo ui, HttpServletRequest req, RexsterApplicationProvider rap) {
        super(graphName, ui, req, rap);
    }

    /**
     * GET http://host/graph/indices
     * get.getIndices();
     */
    @GET
    public Response getAllIndices() {
    	IndexableGraph idxGraph = null;
    	if (this.rag.getGraph() instanceof IndexableGraph) {
    		idxGraph = (IndexableGraph) this.rag.getGraph();
    	}
    	
    	if (idxGraph == null) {
    		JSONObject error = this.generateErrorObject("The requested graph is not of type IndexableGraph.");
            throw new WebApplicationException(
            		this.addHeaders(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error)).build());
    	}
    	
        Long start = this.getStartOffset();
        Long end = this.getEndOffset();

        long counter = 0l;

        try {
            JSONArray indexArray = new JSONArray();
            for (Index index : idxGraph.getIndices()) {
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
            throw new WebApplicationException(
            		this.addHeaders(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error)).build());
        }

        return this.addHeaders(Response.ok(this.resultObject)).build();

    }

    /**
     * GET http://host/graph/indices/indexName?key=key1&value=value1
     * Index index = graph.getIndex(indexName,...);
     * index.get(key,value);
     */
    @GET
    @Path("/{indexName}")
    public Response getElementsFromIndex(@PathParam("indexName") String indexName) {
        final Index index = this.getIndexFromGraph(indexName);

        String key = null;
        Object value = null;

        Object temp = this.requestObject.opt(Tokens.KEY);
        if (null != temp)
            key = temp.toString();

        temp = this.requestObject.opt(Tokens.VALUE);
        if (null != temp)
            value = getTypedPropertyValue(temp.toString());


        Long start = this.getStartOffset();
        Long end = this.getEndOffset();

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
                throw new WebApplicationException(
                		this.addHeaders(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error)).build());
            }
        } else if (null == index) {
            String msg = "Could not find index [" + indexName + "] on graph [" + this.rag.getGraphName() + "]";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(
            		this.addHeaders(Response.status(Response.Status.NOT_FOUND).entity(error)).build());
        } else {
            String msg = "A key and value must be provided to lookup elements in an index";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(
            		this.addHeaders(Response.status(Response.Status.BAD_REQUEST).entity(error)).build());
        }

        return this.addHeaders(Response.ok(this.resultObject)).build();
    }

    /**
     * GET http://host/graph/indices/indexName/keys
     * AutomaticIndex index = (AutomaticIndex) graph.getIndex(indexName,...);
     * index.getAutoIndexKeys();
     */
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
                throw new WebApplicationException(
                		this.addHeaders(Response.status(Response.Status.INTERNAL_SERVER_ERROR)).entity(error).build());
            }
        } else if (null == index) {
            String msg = "Could not find index [" + indexName + "] on graph [" + this.rag.getGraphName() + "]";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(
            		this.addHeaders(Response.status(Response.Status.NOT_FOUND).entity(error)).build());
        } else {
            String msg = "Only automatic indices have user provided keys";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(
            		this.addHeaders(Response.status(Response.Status.BAD_REQUEST).entity(error)).build());
        }

        return this.addHeaders(Response.ok(this.resultObject)).build();
    }

    /**
     * DELETE http://host/graph/indices/indexName
     * graph.dropIndex(indexName);
     * <p/>
     * DELETE http://host/graph/indices/indexName?key=key1&value=value1&class=vertex&id=id1
     * Index index = graph.getIndex(indexName,...)
     * index.remove(key, value, graph.getVertex(id1));
     */
    @DELETE
    @Path("/{indexName}")
    public Response deleteIndex(@PathParam("indexName") String indexName) {
        final Index index = this.getIndexFromGraph(indexName);
        final IndexableGraph graph = (IndexableGraph) this.rag.getGraph();

        String key = null;
        Object value = null;
        String id = null;
        String clazz = null;

        Object temp = this.requestObject.opt(Tokens.KEY);
        if (null != temp)
            key = temp.toString();
        temp = this.requestObject.opt(Tokens.VALUE);
        if (null != temp)
            value = getTypedPropertyValue(temp.toString());
        temp = this.requestObject.opt(Tokens.ID);
        if (null != temp)
            id = temp.toString();
        temp = this.requestObject.opt(Tokens.CLASS);
        if (null != temp)
            clazz = temp.toString();

        if (key == null && value == null && id == null && clazz == null) {
            graph.dropIndex(indexName);
        } else if (null != index & key != null && value != null && clazz != null && id != null) {
            try {
                if (clazz.equals(Tokens.VERTEX))
                    index.remove(key, value, graph.getVertex(id));
                else
                    index.remove(key, value, graph.getEdge(id));

                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());


            } catch (JSONException ex) {
                logger.error(ex);

                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(
                		this.addHeaders(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error)).build());
            }
        } else if (null == index) {
            String msg = "Could not find index [" + indexName + "] on graph [" + this.rag.getGraphName() + "]";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(
            		this.addHeaders(Response.status(Response.Status.NOT_FOUND).entity(error)).build());
        } else {
            String msg = "A key, value, id, and type (vertex/edge) must be provided to lookup elements in an index";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(
            		this.addHeaders(Response.status(Response.Status.BAD_REQUEST).entity(error)).build());
        }

        return this.addHeaders(Response.ok(this.resultObject)).build();

    }

    /**
     * DELETE http://host/graph/indices/indexName/keys?key1&key2
     * AutomaticIndex index = (AutomaticIndex) graph.getIndex(indexName,...);
     * index.removeAutoIndexKey(key1);
     * index.removeAutoIndexKey(key2);
     */
    @DELETE
    @Path("/{indexName}/keys")
    public Response deleteIndexKeys(@PathParam("indexName") String indexName) {

        try {
            final Index index = this.getIndexFromGraph(indexName);
            final List<String> keys = this.getNonRexsterRequestKeys();

            if (null != index && index.getIndexType().equals(Index.Type.AUTOMATIC)) {
                AutomaticIndex autoIndex = (AutomaticIndex) index;
                for (String key : keys) {
                    autoIndex.removeAutoIndexKey(key);
                }
            } else if (null == index) {
                String msg = "Could not find index [" + indexName + "] on graph [" + this.rag.getGraphName() + "]";
                logger.info(msg);

                JSONObject error = generateErrorObject(msg);
                throw new WebApplicationException(
                	this.addHeaders(Response.status(Response.Status.NOT_FOUND).entity(error)).build());
            } else {
                String msg = "Only automatic indices have user provided keys";
                logger.info(msg);

                JSONObject error = generateErrorObject(msg);
                throw new WebApplicationException(
                		this.addHeaders(Response.status(Response.Status.BAD_REQUEST).entity(error)).build());
            }
        } catch (JSONException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(
            		this.addHeaders(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error)).build());
        }

        return this.addHeaders(Response.ok(this.resultObject)).build();

    }

    /**
     * POST http://host/graph/indices/indexName?key=key1&value=value1&class=vertex&id=id1
     * Index index = graph.getIndex(indexName,...);
     * index.put(key,value,graph.getVertex(id1));
     * <p/>
     * POST http://host/graph/indices/indexName?class=vertex&type=automatic
     * graph.createIndex(indexName,Vertex.class,AUTOMATIC)
     */
    @POST
    @Path("/{indexName}")
    public Response putElementInIndexOrCreateIndex(@PathParam("indexName") String indexName) {
        final Index index = this.getIndexFromGraph(indexName);
        final IndexableGraph graph = (IndexableGraph) this.rag.getGraph();

        String key = null;
        Object value = null;
        String id = null;
        String clazz = null;
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
        temp = this.requestObject.opt(Tokens.CLASS);
        if (null != temp)
            clazz = temp.toString();
        temp = this.requestObject.opt(Tokens.TYPE);
        if (null != temp)
            type = temp.toString();


        if (null != index & key != null && value != null && clazz != null && id != null) {
            try {
                if (clazz.equals(Tokens.VERTEX))
                    index.put(key, value, graph.getVertex(id));
                else if (clazz.equals(Tokens.EDGE))
                    index.put(key, value, graph.getEdge(id));
                else {
                    JSONObject error = generateErrorObject("Index class must be either vertex or edge");
                    throw new WebApplicationException(
                    		this.addHeaders(Response.status(Response.Status.BAD_REQUEST).entity(error)).build());
                }


                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());


            } catch (JSONException ex) {
                logger.error(ex);

                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(
                		this.addHeaders(Response.status(Response.Status.INTERNAL_SERVER_ERROR)).entity(error).build());
            }
        } else if (null != index && null != type && null != clazz) {
            String msg = "Index [" + indexName + "] on graph [" + this.rag.getGraphName() + "] already exists";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(
            		this.addHeaders(Response.status(Response.Status.NOT_FOUND).entity(error)).build());
        } else if (null == index) {


            if (null != type && null != clazz) {
                Index.Type t;
                Class c;
                if (type.equals(Index.Type.AUTOMATIC.toString().toLowerCase()))
                    t = Index.Type.AUTOMATIC;
                else if (type.equals(Index.Type.MANUAL.toString().toLowerCase()))
                    t = Index.Type.MANUAL;
                else {
                    JSONObject error = generateErrorObject("Index type must be either automatic or manual");
                    throw new WebApplicationException(
                    		this.addHeaders(Response.status(Response.Status.BAD_REQUEST).entity(error)).build());
                }

                if (clazz.equals(Tokens.VERTEX))
                    c = Vertex.class;
                else if (clazz.equals(Tokens.EDGE))
                    c = Edge.class;
                else {
                    JSONObject error = generateErrorObject("Index class must be either vertex or edge");
                    throw new WebApplicationException(
                    		this.addHeaders(Response.status(Response.Status.BAD_REQUEST).entity(error)).build());
                }

                Index i;
                try {
                    i = graph.createIndex(indexName, c, t);
                } catch (Exception e) {
                    logger.info(e.getMessage());
                    JSONObject error = generateErrorObject(e.getMessage());
                    throw new WebApplicationException(
                    		this.addHeaders(Response.status(Response.Status.BAD_REQUEST).entity(error)).build());
                }
                try {
                    this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());
                    this.resultObject.put(Tokens.RESULTS, new IndexJSONObject(i));
                } catch (JSONException ex) {
                    logger.error(ex);

                    JSONObject error = generateErrorObjectJsonFail(ex);
                    throw new WebApplicationException(
                    		this.addHeaders(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error)).build());
                }


            } else {
                String msg = "Could not find index [" + indexName + "] on graph [" + this.rag.getGraphName() + "]";
                logger.info(msg);

                JSONObject error = generateErrorObject(msg);
                throw new WebApplicationException(
                		this.addHeaders(Response.status(Response.Status.NOT_FOUND).entity(error)).build());
            }
        } else {
            String msg = "A key, value, id, and type (vertex/edge) must be provided to add elements to an index";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(
            		this.addHeaders(Response.status(Response.Status.BAD_REQUEST).entity(error)).build());
        }

        return this.addHeaders(Response.ok(this.resultObject)).build();
    }

    /**
     * POST http://host/graph/indices/indexName?key1&key2
     * AutomaticIndex index = (AutomaticIndex) graph.getIndex(indexName,...);
     * index.addAutoIndexKey(key1);
     * index.addAutoIndexKey(key2);
     * --if any key is 'null' then index.addAutoIndexKey(null) (for wildcard)
     */
    @POST
    @Path("/{indexName}/keys")
    public Response addIndexKeys(@PathParam("indexName") String indexName) {
        try {
            final Index index = this.getIndexFromGraph(indexName);
            final List<String> keys = this.getNonRexsterRequestKeys();

            if (null != index && index.getIndexType().equals(Index.Type.AUTOMATIC)) {
                AutomaticIndex autoIndex = (AutomaticIndex) index;
                for (String key : keys) {
                    if (key.equals("null"))
                        autoIndex.addAutoIndexKey(null);
                    else
                        autoIndex.addAutoIndexKey(key);
                }
            } else if (null == index) {
                String msg = "Could not find index [" + indexName + "] on graph [" + this.rag.getGraphName() + "]";
                logger.info(msg);

                JSONObject error = generateErrorObject(msg);
                throw new WebApplicationException(
                		this.addHeaders(Response.status(Response.Status.NOT_FOUND).entity(error)).build());
            } else {
                String msg = "Only automatic indices have user provided keys";
                logger.info(msg);

                JSONObject error = generateErrorObject(msg);
                throw new WebApplicationException(
                		this.addHeaders(Response.status(Response.Status.BAD_REQUEST).entity(error)).build());
            }
        } catch (JSONException ex) {
            logger.error(ex);
            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(
            		this.addHeaders(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error)).build());
        }

        return this.addHeaders(Response.ok(this.resultObject)).build();
    }

    private Index getIndexFromGraph(final String name) {
    	
    	IndexableGraph idxGraph = null;
    	if (this.rag.getGraph() instanceof IndexableGraph) {
    		idxGraph = (IndexableGraph) this.rag.getGraph();
    	}
    	
    	if (idxGraph == null) {
    		JSONObject error = this.generateErrorObject("The requested graph is not of type IndexableGraph.");
            throw new WebApplicationException(
            		this.addHeaders(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error)).build());
    	}
        

        for (final Index index : idxGraph.getIndices()) {
            if (index.getIndexName().equals(name))
                return index;
        }
        
        return null;
    }

}
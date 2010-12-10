package com.tinkerpop.rexster;

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
    @Path("/{name}")
    public Response getSingleEdge(@PathParam("name") String name) {
        Index index = null;
        for (Index i : ((IndexableGraph) this.rag.getGraph()).getIndices()) {
            if (i.getIndexName().equals(name))
                index = i;
        }

        String key = null;
        Object temp = this.requestObject.opt(Tokens.KEY);
        if (null != temp)
            key = temp.toString();
        String value = null;
        temp = this.requestObject.opt(Tokens.VALUE);
        if (null != temp)
            value = temp.toString();


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
                        elementArray.put(new ElementJSONObject(element));
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
            String msg = "Could not find index [" + name + "] on graph [" + this.rag.getGraphName() + "]";
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
    @Path("/{name}")
    public Response deleteEdge(@PathParam("name") String name) {
        // TODO: delete individual properties

        final IndexableGraph graph = (IndexableGraph) this.rag.getGraph();
        graph.dropIndex(name);

        try {
            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();

    }

}
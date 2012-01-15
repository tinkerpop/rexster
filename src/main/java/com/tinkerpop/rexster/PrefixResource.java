package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.impls.sail.SailGraph;
import com.tinkerpop.rexster.extension.HttpMethod;
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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */

@Path("/graphs/{graphname}/prefixes")
public class PrefixResource extends AbstractSubResource {

    private static Logger logger = Logger.getLogger(PrefixResource.class);

    public PrefixResource() {
        super(null);
    }

    public PrefixResource(UriInfo ui, HttpServletRequest req, RexsterApplicationProvider rap) {
        super(rap);
        this.httpServletRequest = req;
        this.uriInfo = ui;
    }

    @OPTIONS
    public Response optionsPrefixes() {
        return buildOptionsResponse(HttpMethod.GET.toString(),
                HttpMethod.POST.toString());
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    public Response getPrefixes(@PathParam("graphname") String graphName) {

        try {
            final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
            final SailGraph graph = ((SailGraph) rag.getUnwrappedGraph());
            final JSONArray results = new JSONArray();
            for (final Map.Entry<String, String> entry : graph.getNamespaces().entrySet()) {
                JSONObject result = new JSONObject();
                result.put(entry.getKey(), entry.getValue());
                results.put(result);
            }

            this.resultObject.put(Tokens.RESULTS, results);
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

            return Response.ok(this.resultObject).build();
        } catch (JSONException ex) {
            logger.error(ex);
            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } catch (RuntimeException re) {
            logger.error(re);
            JSONObject error = generateErrorObject(re.getMessage(), re);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }
    }

    @OPTIONS
    @Path("/{prefix}")
    public Response optionsSinglePrefix() {
        return buildOptionsResponse(HttpMethod.GET.toString(),
                HttpMethod.DELETE.toString());
    }

    @GET
    @Path("/{prefix}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    public Response getSinglePrefix(@PathParam("graphname") String graphName, @PathParam("prefix") String prefix) {

        try {
            final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
            final SailGraph graph = ((SailGraph) rag.getUnwrappedGraph());
            this.resultObject.put(Tokens.RESULTS, graph.getNamespaces().get(prefix));
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

            return Response.ok(this.resultObject).build();
        } catch (JSONException ex) {
            logger.error(ex);
            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } catch (RuntimeException re) {
            logger.error(re);
            JSONObject error = generateErrorObject(re.getMessage(), re);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }
    }

    @DELETE
    @Path("/{prefix}")
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    public Response deleteSinglePrefix(@PathParam("graphname") String graphName, @PathParam("prefix") String prefix) {

        try {
            final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
            final SailGraph graph = ((SailGraph) rag.getUnwrappedGraph());
            graph.removeNamespace(prefix);
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

            return Response.ok(this.resultObject).build();
        } catch (JSONException ex) {
            logger.error(ex);
            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } catch (RuntimeException re) {
            logger.error(re);
            JSONObject error = generateErrorObject(re.getMessage(), re);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    @Consumes({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    public Response postSinglePrefix(@PathParam("graphname") String graphName, JSONObject json) {
        this.setRequestObject(json);
        return this.postSinglePrefix(graphName);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
    public Response postSinglePrefix(@PathParam("graphname") String graphName) {
        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);

        try {
            final SailGraph graph = ((SailGraph) rag.getUnwrappedGraph());

            final JSONObject reqObject = this.getRequestObject();

            if (!reqObject.has("prefix") || !reqObject.has("namespace")) {
                JSONObject error = generateErrorObject("Parameters 'prefix' and 'namespace' required");
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
            graph.addNamespace(reqObject.optString("prefix"), reqObject.optString("namespace"));
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

            return Response.ok(this.resultObject).build();
        } catch (JSONException ex) {
            logger.error(ex);
            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (RuntimeException re) {
            logger.error(re);
            JSONObject error = generateErrorObject(re.getMessage(), re);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }
    }
}

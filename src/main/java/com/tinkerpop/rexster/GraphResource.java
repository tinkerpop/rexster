package com.tinkerpop.rexster;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.IndexableGraph;
import com.tinkerpop.blueprints.pgm.Index;
import com.tinkerpop.rexster.traversals.ElementJSONObject;
import com.tinkerpop.rexster.traversals.Traversal;

@Path("/{graphname}")
public class GraphResource extends BaseResource {

	private static Logger logger = Logger.getLogger(GraphResource.class);
	
	public GraphResource(@PathParam("graphname") String graphName, @Context UriInfo ui, @Context HttpServletRequest req) throws JSONException {
        this.sh.stopWatch();
        this.resultObject.put(Tokens.VERSION, WebServer.GetRexsterApplication().getVersion());
		this.rag = WebServer.GetRexsterApplication().getApplicationGraph(graphName);
		Map<String, String> queryParameters = req.getParameterMap();
		this.buildRequestObject(queryParameters);
		
		this.request = req;
		this.uriInfo = ui;
	}
	
	@GET
    @Produces("application/json")
	public JSONObject getGraph() throws JSONException, Exception {
		
		if (this.rag != null) {
        
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
        }
        
        return this.resultObject;
	}
	
	@GET @Path("/traversal/{path: .+}")
	@Produces("application/json")
	public JSONObject getTraversal() throws JSONException, Exception {
		
		List<PathSegment> pathSegments = this.uriInfo.getPathSegments();
		String pattern = "";
		
		for (int ix = 2; ix < pathSegments.size(); ix++) {
			pattern = pattern + "/" + pathSegments.get(ix).getPath();
		}
	
        Class traversalClass = this.rag.getLoadedTraversals().get(pattern.substring(1));
        
        if (traversalClass != null){
        	Traversal traversal = (Traversal) traversalClass.newInstance();
            
            RexsterResourceContext ctx = new RexsterResourceContext();
            ctx.setRequest(this.request);
            ctx.setResultObject(this.resultObject);
            ctx.setUriInfo(this.uriInfo);
            ctx.setRexsterApplicationGraph(this.rag);
            ctx.setRequestObject(this.requestObject);
            
            traversal.evaluate(ctx);
        	
        }
        
        this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        
		return this.resultObject;
	}
	
	@GET @Path("/edges")
	@Produces("application/json")
	public JSONObject getAllEdges() throws JSONException, Exception {
		
		Long start = this.getStartOffset();
        if (null == start)
            start = 0l;
        Long end = this.getEndOffset();
        if (null == end)
            end = Long.MAX_VALUE;

        long counter = 0l;
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
        
        return this.resultObject;

    }
	
	@GET @Path("/edges/{id}")
	@Produces("application/json")
	public JSONObject getSingleEdge(@PathParam("id") String id) throws JSONException {
        final Edge edge = this.rag.getGraph().getEdge(id);
        
        if (null != edge) {
        	this.resultObject.put(Tokens.RESULTS, new ElementJSONObject(edge, this.getReturnKeys()));
        } else {
        	this.resultObject.put(Tokens.RESULTS, (Object) null);
        }
        
        this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());
        
        return this.resultObject;
    }
	
	@POST @Path("/edges/{id}")
	@Produces("application/json")
    public JSONObject postEdge(@PathParam("id") String id) throws JSONException {

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
        }
        if (null != edge) {
        	Iterator keys = this.requestObject.keys();
            while (keys.hasNext()){
            	String key = keys.next().toString();
                if (!key.startsWith(Tokens.UNDERSCORE))
                    edge.setProperty(key, this.requestObject.get(key));
            }
            this.resultObject.put(Tokens.RESULTS, new ElementJSONObject(edge, this.getReturnKeys()));
        }

        this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());

        return this.resultObject;
    }

    @DELETE  @Path("/edges/{id}")
	@Produces("application/json")
    public JSONObject deleteEdge(@PathParam("id") String id) throws JSONException {
        // TODO: delete individual properties
        
        final Graph graph = this.rag.getGraph();
        final Edge edge = graph.getEdge(id);
        if (null != edge)
            graph.removeEdge(edge);

        this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        return this.resultObject;

    }

	@GET @Path("/vertices/{id}/{direction}")
	@Produces("application/json")
	public JSONObject getVertexEdges(@PathParam("id") String vertexId, @PathParam("direction") String direction) {
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
            }

            this.resultObject.put(Tokens.RESULTS, edgeArray);
            this.resultObject.put(Tokens.TOTAL_SIZE, counter);
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return this.resultObject;
    }

	@GET @Path("/vertices/{id}")
	@Produces("application/json")
	public JSONObject getSingleVertex(@PathParam("id") String id) throws JSONException {
        Vertex vertex = this.rag.getGraph().getVertex(id);
        if (null != vertex) {
            this.resultObject.put(Tokens.RESULTS, new ElementJSONObject(vertex, this.getReturnKeys()));
        } else {
            this.resultObject.put(Tokens.RESULTS, (Object) null);
        }
        
        this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());
        
        return this.resultObject;
    }


    @GET @Path("/vertices")
	@Produces("application/json")
	public JSONObject getVertices()throws JSONException {
        Long start = this.getStartOffset();
        if (null == start)
            start = 0l;
        Long end = this.getEndOffset();
        if (null == end)
            end = Long.MAX_VALUE;

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
            //itty = this.rag.getGraph().getIndex().get(key, this.requestObject.get(key));
        	itty = ((IndexableGraph)this.rag.getGraph()).getIndex(Index.VERTICES, Vertex.class).get(key, this.requestObject.get(key));
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
        
        return this.resultObject;
    }
    
    @POST @Path("/vertices/{id}")
	@Produces("application/json")
    public JSONObject postVertex(@PathParam("id") String id) throws JSONException {
        
    	Graph graph = this.rag.getGraph();
        Vertex vertex = graph.getVertex(id);
        if (null == vertex) {
            vertex = graph.addVertex(id);
        }
        
        Iterator keys = this.requestObject.keys();
        while(keys.hasNext()) {
        	String key = keys.next().toString();
            if (!key.startsWith(Tokens.UNDERSCORE))
                vertex.setProperty(key, this.requestObject.get(key));
        }
        
        this.resultObject.put(Tokens.RESULTS, new ElementJSONObject(vertex, this.getReturnKeys()));
        this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        return this.resultObject;
    }

    @DELETE @Path("/vertices/{id}")
	@Produces("application/json")
    public JSONObject deleteVertex(@PathParam("id") String id) throws JSONException {
        // TODO: delete individual properties
        
        final Graph graph = this.rag.getGraph();
        final Vertex vertex = graph.getVertex(id);
        if (null != vertex)
            graph.removeVertex(vertex);

        this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        return this.resultObject;

    }	
}

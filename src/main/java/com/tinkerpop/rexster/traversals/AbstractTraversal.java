package com.tinkerpop.rexster.traversals;

import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.TransactionalGraph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;
import com.tinkerpop.rexster.BaseResource;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.ResultObjectCache;
import com.tinkerpop.rexster.RexsterApplication;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.WebServer;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.*;

import javax.ws.rs.core.MultivaluedMap;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class AbstractTraversal implements Traversal {

    protected Graph graph;
    protected static Logger logger = Logger.getLogger(AbstractTraversal.class);

    protected boolean success = true;
    protected String message = null;
    protected boolean usingCachedResult = false;
    protected String cacheRequestURI = null;
    protected boolean allowCached = true;
    protected ResultObjectCache resultObjectCache;
    
    protected RexsterResourceContext ctx;
    
    protected JSONObject resultObject = new JSONObject();
    protected JSONObject requestObject = new JSONObject();

    public AbstractTraversal() {
    	
    	if (WebServer.GetRexsterApplication() != null) {
    		this.resultObjectCache = WebServer.GetRexsterApplication().getResultObjectCache();
    	}
    }

    public JSONObject evaluate(RexsterResourceContext ctx) throws JSONException {
        
    	this.ctx = ctx;
    	this.graph = ctx.getRexsterApplicationGraph().getGraph();
    	this.requestObject = ctx.getRequestObject();
    	this.resultObject = ctx.getResultObject();
    	
    	this.preQuery();
        if (!usingCachedResult)
            this.traverse();
        this.postQuery();
        return this.resultObject;
    }

    /*
    @Get
    public JSONObject evaluate(final String json) {
    	
    	String graphName = this.getRequest().getResourceRef().getSegments().get(0);
        this.graph = ((RexsterApplication) this.getApplication()).getGraph(graphName);
    	
        if (null == json || json.length() == 0)
            return this.evaluate();
        else {
            this.buildRequestObject(json);
            this.preQuery();
            if (!usingCachedResult)
                this.traverse();
            this.postQuery();
            return this.resultObject;
        }
    }
     */
    
    private String createCacheRequestURI() {
        Map<String, String> queryParameters = this.ctx.getRequest().getParameterMap();
        
        List<Map.Entry<String, String>> list = new ArrayList<Map.Entry<String, String>>(queryParameters.entrySet());
        for (Map.Entry<String, String> entry : queryParameters.entrySet()){
        	if (entry.getKey() != Tokens.OFFSET_START
        		&& entry.getKey() != Tokens.OFFSET_END
        		&& entry.getKey() != Tokens.ALLOW_CACHED) {
        		list.add(entry);
        	}
        }
        
        java.util.Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
            public int compare(Map.Entry<String, String> e, Map.Entry<String, String> e1) {
                return e.getKey().compareTo(e1.getKey());
            }
        });
        return this.ctx.getUriInfo().getBaseUri().toString() + list.toString();
        //return this.getRequest().getResourceRef().getBaseRef().toString() + list.toString();
    }

    protected static List<Vertex> getVertices(final Graph graph, final JSONObject propertyMap) {
        List<Vertex> vertices = new ArrayList<Vertex>();
        Iterator keys = propertyMap.keys();
        while (keys.hasNext()) {
        	String key = (String) keys.next();
            if (key.equals(Tokens.ID)) {
            	try {
            		vertices.add(graph.getVertex(propertyMap.get(key)));
            	} catch (JSONException ex) {}
            } else {
            	try {
	                Iterable<Element> elements = graph.getIndex().get(key, propertyMap.get(key));
	                if (null != elements) {
	                    for (Element element : elements) {
	                        if (element instanceof Vertex) {
	                            vertices.add((Vertex) element);
	                        }
	                    }
	                }
            	} catch (JSONException ex) {}
            }
        }
        return vertices;
    }

    protected Vertex getVertex(final String requestObjectKey) {
        if (this.ctx.getRequestObject().has(requestObjectKey)) {
        	JSONObject jsonObject = (JSONObject) this.ctx.getRequestObject().opt(requestObjectKey);
        	List<Vertex> temp = getVertices(graph, jsonObject);
            if (null != temp && temp.size() > 0)
                return temp.get(0);
        }
        return null;
    }

    protected String getRequestValue(final String requestObjectKey) {
        return (String) this.ctx.getRequestObject().opt(requestObjectKey);
    }

    public void addApiToResultObject() {

    }

    protected void preQuery() {
        this.cacheRequestURI = this.createCacheRequestURI();
        Boolean temp = (Boolean) this.ctx.getRequestObject().opt(Tokens.ALLOW_CACHED);
        if (null != temp) {
            this.allowCached = temp;
        }
        logger.debug("Raw request object: " + this.ctx.getRequestObject().toString());
    }

    protected void postQuery() throws JSONException {
        this.ctx.getResultObject().put(Tokens.SUCCESS, this.success);
        if (!this.success) {
            this.addApiToResultObject();
        }
        if (null != message) {
            this.ctx.getResultObject().put(Tokens.MESSAGE, this.message);
        }
        logger.debug("Raw result object: " + this.resultObject.toString());
    }

    protected void cacheCurrentResultObjectState() {
        //JSONObject tempResultObject = new JSONObject();
        //tempResultObject.putAll(this.resultObject);
        this.resultObjectCache.putCachedResult(this.cacheRequestURI, this.resultObject);
    }

    protected Map<String, Object> getParameters() {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens.ALLOW_CACHED, "allow a previously cached result to be provided (default is true)");
        return parameters;
    }
}
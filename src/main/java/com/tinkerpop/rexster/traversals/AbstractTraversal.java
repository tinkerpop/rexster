package com.tinkerpop.rexster.traversals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Index;
import com.tinkerpop.blueprints.pgm.IndexableGraph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.rexster.ResultObjectCache;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.WebServer;

/**
 * This is a base implementation of the Traversal interface.  
 * 
 * The AbstractTraversal provides for a simple pipeline of request processing and 
 * caching support.  When a request is accepted by Rexster, the evaluate method is 
 * called and the context of the request is passed in.  The following methods are 
 * called in the following order to process the request:
 * 
 * <ul>
 * 	<li>preQuery</li>
 *  <li>isResultInCache</li>
 *  <li>traverse</li>
 *  <li>postQuery</li>
 * </ul>
 * 
 * The traverse method is only called if caching is not allowed or if the 
 * isResultInCache method returns false.   
 */
public abstract class AbstractTraversal implements Traversal {

    protected Graph graph;
    protected static Logger logger = Logger.getLogger(AbstractTraversal.class);

    protected boolean success = true;
    protected String message = null;
    protected String cacheRequestURI = null;
    protected boolean allowCached = true;
    protected ResultObjectCache resultObjectCache;
    
    protected RexsterResourceContext ctx;
    
    protected JSONObject resultObject = new JSONObject();
    protected JSONObject requestObject = new JSONObject();

    public JSONObject evaluate(RexsterResourceContext ctx) throws TraversalException {
        
    	if (ctx == null) {
    		throw new TraversalException("The resource context cannot be null.");
    	}
    	
    	if (ctx.getRexsterApplicationGraph() == null){
    		throw new TraversalException("The application graph from the resource context cannot be null.");
    	}
    	
    	this.ctx = ctx;
    	this.graph = ctx.getRexsterApplicationGraph().getGraph();
    	this.requestObject = ctx.getRequestObject();
    	this.resultObject = ctx.getResultObject();
    	this.resultObjectCache = ctx.getCache();
    	
    	if (this.graph == null){
    		throw new TraversalException("The graph from the resource context cannot be null.");
    	}
    	
    	if (this.requestObject == null){
    		throw new TraversalException("The request object from the resource context cannot be null.");
    	}
    	
    	if (this.resultObject == null){
    		throw new TraversalException("The result object from the resource context cannot be null.");
    	}
    	
    	try {
	    	this.preQuery();
	    	
	    	boolean resultInCache = false;
	    	
	    	// if the request does not want cached or the result is not in the cache
	    	// then go ahead and traverse
	        if (!this.allowCached || !(resultInCache = this.isResultInCache())) {
	            this.traverse();
	        }
	        
	        this.postQuery(resultInCache);
    	} catch (JSONException jsonException)  {
    		throw new TraversalException(jsonException);
    	}
    	
        return this.resultObject;
    }
    
    protected abstract void traverse() throws JSONException;
    protected abstract void addApiToResultObject();
    
    /**
     * Determine if the requested object is already in the cache.
     * 
     * Implementing classes should store cached values for use in the postQuery
     * method.  If the object is found, return a true value and false otherwise.
     *  
     * @return true if the object is in the cache and false otherwise.
     */
    protected abstract boolean isResultInCache();
    
    private String createCacheRequestURI() {
        Map<String, String> queryParameters = this.ctx.getRequest().getParameterMap();
        
        List<Map.Entry<String, String>> list = new ArrayList<Map.Entry<String, String>>();
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
	                Iterable<Vertex> verticesIterable = ((IndexableGraph) graph).getIndex(Index.VERTICES, Vertex.class).get(key, propertyMap.get(key));
	                for (Vertex vertex : verticesIterable) {
	                    vertices.add(vertex);
	                }
            	} catch (JSONException ex) {}
            }
        }
        return vertices;
    }

    protected Vertex getVertex(final String requestObjectKey) {
        if (this.ctx.getRequestObject().has(requestObjectKey)) {
        	JSONObject jsonObject = this.ctx.getRequestObject().optJSONObject(requestObjectKey);
        	List<Vertex> temp = getVertices(graph, jsonObject);
            if (null != temp && temp.size() > 0){
                return temp.get(0);
            }
        }
        return null;
    }

    protected String getRequestValue(final String requestObjectKey) {
    	return this.ctx.getRequestObject().optString(requestObjectKey, null);
    }

    /**
     * First method executed in the pipeline of request processing. 
     * 
     * The base implementation of this method gets the key to the cache based on the 
     * URI and determines if the allow_cached parameter was passed.
     * 
     * Implementing classes should call super.preQuery or implement this functionality
     * themselves to ensure caching works within the workflow of AbstractTraversal. 
     */
    protected void preQuery() {
        this.cacheRequestURI = this.createCacheRequestURI();
        Boolean temp = this.ctx.getRequestObject().optBoolean(Tokens.ALLOW_CACHED, false);
        if (null != temp) {
            this.allowCached = temp;
        }
        logger.debug("Raw request object: " + this.ctx.getRequestObject().toString());
    }

    protected void postQuery(boolean resultInCache) throws JSONException {
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
    	ArrayList<String> keysToCopy = new ArrayList<String>();
    	Iterator<String> keys = this.resultObject.keys();
    	while (keys.hasNext()) {
    		keysToCopy.add(keys.next());
    	}
    	
    	String[] toCopy = new String[keysToCopy.size()];
    	toCopy = keysToCopy.toArray(toCopy);
    	
    	try {
    		JSONObject tempResultObject = new JSONObject(this.resultObject, toCopy);
            this.resultObjectCache.putCachedResult(this.cacheRequestURI, tempResultObject);	
    	} catch (JSONException ex) {
    		// can't cache
    		logger.warn("Could not cache result for: " + this.cacheRequestURI, ex);
    	}
        
    }

    protected Map<String, Object> getParameters() {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens.ALLOW_CACHED, "allow a previously cached result to be provided (default is true)");
        return parameters;
    }
}
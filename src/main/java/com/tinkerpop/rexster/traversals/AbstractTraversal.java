package com.tinkerpop.rexster.traversals;

import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.TransactionalGraph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;
import com.tinkerpop.rexster.BaseResource;
import com.tinkerpop.rexster.ResultObjectCache;
import com.tinkerpop.rexster.RexsterApplication;
import com.tinkerpop.rexster.Tokens;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import java.util.*;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class AbstractTraversal extends BaseResource implements Traversal {

    protected Graph graph;
    protected ResultObjectCache resultObjectCache;
    protected static Logger logger = Logger.getLogger(AbstractTraversal.class);

    protected boolean success = true;
    protected String message = null;
    protected boolean usingCachedResult = false;
    protected String cacheRequestURI = null;
    protected boolean allowCached = true;

    public AbstractTraversal() {
        if (null != this.getApplication()) {
            this.resultObjectCache = this.getRexsterApplication().getResultObjectCache();
        }
    }

    //@Get
    public Representation evaluate() {
        Map<String, String> queryParameters = createQueryMap(this.getRequest().getResourceRef().getQueryAsForm());
        
        String graphName = this.getRequest().getResourceRef().getSegments().get(0);
        this.graph = ((RexsterApplication) this.getApplication()).getGraph(graphName);
        
        this.buildRequestObject(queryParameters);
        this.preQuery();
        if (!usingCachedResult)
            this.traverse();
        this.postQuery();
        return getStringRepresentation();
    }

    @Get
    public Representation evaluate(final String json) {
    	
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
            return getStringRepresentation();
        }
    }

    private String createCacheRequestURI() {
        Map<String, String> queryParameters = createQueryMap(this.getRequest().getResourceRef().getQueryAsForm());
        queryParameters.remove(Tokens.OFFSET_START);
        queryParameters.remove(Tokens.OFFSET_END);
        queryParameters.remove(Tokens.ALLOW_CACHED);
        List<Map.Entry<String, String>> list = new ArrayList<Map.Entry<String, String>>(queryParameters.entrySet());
        java.util.Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
            public int compare(Map.Entry<String, String> e, Map.Entry<String, String> e1) {
                return e.getKey().compareTo(e1.getKey());
            }
        });
        return this.getRequest().getResourceRef().getBaseRef().toString() + list.toString();
    }

    protected static List<Vertex> getVertices(final Graph graph, final Map<String, Object> propertyMap) {
        List<Vertex> vertices = new ArrayList<Vertex>();
        for (String key : propertyMap.keySet()) {
            if (key.equals(Tokens.ID)) {
                vertices.add(graph.getVertex(propertyMap.get(key)));
            } else {
                Iterable<Element> elements = graph.getIndex().get(key, propertyMap.get(key));
                if (null != elements) {
                    for (Element element : elements) {
                        if (element instanceof Vertex) {
                            vertices.add((Vertex) element);
                        }
                    }
                }
            }
        }
        return vertices;
    }

    protected Vertex getVertex(final String requestObjectKey) {
        if (this.requestObject.containsKey(requestObjectKey)) {
            List<Vertex> temp = getVertices(graph, (Map) requestObject.get(requestObjectKey));
            if (null != temp && temp.size() > 0)
                return temp.get(0);
        }
        return null;
    }

    protected String getRequestValue(final String requestObjectKey) {
        return (String) this.requestObject.get(requestObjectKey);
    }

    public void addApiToResultObject() {

    }

    protected void preQuery() {
        this.cacheRequestURI = this.createCacheRequestURI();
        Boolean temp = (Boolean) this.requestObject.get(Tokens.ALLOW_CACHED);
        if (null != temp) {
            this.allowCached = temp;
        }
        logger.debug("Raw request object: " + this.requestObject.toString());
    }

    protected void postQuery() {
        this.resultObject.put(Tokens.SUCCESS, this.success);
        if (!this.success) {
            this.addApiToResultObject();
        }
        if (null != message) {
            this.resultObject.put(Tokens.MESSAGE, this.message);
        }
        this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        logger.debug("Raw result object: " + this.resultObject.toJSONString());
    }

    protected void cacheCurrentResultObjectState() {
        JSONObject tempResultObject = new JSONObject();
        tempResultObject.putAll(this.resultObject);
        this.resultObjectCache.putCachedResult(this.cacheRequestURI, tempResultObject);
    }

    protected Map<String, Object> getParameters() {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens.ALLOW_CACHED, "allow a previously cached result to be provided (default is true)");
        return parameters;
    }
}
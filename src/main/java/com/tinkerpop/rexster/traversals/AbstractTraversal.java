package com.tinkerpop.rexster.traversals;

import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;
import com.tinkerpop.rexster.GraphHolder;
import com.tinkerpop.rexster.RestTokens;
import com.tinkerpop.rexster.ResultObjectCache;
import com.tinkerpop.rexster.StatisticsHelper;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import java.util.*;

/**
 * author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class AbstractTraversal extends ServerResource implements Traversal {

    protected static Graph graph = GraphHolder.getGraph();
    protected static Logger logger = Logger.getLogger(AbstractTraversal.class);

    protected final JSONParser parser = new JSONParser();
    protected final StatisticsHelper sh = new StatisticsHelper();

    protected static final String SUCCESS = "success";
    protected static final String MESSAGE = "message";
    protected static final String QUERY_TIME = "query_time";
    protected static final String ALLOW_CACHED = "allow_cached";
    protected static final String ID = "id";

    private static final String PERIOD_REGEX = "\\.";

    protected JSONObject requestObject;
    protected JSONObject resultObject = new JSONObject();

    protected boolean success = true;
    protected String message = null;
    protected boolean usingCachedResult = false;
    protected String cacheRequestURI = null;
    protected boolean allowCached = true;

    public AbstractTraversal() {
        sh.stopWatch();
    }

    @Get
    public Representation evaluate() {
        Map<String, String> queryParameters = this.getRequest().getResourceRef().getQueryAsForm().getValuesMap();
        this.buildRequestObject(queryParameters);
        this.preQuery();
        if (!usingCachedResult)
            this.traverse();
        this.postQuery();
        return new StringRepresentation(this.resultObject.toJSONString(), MediaType.APPLICATION_JSON);
    }

    @Get(RestTokens.JSON_APPLICATION)
    public Representation evaluate(final String json) {
        this.buildRequestObject(json);
        this.preQuery();
        if (!usingCachedResult)
            this.traverse();
        this.postQuery();
        return new StringRepresentation(this.resultObject.toJSONString(), MediaType.APPLICATION_JSON);
    }

    protected void buildRequestObject(final Map queryParameters) {
        this.requestObject = new JSONObject();

        for (String key : (Set<String>) queryParameters.keySet()) {
            String[] keys = key.split(PERIOD_REGEX);
            JSONObject embeddedObject = this.requestObject;
            for (int i = 0; i < keys.length - 1; i++) {
                JSONObject tempEmbeddedObject = (JSONObject) embeddedObject.get(keys[i]);
                if (null == tempEmbeddedObject) {
                    tempEmbeddedObject = new JSONObject();
                    embeddedObject.put(keys[i], tempEmbeddedObject);
                }
                embeddedObject = tempEmbeddedObject;
            }
            String rawValue = (String) queryParameters.get(key);
            try {
                Object parsedValue = parser.parse(rawValue);
                embeddedObject.put(keys[keys.length - 1], parsedValue);
            } catch (ParseException e) {
                embeddedObject.put(keys[keys.length - 1], rawValue);
            }
        }
    }

    protected void buildRequestObject(final String jsonString) {
        this.requestObject = new JSONObject();
        try {
            this.requestObject = (JSONObject) parser.parse(jsonString);
        } catch (ParseException e) {
            logger.error(e.getMessage());
        }
    }

    private String createCacheRequestURI() {
        Map<String, String> queryParameters = this.getRequest().getResourceRef().getQueryAsForm().getValuesMap();
        queryParameters.remove("offset.start");
        queryParameters.remove("offset.end");
        queryParameters.remove(ALLOW_CACHED);
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
            if (key.equals(ID)) {
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

    protected void preQuery() {
        if (graph instanceof Neo4jGraph) {
            ((Neo4jGraph) graph).startTransaction();
        }
        this.cacheRequestURI = this.createCacheRequestURI();
        Boolean temp = (Boolean) this.requestObject.get(ALLOW_CACHED);
        if (null != temp) {
            this.allowCached = temp;
        }
        logger.debug("Raw request object: " + this.requestObject.toString());
    }

    protected void postQuery() {
        if (graph instanceof Neo4jGraph) {
            ((Neo4jGraph) graph).stopTransaction(true);
        }
        this.resultObject.put(SUCCESS, this.success);
        if (null != message) {
            this.resultObject.put(MESSAGE, this.message);
        }
        this.resultObject.put(QUERY_TIME, sh.stopWatch());
        logger.debug("Raw result object: " + this.resultObject.toJSONString());
    }

    protected void cacheCurrentResultObjectState() {
        JSONObject tempResultObject = new JSONObject();
        tempResultObject.putAll(this.resultObject);
        ResultObjectCache.addCachedResult(this.cacheRequestURI, tempResultObject);
    }
}
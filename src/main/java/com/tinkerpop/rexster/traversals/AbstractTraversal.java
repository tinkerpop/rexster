package com.tinkerpop.rexster.traversals;

import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;
import com.tinkerpop.rexster.RestTokens;
import com.tinkerpop.rexster.ResultObjectCache;
import com.tinkerpop.rexster.RexsterApplication;
import com.tinkerpop.rexster.StatisticsHelper;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

import java.util.*;

/**
 * author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class AbstractTraversal extends ServerResource implements Traversal {

    protected Graph graph;
    protected ResultObjectCache resultObjectCache;
    protected static Logger logger = Logger.getLogger(AbstractTraversal.class);

    protected final JSONParser parser = new JSONParser();
    protected final StatisticsHelper sh = new StatisticsHelper();

    protected static final String SUCCESS = "success";
    protected static final String MESSAGE = "message";
    protected static final String QUERY_TIME = "query_time";
    protected static final String ALLOW_CACHED = "allow_cached";
    protected static final String OFFSET_END = "offset.end";
    protected static final String OFFSET_START = "offset.start";
    protected static final String ID = "id";

    private static final String PERIOD_REGEX = "\\.";
    private static final String COMMA = ",";
    private static final String LEFT_BRACKET = "[";
    private static final String RIGHT_BRACKET = "]";

    protected JSONObject requestObject;
    protected JSONObject resultObject = new JSONObject();

    protected boolean success = true;
    protected String message = null;
    protected boolean usingCachedResult = false;
    protected String cacheRequestURI = null;
    protected boolean allowCached = true;

    public AbstractTraversal() {
        sh.stopWatch();
        if (null != this.getApplication()) {
            this.graph = ((RexsterApplication) this.getApplication()).getGraph();
            this.resultObjectCache = ((RexsterApplication) this.getApplication()).getResultObjectCache();
        }
    }

    private static Map<String, String> createQueryMap(final Series<Parameter> series) {
        Map<String, String> map = new HashMap<String, String>();
        for (Parameter parameter : series) {
            map.put(parameter.getName(), parameter.getValue());
        }
        return map;
    }

    @Get
    public Representation evaluate() {
        Map<String, String> queryParameters = createQueryMap(this.getRequest().getResourceRef().getQueryAsForm());
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
                if (rawValue.startsWith(LEFT_BRACKET) && rawValue.endsWith(RIGHT_BRACKET)) {
                    rawValue = rawValue.substring(1,rawValue.length()-1);
                    JSONArray array = new JSONArray();
                    for(String value : rawValue.split(COMMA)) {
                        array.add(value.trim());
                    }
                    embeddedObject.put(keys[keys.length - 1], array);
                } else {
                    Object parsedValue = parser.parse(rawValue);
                    embeddedObject.put(keys[keys.length - 1], parsedValue);
                }
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
        Map<String, String> queryParameters = createQueryMap(this.getRequest().getResourceRef().getQueryAsForm());
        queryParameters.remove(OFFSET_START);
        queryParameters.remove(OFFSET_END);
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

    protected String getRequestValue(final String requestObjectKey) {
        return (String) this.requestObject.get(requestObjectKey);
    }

    public void addApiToResultObject() {

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
        if (!this.success) {
            this.addApiToResultObject();
        }
        if (null != message) {
            this.resultObject.put(MESSAGE, this.message);
        }
        this.resultObject.put(QUERY_TIME, sh.stopWatch());
        logger.debug("Raw result object: " + this.resultObject.toJSONString());
    }

    protected void cacheCurrentResultObjectState() {
        JSONObject tempResultObject = new JSONObject();
        tempResultObject.putAll(this.resultObject);
        this.resultObjectCache.putCachedResult(this.cacheRequestURI, tempResultObject);
    }

    protected Map<String, Object> getParameters() {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(ALLOW_CACHED, "allow a previously cached result to be provided (default is true)");
        return parameters;
    }
}
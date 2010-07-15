package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Vertex;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.restlet.data.Parameter;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class BaseResource extends ServerResource {

    protected final JSONParser parser = new JSONParser();
    protected JSONObject requestObject = new JSONObject();
    protected final JSONObject resultObject = new JSONObject();
    protected final static Logger logger = Logger.getLogger(BaseResource.class);
    protected final StatisticsHelper sh = new StatisticsHelper();

    protected static final String REXSTER = "rexster";
    protected static final String RETURN_KEYS = "return_keys";
    protected static final String OFFSET = "offset";
    protected static final String START = "start";
    protected static final String END = "end";

    protected static final String OUT_E = "outE";
    protected static final String IN_E = "inE";
    protected static final String BOTH_E = "bothE";
    protected static final String TOTAL_SIZE = "total_size";
    protected static final String MESSAGE = "message";
    protected static final String QUERY_TIME = "query_time";
    protected static final String RESULT = "result";
    protected static final String VERSION = "version";
    protected static final String UNDERSCORE = "_";
    protected static final String PERIOD_REGEX = "\\.";
    protected static final String COMMA = ",";
    protected static final String LEFT_BRACKET = "[";
    protected static final String RIGHT_BRACKET = "]";


    public BaseResource() {
        sh.stopWatch();
        this.resultObject.put(VERSION, RexsterApplication.getVersion());
    }

    protected RexsterApplication getRexsterApplication() {
        return (RexsterApplication) this.getApplication();
    }

    protected static Map<String, String> createQueryMap(final Series<Parameter> series) {
        Map<String, String> map = new HashMap<String, String>();
        for (Parameter parameter : series) {
            map.put(parameter.getName(), parameter.getValue());
        }
        return map;
    }

    public void buildRequestObject(final Map queryParameters) {
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
                    rawValue = rawValue.substring(1, rawValue.length() - 1);
                    JSONArray array = new JSONArray();
                    for (String value : rawValue.split(COMMA)) {
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

    public void buildRequestObject(final String jsonString) {
        try {
            this.requestObject = (JSONObject) parser.parse(jsonString);
        } catch (ParseException e) {
            logger.error(e.getMessage());
        }
    }

    public JSONObject getRexsterRequest() {
        return (JSONObject) this.requestObject.get(REXSTER);
    }

    protected JSONObject getNonRexsterRequest() {
        JSONObject object = new JSONObject();
        for (Map.Entry entry : (Set<Map.Entry>) this.requestObject.entrySet()) {
            if (!entry.getKey().equals(REXSTER)) {
                object.put(entry.getKey(), entry.getValue());
            }
        }
        return object;
    }

    public Long getStartOffset() {
        JSONObject rexster = this.getRexsterRequest();
        if (null != rexster) {
            if (rexster.containsKey(OFFSET)) {
                Long start = ((Long) ((JSONObject) rexster.get(OFFSET)).get(START));
                if (null != start)
                    return start;
                else
                    return null;
            } else
                return null;
        } else {
            return null;
        }
    }


    public Long getEndOffset() {
        JSONObject rexster = this.getRexsterRequest();
        if (null != rexster) {
            if (rexster.containsKey(OFFSET)) {
                Long end = ((Long) ((JSONObject) rexster.get(OFFSET)).get(END));
                if (null != end)
                    return end;
                else
                    return null;
            } else
                return null;
        } else {
            return null;
        }

    }

    public List<String> getReturnKeys() {
        JSONObject rexster = this.getRexsterRequest();
        if (null != rexster)
            return (List<String>) rexster.get(RETURN_KEYS);
        else
            return null;
    }


    protected boolean hasPropertyValues(Element element, JSONObject properties) {
        for (Map.Entry entry : (Set<Map.Entry>) properties.entrySet()) {
            Object temp;
            if (entry.getKey().equals(Tokens._ID))
                temp = element.getId();
            else if (entry.getKey().equals(Tokens._LABEL))
                temp = ((Edge) element).getLabel();
            else if (entry.getKey().equals(Tokens._IN_V))
                temp = ((Edge) element).getInVertex().getId();
            else if (entry.getKey().equals(Tokens._OUT_V))
                temp = ((Edge) element).getOutVertex().getId();
            else if (entry.getKey().equals(Tokens._TYPE)) {
                if (element instanceof Vertex)
                    temp = Tokens.VERTEX;
                else
                    temp = Tokens.EDGE;
            } else
                temp = element.getProperty((String) entry.getKey());
            if (null == temp || !temp.equals(entry.getValue()))
                return false;
        }
        return true;
    }

    public JSONObject getRequestObject() {
        return this.requestObject;
    }

    public JSONObject getResultObject() {
        return this.resultObject;
    }
}
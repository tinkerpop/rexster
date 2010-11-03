package com.tinkerpop.rexster;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Vertex;

public abstract class BaseResource {

	private static Logger logger = Logger.getLogger(BaseResource.class);
	
	protected RexsterApplicationGraph rag = null;

    protected final StatisticsHelper sh = new StatisticsHelper();
    
    protected JSONObject requestObject = new JSONObject();
    
    protected JSONObject resultObject = new JSONObject();
    
    protected HttpServletRequest request;
    
    protected UriInfo uriInfo;

    public BaseResource() {
        sh.stopWatch();
        
        try {
        	this.resultObject.put(Tokens.VERSION, RexsterApplication.getVersion());
        } catch (Exception ex) {}
    }
    
    public JSONObject getRequestObject() {
		return requestObject;
	}

	public void setRequestObject(JSONObject requestObject) {
		this.requestObject = requestObject;
	}

	public JSONObject getResultObject() {
		return resultObject;
	}

	public void setResultObject(JSONObject resultObject) {
		this.resultObject = resultObject;
	}

	public UriInfo getUriInfo() {
		return uriInfo;
	}

	public void setUriInfo(UriInfo uriInfo) {
		this.uriInfo = uriInfo;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}
	
    public void buildRequestObject(final Map queryParameters) throws JSONException {
        for (String key : (Set<String>) queryParameters.keySet()) {
            String[] keys = key.split(Tokens.PERIOD_REGEX);
            JSONObject embeddedObject = this.requestObject;
            for (int i = 0; i < keys.length - 1; i++) {
                JSONObject tempEmbeddedObject = (JSONObject) embeddedObject.opt(keys[i]);
                if (null == tempEmbeddedObject) {
                    tempEmbeddedObject = new JSONObject();
                    embeddedObject.put(keys[i], tempEmbeddedObject);
                }
                embeddedObject = tempEmbeddedObject;
            }
            
            // grrr...why do i have to do this?
            String rawValue;
            Object val = queryParameters.get(key);
            if (val instanceof String){
            	rawValue = (String) val;
            } else { 
            	String[] values = (String[]) val;
            	rawValue = values[0];
            }
            
            try {
                if (rawValue.startsWith(Tokens.LEFT_BRACKET) && rawValue.endsWith(Tokens.RIGHT_BRACKET)) {
                    rawValue = rawValue.substring(1, rawValue.length() - 1);
                    JSONArray array = new JSONArray();
                    for (String value : rawValue.split(Tokens.COMMA)) {
                        array.put(value.trim());
                    }
                    embeddedObject.put(keys[keys.length - 1], array);
                } else {
                	JSONTokener tokener = new JSONTokener(rawValue);
                	Object parsedValue = new JSONObject(tokener);
                    embeddedObject.put(keys[keys.length - 1], parsedValue);
                }
            } catch (JSONException e) {
            	embeddedObject.put(keys[keys.length - 1], rawValue);
            }
        }
    }
    
    public void buildRequestObject(final String jsonString) {
        try {
        	JSONTokener tokener = new JSONTokener(jsonString);
            this.requestObject = new JSONObject(tokener);
        } catch (JSONException e) {
            logger.error(e.getMessage());
        }
    }

    public JSONObject getRexsterRequest() {
        return this.requestObject.optJSONObject(Tokens.REXSTER);
    }

    protected JSONObject getNonRexsterRequest() throws JSONException {
        JSONObject object = new JSONObject();
        Iterator keys = this.requestObject.keys();
        while(keys.hasNext()) {
        	String key = keys.next().toString();
            if (!key.equals(Tokens.REXSTER)) {
                object.put(key, this.requestObject.opt(key));
            }
        }
        return object;
    }

    public Long getStartOffset() {
        JSONObject rexster = this.getRexsterRequest();
        if (null != rexster) {
            if (rexster.has(Tokens.OFFSET)) {
                Long start = rexster.optJSONObject(Tokens.OFFSET).optLong(Tokens.START);
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
            if (rexster.has(Tokens.OFFSET)) {
                Long end = rexster.optJSONObject(Tokens.OFFSET).optLong(Tokens.END);
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
        if (null != rexster) {
            //return (List<String>) rexster.opt(Tokens.RETURN_KEYS);
        	JSONArray arr = rexster.optJSONArray(Tokens.RETURN_KEYS);
            List<String> keys = new ArrayList<String>();
            
            if (arr != null){
	            for (int ix = 0; ix < arr.length(); ix++){
	            	keys.add(arr.optString(ix));
	            }
            }
            else {
            	keys = null;
            }
            	
        	return keys;
        } else {
            return null;
        }
    }


    protected boolean hasPropertyValues(Element element, JSONObject properties) throws JSONException {
    	Iterator keys = properties.keys();
        while(keys.hasNext()) {
        	String key = keys.next().toString();
            Object temp;
            if (key.equals(Tokens._ID))
                temp = element.getId();
            else if (key.equals(Tokens._LABEL))
                temp = ((Edge) element).getLabel();
            else if (key.equals(Tokens._IN_V))
                temp = ((Edge) element).getInVertex().getId();
            else if (key.equals(Tokens._OUT_V))
                temp = ((Edge) element).getOutVertex().getId();
            else if (key.equals(Tokens._TYPE)) {
                if (element instanceof Vertex)
                    temp = Tokens.VERTEX;
                else
                    temp = Tokens.EDGE;
            } else
                temp = element.getProperty(key);
            if (null == temp || !temp.equals(properties.get(key)))
                return false;
        }
        return true;
    }
	
	protected String getTimeAlive() {
        long timeMillis = System.currentTimeMillis() - WebServer.GetRexsterApplication().getStartTime();
        long timeSeconds = timeMillis / 1000;
        long timeMinutes = timeSeconds / 60;
        long timeHours = timeMinutes / 60;
        long timeDays = timeHours / 24;

        String seconds = Integer.toString((int) (timeSeconds % 60));
        String minutes = Integer.toString((int) (timeMinutes % 60));
        String hours = Integer.toString((int) timeHours % 24);
        String days = Integer.toString((int) timeDays);

        for (int i = 0; i < 2; i++) {
            if (seconds.length() < 2) {
                seconds = "0" + seconds;
            }
            if (minutes.length() < 2) {
                minutes = "0" + minutes;
            }
            if (hours.length() < 2) {
                hours = "0" + hours;
            }
        }
        return days + "[d]:" + hours + "[h]:" + minutes + "[m]:" + seconds + "[s]";
    }
}
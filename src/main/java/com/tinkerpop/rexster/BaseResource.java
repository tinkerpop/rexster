package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Vertex;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.util.*;

public abstract class BaseResource {

    private static Logger logger = Logger.getLogger(BaseResource.class);

    public static final String HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    private RexsterApplicationGraph rag = null;

    protected final StatisticsHelper sh = new StatisticsHelper();

    private JSONObject requestObject = null;

    protected JSONObject resultObject = new JSONObject();

    private RexsterApplicationProvider rexsterApplicationProvider;

    @Context
    protected HttpServletRequest httpServletRequest;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected ServletContext servletContext;

    public BaseResource(RexsterApplicationProvider rexsterApplicationProvider) {

        // the general assumption is that the web server is the provider for RexsterApplication
        // instances.  this really should only change in unit test scenarios.
        this.rexsterApplicationProvider = rexsterApplicationProvider;

        sh.stopWatch();

        try {
            this.resultObject.put(Tokens.VERSION, RexsterApplication.getVersion());
        } catch (JSONException ex) {
            JSONObject error = generateErrorObject(ex.getMessage());
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }
    }

    public JSONObject generateErrorObject(String message) {
        return generateErrorObject(message, null);
    }

    public JSONObject generateErrorObjectJsonFail(Exception source) {
        return generateErrorObject("An error occurred while generating the response object", source);
    }

    public JSONObject generateErrorObject(String message, Exception source) {
        Map<String, String> m = new HashMap<String, String>();
        m.put(Tokens.MESSAGE, message);

        if (source != null) {
            m.put("error", source.getMessage());
        }

        // use a hashmap with the constructor so that a JSONException
        // will not be thrown
        return new JSONObject(m);
    }

    protected RexsterApplicationProvider getRexsterApplicationProvider() {
        if (this.rexsterApplicationProvider == null) {
            try {
                this.rexsterApplicationProvider = new WebServerRexsterApplicationProvider(this.servletContext);
            } catch (Exception ex) {
                logger.info("The Rexster Application Provider could not be configured", ex);
            }
        }

        return this.rexsterApplicationProvider;
    }

    /**
     * Sets the request object.
     * <p/>
     * If this is set then the any previous call to getRequestObject which instantiated
     * the request object from the URI parameters will be overriden.
     *
     * @param jsonObject The JSON Object.
     */
    protected void setRequestObject(JSONObject jsonObject) {
        this.requestObject = jsonObject;
    }

    /**
     * Gets the request object.
     * <p/>
     * If it does not exist then an attempt is made to parse the parameter list on
     * the URI into a JSON object.  It is then stored until the next request so
     * that parse of the URI does not have to happen more than once.
     *
     * @return The request object.
     */
    public JSONObject getRequestObject() {
        if (this.requestObject == null) {
            try {
                this.requestObject = new JSONObject();

                if (this.httpServletRequest != null) {
                    Map<String, String> queryParameters = this.httpServletRequest.getParameterMap();
                    this.buildRequestObject(queryParameters);
                }

            } catch (JSONException ex) {

                logger.error(ex);

                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
        }

        return this.requestObject;
    }

    private void buildRequestObject(final Map queryParameters) throws JSONException {
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
            if (val instanceof String) {
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

    public void buildRequestObject(final MultivaluedMap<String, String> formParams) {
        HashMap map = new HashMap();

        // need to pick apart the multivalue map.  want single primitives instead of
        // lists if possible.
        for (String key : formParams.keySet()) {
            List list = formParams.get(key);
            if (list != null && list.size() > 1) {
                JSONArray set = new JSONArray();
                set.put(list);
                map.put(key, set);
            } else if (list.size() == 1) {
                map.put(key, formParams.getFirst(key));
            } else {
                map.put(key, "");
            }
        }

        this.requestObject = new JSONObject(map);
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
        return this.getRequestObject().optJSONObject(Tokens.REXSTER);
    }

    protected JSONObject getNonRexsterRequest() throws JSONException {
        JSONObject object = new JSONObject();
        Iterator keys = this.getRequestObject().keys();
        while (keys.hasNext()) {
            String key = keys.next().toString();
            if (!key.equals(Tokens.REXSTER)) {
                object.put(key, this.getRequestObject().opt(key));
            }
        }
        return object;
    }

    protected List<String> getNonRexsterRequestKeys() throws JSONException {
        final List<String> keys = new ArrayList<String>();
        final JSONObject request = this.getNonRexsterRequest();
        if (request.length() > 0) {
            final Iterator itty = request.keys();
            while (itty.hasNext()) {
                keys.add((String) itty.next());
            }
        }
        return keys;

    }

    public Long getStartOffset() {
        Long offset = getOffset(Tokens.START);
        if (null == offset)
            return 0l;
        else
            return offset;
    }


    public Long getEndOffset() {
        Long offset = getOffset(Tokens.END);
        if (null == offset)
            return Long.MAX_VALUE;
        else
            return offset;
    }

    private Long getOffset(String offsetToken) {
        JSONObject rexster = this.getRexsterRequest();
        if (rexster != null) {
            if (rexster.has(Tokens.OFFSET)) {

                // returns zero if the value identified by the offsetToken is
                // not a number and the key is just present.
                if (rexster.optJSONObject(Tokens.OFFSET).has(offsetToken)) {
                    return rexster.optJSONObject(Tokens.OFFSET).optLong(offsetToken);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Determines if the data types should be shown in the results.
     * <p/>
     * Checks the request for the show_types parameter which must be set
     * to a boolean value. Types are not shown by default or if the value
     * cannot be parsed from the request.
     *
     * @return true if showTypes is set to "true" and false otherwise.
     */
    protected boolean hasShowTypes() {
        boolean showTypes = false;
        JSONObject rexster = this.getRexsterRequest();
        if (rexster != null) {
            if (rexster.has(Tokens.SHOW_TYPES)) {
                showTypes = rexster.optBoolean(Tokens.SHOW_TYPES, false);
            }
        }

        return showTypes;
    }

    public List<String> getReturnKeys() {
        JSONObject rexster = this.getRexsterRequest();
        if (null != rexster) {

            List<String> keys = new ArrayList<String>();
            Object returnKeys = rexster.opt(Tokens.RETURN_KEYS);

            if (returnKeys != null) {

                // returnKeys may be an array if multiple return keys were sent or
                // just a string...this could be an error in how the JSON request object
                // is being built...not sure.
                if (returnKeys instanceof JSONArray) {
                    JSONArray arr = (JSONArray) returnKeys;

                    if (arr != null) {
                        for (int ix = 0; ix < arr.length(); ix++) {
                            keys.add(arr.optString(ix));
                        }
                    }
                } else if (returnKeys instanceof String) {
                    keys.add(returnKeys.toString());
                } else {
                    keys = null;
                }
            } else {
                keys = null;
            }

            if (keys != null && keys.size() == 1 && keys.get(0).equals(Tokens.WILDCARD)) {
                keys = null;
            }

            return keys;
        } else {
            return null;
        }
    }


    protected boolean hasPropertyValues(Element element, JSONObject properties) throws JSONException {
        Iterator keys = properties.keys();
        while (keys.hasNext()) {
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

    protected boolean hasElementProperties(JSONObject requestObject) {
        Iterator keys = requestObject.keys();
        while (keys.hasNext()) {
            String key = keys.next().toString();
            if (!key.startsWith(Tokens.UNDERSCORE)) {
                return true;
            }
        }
        return false;
    }

    protected String getTimeAlive() {
        long timeMillis = System.currentTimeMillis() - this.rexsterApplicationProvider.getStartTime();
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

    /**
     * Adds headers to a response.
     *
     * @param builder The response builder to append headers to.
     * @return The builder for the response with the appended headers.
     */
    protected ResponseBuilder addHeaders(ResponseBuilder builder) {
        // allows services to be consumed from other domains...specifically,
        // the one used by the rexster web tool
        return builder.header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*");
    }
}
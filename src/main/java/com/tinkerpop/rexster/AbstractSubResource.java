package com.tinkerpop.rexster;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractSubResource extends BaseResource {

    private static final Logger logger = Logger.getLogger(AbstractSubResource.class);

    protected AbstractSubResource(String graphName, UriInfo ui, HttpServletRequest req) {
        super();

        this.rag = WebServer.GetRexsterApplication().getApplicationGraph(graphName);
        if (this.rag == null) {

            logger.info("Request for a non-configured graph [" + graphName + "]");

            JSONObject error = generateErrorObject("Graph [" + graphName + "] could not be found");
            throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
        }

        try {
            this.resultObject.put(Tokens.VERSION, RexsterApplication.getVersion());
            Map<String, String> queryParameters = req.getParameterMap();
            this.buildRequestObject(queryParameters);

            this.request = req;
            this.uriInfo = ui;
        } catch (JSONException ex) {

            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }
    }

    protected AbstractSubResource(RexsterApplicationGraph rag, UriInfo ui, HttpServletRequest req) {
        super();

        this.rag = rag;
        if (this.rag == null) {

            logger.info("Request for a non-configured graph");

            JSONObject error = generateErrorObject("Graph could not be found");
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        try {
            this.resultObject.put(Tokens.VERSION, RexsterApplication.getVersion());
            Map<String, String> queryParameters = req.getParameterMap();
            this.buildRequestObject(queryParameters);

            this.request = req;
            this.uriInfo = ui;
        } catch (JSONException ex) {

            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }
    }

    /**
     * Takes a property value string from the URI and attempts to parse it
     * to its defined data type.
     * <p/>
     * The format of the property value must be enclosed with parens with
     * two values within it separated by a comma.  The first value is the data
     * type which may be one of the following:
     * <p/>
     * i or integer
     * f or float
     * l or long
     * d or double
     * s or string
     * map
     * list
     * <p/>
     * In the event that the type is not set or the parens cannot be tracked
     * as open and closed the parser will determine the value to simply be a string.
     * <p/>
     * The value of a list must be defined with enclosing square brackets with values
     * separated by commas.  Values are defined with the same rules involving parens
     * defined above.
     * <p/>
     * The value of a map must be defined with enclosing parens where comma
     * separated segments of new key-value pairs represent the properties of the
     * map itself.
     * <p/>
     * Values may be nested to any depth.
     *
     * @param propertyValue The value from a key-value pair.  At a top level, this will
     *                      come from the URI as a one of the query parameters.
     * @return The property value coerced to the appropriate Java data type.
     */
    protected Object getTypedPropertyValue(String propertyValue) {
        Object typedPropertyValue = propertyValue;
        if (typedPropertyValue == null) {
            typedPropertyValue = "";
        }

        // determine if the property is typed, otherwise assume it is a string
        if (propertyValue != null && propertyValue.startsWith("(") && propertyValue.endsWith(")")) {
            String dataType = this.getDataTypeSegment(propertyValue);
            String theValue = this.getValueSegment(propertyValue);

            if (dataType.equals("string")) {
                typedPropertyValue = theValue;
            } else if (dataType.equals("integer")) {
                typedPropertyValue = tryParseInteger(theValue);
            } else if (dataType.equals("long")) {
                typedPropertyValue = tryParseLong(theValue);
            } else if (dataType.equals("double")) {
                typedPropertyValue = tryParseDouble(theValue);
            } else if (dataType.equals("float")) {
                typedPropertyValue = tryParseFloat(theValue);
            } else if (dataType.equals("list")) {
                ArrayList<String> items = this.tryParseList(theValue);
                ArrayList typedItems = new ArrayList();
                for (String item : items) {
                    typedItems.add(this.getTypedPropertyValue(item));
                }

                typedPropertyValue = typedItems;
            } else if (dataType.equals("map")) {
                HashMap<String, String> stringProperties = this.tryParseMap(theValue);
                HashMap<String, Object> properties = new HashMap<String, Object>();

                for (Map.Entry<String, String> entry : stringProperties.entrySet()) {
                    properties.put(entry.getKey(), this.getTypedPropertyValue(entry.getValue()));
                }

                typedPropertyValue = properties;
            }
        }

        return typedPropertyValue;
    }

    private HashMap<String, String> tryParseMap(String mapValue) {
        // parens on the ends have been validated already...they must be
        // here to have gotten this far.
        String stripped = mapValue.substring(1, mapValue.length() - 1);

        HashMap<String, String> pairs = new HashMap<String, String>();

        ArrayList<Integer> delimiterPlaces = new ArrayList<Integer>();
        int parensOpened = 0;

        for (int ix = 0; ix < stripped.length(); ix++) {
            char c = stripped.charAt(ix);
            if (c == ',') {
                if (parensOpened == 0) {
                    delimiterPlaces.add(ix);
                }
            } else if (c == '(') {
                parensOpened++;
            } else if (c == ')') {
                parensOpened--;
            }
        }

        int lastPlace = 0;
        int equalPlace = 0;
        for (Integer place : delimiterPlaces) {
            String property = stripped.substring(lastPlace, place);
            equalPlace = property.indexOf("=");
            pairs.put(property.substring(0, equalPlace), property.substring(equalPlace + 1));
            lastPlace = place + 1;
        }

        String property = stripped.substring(lastPlace);
        equalPlace = property.indexOf("=");
        pairs.put(property.substring(0, equalPlace), property.substring(equalPlace + 1));

        return pairs;
    }

    private ArrayList<String> tryParseList(String listValue) {

        // square brackets on the ends have been validated already...they must be
        // here to have gotten this far.
        String stripped = listValue.substring(1, listValue.length() - 1);

        ArrayList<String> items = new ArrayList<String>();

        int place = stripped.indexOf(',');

        if (place > -1) {

            boolean isEndItem = false;
            int parensOpened = 0;
            StringBuffer sb = new StringBuffer();
            for (int ix = 0; ix < stripped.length(); ix++) {
                char c = stripped.charAt(ix);
                if (c == ',') {
                    // a delimiter was found, determine if it is a true
                    // delimiter or if it is part of a value or a separator
                    // with a paren set
                    if (parensOpened == 0) {
                        isEndItem = true;
                    }
                } else if (c == '(') {
                    parensOpened++;
                } else if (c == ')') {
                    parensOpened--;
                }

                if (ix == stripped.length() - 1) {
                    // this is the last character in the value...by default
                    // this is an end item event
                    isEndItem = true;

                    // append the character because it is valid in the value and
                    // not a delimiter that would normally be added.
                    sb.append(c);
                }

                if (isEndItem) {
                    items.add(sb.toString());
                    sb = new StringBuffer();
                    isEndItem = false;
                } else {
                    sb.append(c);
                }
            }
        } else {
            // there is one item in the array or the array is empty
            items.add(stripped);
        }

        return items;
    }

    private String getValueSegment(String propertyValue) {
        // assumes that the propertyValue has open and closed parens
        String value = "";
        String stripped = propertyValue.substring(1, propertyValue.length() - 1);
        int place = stripped.indexOf(',');

        // assume that the first char could be the comma, which
        // would default to a string data type.
        if (place > -1) {
            if (place + 1 < stripped.length()) {
                // stripped doesn't have the trailing paren so take
                // the value from the first character after the comma to the end
                value = stripped.substring(place + 1);
            } else {
                // there is nothing after the comma
                value = "";
            }
        } else {
            // there was no comma, so the whole thing is the value and it
            // is assumed to be a string
            value = stripped;
        }

        return value;
    }

    private String getDataTypeSegment(String propertyValue) {
        // assumes that the propertyValue has open and closed parens
        String dataType = "string";

        // strip the initial parens and read up to the comman.
        // no need to check for string as that is the default
        int place = propertyValue.indexOf(',');
        String inner = propertyValue.substring(1, place).trim();
        if (inner.equals("s") || inner.equals("string")) {
            dataType = "string";
        } else if (inner.equals("i") || inner.equals("integer")) {
            dataType = "integer";
        } else if (inner.equals("d") || inner.equals("double")) {
            dataType = "double";
        } else if (inner.equals("f") || inner.equals("float")) {
            dataType = "float";
        } else if (inner.equals("list")) {
            // TODO: need to ensure square brackets enclose the array
            dataType = "list";
        } else if (inner.equals("l") || inner.equals("long")) {
            dataType = "long";
        } else if (inner.equals("map")) {
            // TODO: need to validate format...outer parens plus name value pairs
            dataType = "map";
        }

        return dataType;
    }

    private Object tryParseInteger(String intValue) {
        Object parsedValue;
        try {
            parsedValue = Integer.parseInt(intValue);
        } catch (NumberFormatException nfe) {
            parsedValue = intValue;
        }

        return parsedValue;
    }

    private Object tryParseFloat(String floatValue) {
        Object parsedValue;
        try {
            parsedValue = Float.parseFloat(floatValue);
        } catch (NumberFormatException nfe) {
            parsedValue = floatValue;
        }

        return parsedValue;
    }

    private Object tryParseLong(String longValue) {
        Object parsedValue;
        try {
            parsedValue = Long.parseLong(longValue);
        } catch (NumberFormatException nfe) {
            parsedValue = longValue;
        }

        return parsedValue;
    }

    private Object tryParseDouble(String doubleValue) {
        Object parsedValue;
        try {
            parsedValue = Double.parseDouble(doubleValue);
        } catch (NumberFormatException nfe) {
            parsedValue = doubleValue;
        }

        return parsedValue;
    }
}

package com.tinkerpop.rexster.util;

import com.tinkerpop.blueprints.Compare;
import com.tinkerpop.blueprints.VertexQuery;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Helper class for reading parameters from the JSON request object.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RequestObjectHelper {

    /**
     * Given a request object, return the fragment of JSON that deals with Rexster-reserved parameters.
     * <p/>
     * These parameters are the returnKeys, showTypes, and offset.
     *
     * @param requestObject the request object
     * @return the JSON
     */
    public static JSONObject getRexsterRequest(final JSONObject requestObject) {
        return requestObject != null ? requestObject.optJSONObject(Tokens.REXSTER) : null;
    }

    /**
     * Given a request object, return the desired returnKeys. Utilizes the value of the DEFAULT_WILDCARD.
     *
     * @param requestObject the request object
     * @return the return keys
     */
    public static Set<String> getReturnKeys(final JSONObject requestObject) {
        return getReturnKeys(requestObject, Tokens.WILDCARD);
    }

    /**
     * Given a request object, return the desired returnKeys.
     *
     * @param requestObject the request object
     * @param wildcard      a value that represents the specification of all keys
     * @return the return keys
     */
    public static Set<String> getReturnKeys(final JSONObject requestObject, final String wildcard) {

        final JSONObject rexsterRequestObject = getRexsterRequest(requestObject);

        if (rexsterRequestObject != null) {
            try {
                final JSONArray jsonArrayOfReturnKeys = rexsterRequestObject.optJSONArray(Tokens.RETURN_KEYS);
                return getReturnKeys(jsonArrayOfReturnKeys, wildcard);
            } catch (Exception e) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Given an array of keys from the request object, return the desired returnKeys.
     * <p/>
     * Useful for when the return keys are being passed in as a parameter to an extension method.
     *
     * @param arrayOfKeys array of keys from the request object.
     * @param wildcard    a value that represents the specification of all keys
     * @return the return keys
     */
    public static Set<String> getReturnKeys(final JSONArray arrayOfKeys, final String wildcard) {
        Set<String> returnKeys = null;
        if (arrayOfKeys != null) {
            returnKeys = new HashSet<String>();

            for (int ix = 0; ix < arrayOfKeys.length(); ix++) {
                returnKeys.add(arrayOfKeys.optString(ix));
            }

            if (returnKeys.size() == 1 && returnKeys.contains(wildcard)) {
                returnKeys = null;
            }
        } else {
            returnKeys = null;
        }

        return returnKeys;
    }

    /**
     * Given a request object, return the show types.
     *
     * @param requestObject the request object
     * @return whether the user specified a show types (default is false)
     */
    public static boolean getShowTypes(final JSONObject requestObject) {
        final JSONObject rexsterRequestObject = getRexsterRequest(requestObject);

        if (rexsterRequestObject != null) {
            try {
                return rexsterRequestObject.getBoolean(Tokens.SHOW_TYPES);
            } catch (JSONException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Given a request object, return the start offset for paging purposes.
     *
     * @param requestObject the request object.
     * @return the start offset
     */
    public static Long getStartOffset(final JSONObject requestObject) {
        final Long offset = getOffset(requestObject, Tokens.START);
        if (null == offset)
            return 0l;
        else
            return offset;
    }

    /**
     * Given a request object, return the end offset for paging purposes.
     *
     * @param requestObject the request object.
     * @return the end offset
     */
    public static Long getEndOffset(final JSONObject requestObject) {
        final Long offset = getOffset(requestObject, Tokens.END);
        if (null == offset)
            return Long.MAX_VALUE;
        else
            return offset;
    }

    /**
     * Given a request object, determine if it has graph element properties.
     * <p/>
     * Graph element properties are those that do not have an underscore.
     *
     * @param requestObject the request object.
     * @return true if the element has properties and false otherwise.
     */
    public static boolean hasElementProperties(final JSONObject requestObject) {

        if (requestObject == null) {
            return false;
        }

        final Iterator keys = requestObject.keys();
        while (keys.hasNext()) {
            final String key = keys.next().toString();
            if (!key.startsWith(Tokens.UNDERSCORE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add has operations to the VertexQuery.
     */
    public static void buildQueryProperties(final JSONObject requestObject, final VertexQuery query) {
        final JSONArray propertyArray = requestObject.optJSONArray(Tokens._PROPERTIES);
        if (propertyArray != null) {
            try {
                final StringBuffer sb = new StringBuffer();
                for (int ix = 0; ix < propertyArray.length(); ix++) {
                    sb.append(propertyArray.optString(ix));

                    if (ix < propertyArray.length() - 1) {
                        sb.append(",");
                    }
                }

                final String propertyArgument = sb.toString();
                int startBracePlace = propertyArgument.indexOf('[');
                int endBracePlace = propertyArgument.indexOf(']');
                while (startBracePlace > -1 && endBracePlace > -1) {

                    // extract the elements of the array within the braces
                    final String triple = propertyArgument.substring(startBracePlace + 1, endBracePlace);
                    final String[] separated = triple.split(",");
                    final String[] tripleSplit = new String[3];
                    tripleSplit[0] = separated[0].trim();
                    tripleSplit[1] = separated[1].trim();

                    final String[] tripleValue = Arrays.copyOfRange(separated, 2, separated.length);
                    tripleSplit[2] = StringUtils.join(tripleValue, ',');

                    final String[] splitTripleValue = tripleSplit[2].split(" ");


                    final Compare c;
                    final String compareString = tripleSplit[1];
                    try {
                        c = Compare.fromString(compareString);
                    } catch (IllegalArgumentException iae) {
                        throw new WebApplicationException(Response.Status.BAD_REQUEST);
                    }

                    if (splitTripleValue.length == 1) {
                        query.has(tripleSplit[0], c, (Comparable) ElementHelper.getTypedPropertyValue(splitTripleValue[0], true));
                    } else {
                        final List l = new ArrayList(splitTripleValue.length);
                        for (String stv : splitTripleValue) {
                            l.add(ElementHelper.getTypedPropertyValue(stv, true));
                        }

                        if (c == Compare.EQUAL) {
                            query.has(tripleSplit[0], l.toArray(new Object[l.size()]));
                        } else {
                            query.hasNot(tripleSplit[0], l.toArray(new Object[l.size()]));
                        }
                    }

                    startBracePlace = propertyArgument.indexOf('[', endBracePlace);
                    endBracePlace = propertyArgument.indexOf(']', endBracePlace + 1);
                }

            } catch (WebApplicationException wae) {
                throw wae;
            } catch (Exception jse) {
                // the properties were not in the correct format of [[x,=,y],[x,<>,z]]
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
        }
    }

    private static Long getOffset(final JSONObject requestObject, final String offsetToken) {

        final JSONObject rexsterRequestObject = getRexsterRequest(requestObject);

        if (rexsterRequestObject != null) {

            if (rexsterRequestObject != null && rexsterRequestObject.has(Tokens.OFFSET)) {

                // returns zero if the value identified by the offsetToken is
                // not a number and the key is just present.
                if (rexsterRequestObject.optJSONObject(Tokens.OFFSET).has(offsetToken)) {
                    return rexsterRequestObject.optJSONObject(Tokens.OFFSET).optLong(offsetToken);
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
}

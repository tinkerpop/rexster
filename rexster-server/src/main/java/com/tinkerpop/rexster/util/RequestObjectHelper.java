package com.tinkerpop.rexster.util;

import com.tinkerpop.rexster.Tokens;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class RequestObjectHelper {

    public static final String DEFAULT_WILDCARD = "*";

    /**
     * Given a request object, return the fragment of JSON that deals with Rexster-reserved parameters.
     * <p/>
     * These parameters are the returnKeys, showTypes, and offset.
     *
     * @param requestObject the request object
     * @return the JSON
     */
    public static JSONObject getRexsterRequest(JSONObject requestObject) {
        return requestObject != null ? requestObject.optJSONObject(Tokens.REXSTER) : null;
    }

    /**
     * Given a request object, return the desired returnKeys. Utilizes the value of the DEFAULT_WILDCARD.
     *
     * @param requestObject the request object
     * @return the return keys
     */
    public static List<String> getReturnKeys(final JSONObject requestObject) {
        return getReturnKeys(requestObject, DEFAULT_WILDCARD);
    }

    /**
     * Given a request object, return the desired returnKeys.
     *
     * @param requestObject the request object
     * @param wildcard      a value that represents the specification of all keys
     * @return the return keys
     */
    public static List<String> getReturnKeys(final JSONObject requestObject, final String wildcard) {

        JSONObject rexsterRequestObject = getRexsterRequest(requestObject);

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
    public static List<String> getReturnKeys(final JSONArray arrayOfKeys, final String wildcard) {
        List<String> returnKeys = null;
        if (arrayOfKeys != null) {
            returnKeys = new ArrayList<String>();

            if (arrayOfKeys != null) {
                for (int ix = 0; ix < arrayOfKeys.length(); ix++) {
                    returnKeys.add(arrayOfKeys.optString(ix));
                }
            } else {
                returnKeys = null;
            }

            if (returnKeys != null && returnKeys.size() == 1
                    && returnKeys.get(0).equals(wildcard)) {
                returnKeys = null;
            }
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
        JSONObject rexsterRequestObject = getRexsterRequest(requestObject);

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
    public static Long getStartOffset(JSONObject requestObject) {
        Long offset = getOffset(requestObject, Tokens.START);
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
    public static Long getEndOffset(JSONObject requestObject) {
        Long offset = getOffset(requestObject, Tokens.END);
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
    public static boolean hasElementProperties(JSONObject requestObject) {

        if (requestObject == null) {
            return false;
        }

        Iterator keys = requestObject.keys();
        while (keys.hasNext()) {
            String key = keys.next().toString();
            if (!key.startsWith(Tokens.UNDERSCORE)) {
                return true;
            }
        }
        return false;
    }

    private static Long getOffset(JSONObject requestObject, String offsetToken) {

        JSONObject rexsterRequestObject = getRexsterRequest(requestObject);

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

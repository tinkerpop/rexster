package com.tinkerpop.rexster.util;

import com.tinkerpop.rexster.Tokens;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import sun.reflect.generics.tree.ReturnType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class RequestObjectHelper {

    public static final String DEFAULT_WILDCARD = "*";

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
     * @param wildcard a value that represents the specification of all keys
     * @return the return keys
     */
    public static List<String> getReturnKeys(final JSONObject requestObject, final String wildcard) {
        if (requestObject != null) {
            try {
                final JSONArray jsonArrayOfReturnKeys = ((JSONArray) requestObject.get(Tokens.RETURN_KEYS));
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
     *
     * Useful for when the return keys are being passed in as a parameter to an extension method.
     *
     * @param arrayOfKeys array of keys from the request object.
     * @param wildcard a value that represents the specification of all keys
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
        if (requestObject != null) {
            try {
                return requestObject.getBoolean(Tokens.SHOW_TYPES);
            } catch (JSONException e) {
                return false;
            }
        } else {
            return false;
        }
    }
}

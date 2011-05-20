package com.tinkerpop.rexster.util;

import com.tinkerpop.rexster.Tokens;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

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
        try {
            final JSONArray jsonArrayOfReturnKeys = ((JSONArray) requestObject.get(Tokens.RETURN_KEYS));

            List<String> returnKeys = null;
            if (jsonArrayOfReturnKeys != null) {
                returnKeys = new ArrayList<String>();

                if (jsonArrayOfReturnKeys != null) {
                    for (int ix = 0; ix < jsonArrayOfReturnKeys.length(); ix++) {
                        returnKeys.add(jsonArrayOfReturnKeys.optString(ix));
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
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Given a request object, return the show types.
     *
     * @param requestObject the request object
     * @return whether the user specified a show types (default is false)
     */
    public static boolean getShowTypes(final JSONObject requestObject) {
        try {
            return requestObject.getBoolean(Tokens.SHOW_TYPES);
        } catch (JSONException e) {
            return false;
        }
    }
}

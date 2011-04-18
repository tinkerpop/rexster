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

    /**
     * Given a request object, return the desired returnKeys.
     *
     * @param requestObject the request object
     * @return the return keys
     */
    public static List<String> getReturnKeys(final JSONObject requestObject) {
        try {
            final List<String> keys = new ArrayList<String>();
            final JSONArray array = ((JSONArray) requestObject.get(Tokens.RETURN_KEYS));
            for (int i = 0; i < array.length(); i++) {
                keys.add(array.getString(i));
            }
            return keys;
        } catch (JSONException e) {
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

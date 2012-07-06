package com.tinkerpop.rexster.extension;

import com.tinkerpop.rexster.Tokens;
import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for extensions.
 */
public abstract class AbstractRexsterExtension implements RexsterExtension {

    /**
     * By default this returns true.  Overriding classes should evaluate the configuration to determine
     * if it is correct.
     */
    public boolean isConfigurationValid(ExtensionConfiguration extensionConfiguration) {
        return true;
    }

    protected JSONObject generateErrorJson() {
        return this.generateErrorJson(null);
    }

    /**
     * Generates a standard JSON object with error information.
     *
     * @param api Adds an API element to the error output.  If null the key will not be added.
     */
    protected JSONObject generateErrorJson(JSONObject api) {
        Map map = new HashMap();
        map.put(Tokens.SUCCESS, false);

        if (api != null) {
            map.put(Tokens.API, api);
        }

        return new JSONObject(map);
    }
}

package com.tinkerpop.rexster.extension;

import com.tinkerpop.rexster.Tokens;
import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for extensions.
 */
public abstract class AbstractRexsterExtension implements RexsterExtension {
    protected static final String SCRIPT = "script";
    protected static final String RETURN_KEYS = "return_keys";

    /**
     * By default this reutrns true.  Overriding classes should evaluate the configuration to determine
     * if it is correct.
     */
    public boolean isConfigurationValid(ExtensionConfiguration extensionConfiguration) {
        return true;
    }

    protected JSONObject generateErrorJson() {
        Map map = new HashMap();
        map.put(Tokens.SUCCESS, false);

        JSONObject api = generateApiJson();
        if (api != null) {
            map.put(Tokens.API, generateApiJson());
        }

        return new JSONObject(map);
    }

    protected abstract JSONObject generateApiJson();

}

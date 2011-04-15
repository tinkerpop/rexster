package com.tinkerpop.rexster.extension;

import com.tinkerpop.rexster.Tokens;
import org.codehaus.jettison.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;

public class ExtensionMethod {
    private Method method;

    private ExtensionDefinition extensionDefinition;

    private ExtensionDescriptor extensionDescriptor;

    public ExtensionMethod(Method method, ExtensionDefinition extensionDefinition, ExtensionDescriptor extensionDescriptor) {
        this.method = method;
        this.extensionDefinition = extensionDefinition;
        this.extensionDescriptor = extensionDescriptor;
    }

    public Method getMethod() {
        return this.method;
    }

    public ExtensionDefinition getExtensionDefinition() {
        return this.extensionDefinition;
    }

    public ExtensionDescriptor getExtensionDescriptor() {
        return this.extensionDescriptor;
    }

    public JSONObject getExtensionApiAsJson() {

        JSONObject fullApi = null;
        HashMap<String, Object> map = new HashMap<String, Object>();
        if (this.extensionDescriptor != null) {
            map.put(Tokens.DESCRIPTION, this.extensionDescriptor.description());

            JSONObject api = null;
            if (this.extensionDescriptor.api().length > 0) {
                HashMap<String, String> innerMap = new HashMap<String, String>();

                for (ExtensionApi apiItem : this.extensionDescriptor.api()){
                    innerMap.put(apiItem.parameterName(), apiItem.description());
                }

                api = new JSONObject(innerMap);
            }

            if (api != null) {
                map.put(Tokens.PARAMETERS, api);
            }

            fullApi = new JSONObject(map);
        }

        return fullApi;
    }
}

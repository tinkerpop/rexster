package com.tinkerpop.rexster.extension;

import com.tinkerpop.rexster.Tokens;
import org.codehaus.jettison.json.JSONObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;

public class ExtensionMethod {
    private Method method;

    private ExtensionDefinition extensionDefinition;

    private ExtensionDescriptor extensionDescriptor;

    private RexsterExtension rexsterExtension;

    public ExtensionMethod(Method method, ExtensionDefinition extensionDefinition, ExtensionDescriptor extensionDescriptor, RexsterExtension rexsterExtension) {
        this.method = method;
        this.extensionDefinition = extensionDefinition;
        this.extensionDescriptor = extensionDescriptor;
        this.rexsterExtension = rexsterExtension;
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

    public RexsterExtension getRexsterExtension() {
        return this.rexsterExtension;
    }

    public JSONObject getExtensionApiAsJson() {

        JSONObject fullApi = null;
        HashMap<String, Object> map = new HashMap<String, Object>();
        if (this.extensionDescriptor != null) {
            map.put(Tokens.DESCRIPTION, this.extensionDescriptor.description());

            JSONObject api = null;
            HashMap<String, String> innerMap = new HashMap<String, String>();
            if (this.extensionDescriptor.apiBehavior() == ExtensionApiBehavior.DEFAULT
                    || this.extensionDescriptor.apiBehavior() == ExtensionApiBehavior.EXTENSION_DESCRIPTOR_ONLY) {

                if (this.extensionDescriptor.api().length > 0) {

                    for (ExtensionApi apiItem : this.extensionDescriptor.api()) {
                        innerMap.put(apiItem.parameterName(), apiItem.description());
                    }
                }
            }

            if (this.extensionDescriptor.apiBehavior() == ExtensionApiBehavior.DEFAULT
                    || this.extensionDescriptor.apiBehavior() == ExtensionApiBehavior.EXTENSION_PARAMETER_ONLY) {
                Annotation[][] parametersAnnotations = method.getParameterAnnotations();
                for (int ix = 0; ix < parametersAnnotations.length; ix++) {
                    Annotation[] annotation = parametersAnnotations[ix];

                    if (annotation != null && annotation[0] instanceof ExtensionRequestParameter) {
                        ExtensionRequestParameter extensionRequestParameter = (ExtensionRequestParameter) annotation[0];
                        innerMap.put(extensionRequestParameter.name(), extensionRequestParameter.description());
                    }
                }
            }

            if (!innerMap.isEmpty()) {
                api = new JSONObject(innerMap);
                map.put(Tokens.PARAMETERS, api);
            }

            fullApi = new JSONObject(map);
        }

        return fullApi;
    }
}

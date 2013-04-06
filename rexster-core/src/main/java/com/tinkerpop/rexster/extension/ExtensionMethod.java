package com.tinkerpop.rexster.extension;

import com.tinkerpop.rexster.Tokens;
import org.codehaus.jettison.json.JSONObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Holds the reflected method for the extension service call and its associated
 * extension attributes.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ExtensionMethod {

    /**
     * The service method.
     */
    private final Method method;

    /**
     * The definition of the extension.
     */
    private final ExtensionDefinition extensionDefinition;

    /**
     * The descriptor for the extension.
     */
    private final ExtensionDescriptor extensionDescriptor;

    /**
     * The extension class that owns this method.
     */
    private final RexsterExtension rexsterExtension;

    public ExtensionMethod(final Method method, final ExtensionDefinition extensionDefinition,
                           final ExtensionDescriptor extensionDescriptor,
                           final RexsterExtension rexsterExtension) {
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

    public RexsterExtension getRexsterExtension() {
        return this.rexsterExtension;
    }

    public JSONObject getExtensionApiAsJson() {

        JSONObject fullApi = null;
        final HashMap<String, Object> map = new HashMap<String, Object>();
        if (this.extensionDescriptor != null) {
            map.put(Tokens.DESCRIPTION, this.extensionDescriptor.description());

            JSONObject api = null;
            final HashMap<String, String> innerMap = new HashMap<String, String>();
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
                final Annotation[][] parametersAnnotations = method.getParameterAnnotations();
                for (int ix = 0; ix < parametersAnnotations.length; ix++) {
                    final Annotation[] annotation = parametersAnnotations[ix];

                    if (annotation != null && annotation.length > 0 && annotation[0] instanceof ExtensionRequestParameter) {
                        final ExtensionRequestParameter extensionRequestParameter = (ExtensionRequestParameter) annotation[0];
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

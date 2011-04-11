package com.tinkerpop.rexster.extension;

import java.lang.reflect.Method;

public class ExtensionMethod {
    private Method method;

    private ExtensionDefinition extensionDefinition;

    public ExtensionMethod(Method method, ExtensionDefinition extensionDefinition) {
        this.method = method;
        this.extensionDefinition = extensionDefinition;
    }

    public Method getMethod() {
        return method;
    }

    public ExtensionDefinition getExtensionDefinition() {
        return extensionDefinition;
    }
}

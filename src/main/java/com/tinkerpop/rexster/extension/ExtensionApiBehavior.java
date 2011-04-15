package com.tinkerpop.rexster.extension;

/**
 * Defines how the extension API information should be generated.
 */
public enum ExtensionApiBehavior {
    /**
     * Generates the API from the ExtensionDescriptor on the method only.
     */
    EXTENSION_DESCRIPTOR_ONLY,

    /**
     * Generates the API from the ExtensionRequestParameter for each parameter in the method only.
     */
    EXTENSION_PARAMETER_ONLY,

    /**
     * Generates the API from both the ExtensionDescriptor and ExtensionRequestParameters annotations,
     * where the ExtensionRequestParameters override keys presented by the ExtensionDescriptor.
     */
    DEFAULT
}

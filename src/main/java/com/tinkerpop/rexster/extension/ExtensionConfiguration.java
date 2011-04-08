package com.tinkerpop.rexster.extension;

/**
 * Holds information that details the configuration of an extension.
 */
public class ExtensionConfiguration {
    private String namespace;

    /**
     * Initializes a new ExtensionConfiguration object as taken from rexster.xml.
     * The namespace may be wildcarded to be one of the follows: *:*, namespace:*, namespace:extension
     */
    public ExtensionConfiguration(String namespace) {
        // must match this format *:*, namespace:*, namespace:extension
        if (!(namespace.matches("(\\w+|\\*):(\\w+|\\*)")
            && !(namespace.startsWith("*") && namespace.equals("*.*")))) {
            throw new IllegalArgumentException("The namespace must match the format of *:*, namespace:*, namespace:extension");
        }

        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }

    /**
     * Determines if the namespace and extension are allowed given the configuration of the graph in rexster.xml.
     */
    public boolean isExtensionAllowed(String namespace, String extensionName) {

        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("Namespace argument cannot be null or empty.");
        }

        if (extensionName == null || extensionName.isEmpty()) {
            throw new IllegalArgumentException("Extension name argument cannot be null or empty.");
        }

        boolean allowed = false;

        if (this.namespace.equals("*:*")) {
            allowed = true;
        } else if (this.namespace.equals(namespace + ":*")) {
            allowed = true;
        } else if (this.namespace.equals(namespace + ":" + extensionName)) {
            allowed = true;
        }

        return allowed;
    }

}

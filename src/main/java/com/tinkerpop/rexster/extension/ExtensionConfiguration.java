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
    public boolean isExtensionAllowed(ExtensionSegmentSet extensionSegmentSet){
        boolean allowed = false;

        if (this.namespace.equals("*:*")) {
            allowed = true;
        } else if (this.namespace.equals(extensionSegmentSet.getNamespace() + ":*")) {
            allowed = true;
        } else if (this.namespace.equals(extensionSegmentSet.getNamespace() + ":" + extensionSegmentSet.getExtension())) {
            allowed = true;
        }

        return allowed;
    }

}

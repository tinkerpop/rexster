package com.tinkerpop.rexster.extension;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * Holder for the path to an extension.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ExtensionSegmentSet {

    private String namespace;

    private String extension;

    private String extensionMethod;

    public ExtensionSegmentSet(final String namespace, final String extension) {
        this(namespace, extension, "");
    }

    public ExtensionSegmentSet(final String namespace, final String extension, final String extensionMethod) {

        if (extensionMethod == null) {
            throw new IllegalArgumentException("extensionMethod");
        }

        if (namespace == null) {
            throw new IllegalArgumentException("namespace");
        }

        if (extension == null) {
            throw new IllegalArgumentException("extension");
        }

        this.namespace = namespace;
        this.extension = extension;
        this.extensionMethod = extensionMethod;
    }

    public ExtensionSegmentSet(final UriInfo uriInfo, final ExtensionPoint extensionPoint) {

        int start = 2;
        if (extensionPoint != ExtensionPoint.GRAPH) {
            start = 4;
        }

        List<PathSegment> pathSegments = uriInfo.getPathSegments();

        // the first item in the path is the graphname, the second is the namespace
        this.namespace = "";
        if (pathSegments.size() > start) {
            PathSegment namespacePathSegment = pathSegments.get(start);
            this.namespace = namespacePathSegment.getPath();
        }

        // the third item in the path is the extension
        this.extension = "";
        if (pathSegments.size() > start + 1) {
            PathSegment extensionPathSegment = pathSegments.get(start + 1);
            this.extension = extensionPathSegment.getPath();
        }

        this.extensionMethod = "";

        // the fourth item in the path is the extension...if it exists
        if (pathSegments.size() > start + 2) {
            PathSegment extensionMethodPathSegment = pathSegments.get(start + 2);
            this.extensionMethod = extensionMethodPathSegment.getPath();
        }
    }

    public boolean isValidFormat() {
        return !this.getNamespace().isEmpty() && !this.getExtension().isEmpty();
    }

    public String getNamespaceAndExtension() {
        String namespaceAndExtension = "";
        if (this.isValidFormat()) {
            namespaceAndExtension = this.getNamespace() + ":" + this.getExtension();
        }

        return namespaceAndExtension;
    }

    @Override
    public String toString() {
        return (this.getNamespace().isEmpty() ? "[parse error]" : this.getNamespace()) + ":" +
                (this.getExtension().isEmpty() ? "[parse error]" : this.getExtension()) + "+"
                + (this.getExtensionMethod().isEmpty() ? "*" : this.getExtensionMethod());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ExtensionSegmentSet that = (ExtensionSegmentSet) o;

        if (!extension.equals(that.extension)) return false;
        if (!extensionMethod.equals(that.extensionMethod)) return false;
        if (!namespace.equals(that.namespace)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = namespace.hashCode();
        result = 31 * result + extension.hashCode();
        result = 31 * result + extensionMethod.hashCode();
        return result;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getExtension() {
        return extension;
    }

    public String getExtensionMethod() {
        return extensionMethod;
    }
}

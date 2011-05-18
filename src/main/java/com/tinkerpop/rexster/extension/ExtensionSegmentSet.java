package com.tinkerpop.rexster.extension;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import java.util.List;

public class ExtensionSegmentSet {

    private String namespace;

    private String extension;

    private String extensionMethod;

    public ExtensionSegmentSet(String namespace, String extension) {
        this.namespace = namespace;
        this.extension = extension;
        this.extensionMethod = "";
    }

    public ExtensionSegmentSet(UriInfo uriInfo, ExtensionPoint extensionPoint) {

        int start = 1;
        if (extensionPoint != ExtensionPoint.GRAPH) {
            start = 3;
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

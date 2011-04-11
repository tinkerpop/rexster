package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.rexster.extension.ExtensionConfiguration;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionSegmentSet;
import com.tinkerpop.rexster.extension.RexsterExtension;
import com.tinkerpop.rexster.traversals.Traversal;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Holds a graph, its assigned traversals and packages.
 */
public class RexsterApplicationGraph {

    private static final Logger logger = Logger.getLogger(RexsterApplication.class);

    private Graph graph;
    private Map<String, Class<? extends Traversal>> loadedTraversals = new HashMap<String, Class<? extends Traversal>>();
    private String graphName;
    private Set<String> packageNames;
    private Set<ExtensionConfiguration> extensionConfigurations;

    public RexsterApplicationGraph(String graphName, Graph graph) {
        this.graphName = graphName;
        this.graph = graph;
    }

    public boolean hasPackages() {
        return this.packageNames != null && this.packageNames.size() > 0;
    }

    public boolean hasExtensions() {
        return this.extensionConfigurations != null && this.extensionConfigurations.size() > 0;
    }

    public String getGraphName() {
        return graphName;
    }

    public Map<String, Class<? extends Traversal>> getLoadedTraversals() {
        return loadedTraversals;
    }

    public void setLoadedTraversals(Map<String, Class<? extends Traversal>> loadedTraversals) {
        this.loadedTraversals = loadedTraversals;
    }

    public Graph getGraph() {
        return graph;
    }

    public Set<String> getPackageNames() {
        return packageNames;
    }

    public boolean isExtensionAllowed(ExtensionSegmentSet extensionSegmentSet) {
        boolean allowed = false;
        for (ExtensionConfiguration extensionConfiguration : this.extensionConfigurations) {
            if (extensionConfiguration.isExtensionAllowed(extensionSegmentSet)) {
                allowed = true;
                break;
            }
        }

        return allowed;
    }

    public void loadExtensionsConfigurations(List<HierarchicalConfiguration> configurations) {
        this.extensionConfigurations = new HashSet<ExtensionConfiguration>();

        if (configurations != null) {
            for (HierarchicalConfiguration configuration : configurations) {
                String namespace = configuration.getString(Tokens.REXSTER_GRAPH_EXTENSION_NS);

                try {
                    this.getExtensionConfigurations().add(new ExtensionConfiguration(namespace));
                } catch (IllegalArgumentException iae) {
                    logger.warn("Extension defined with an invalid namespace: " + namespace
                        + ".  It will not be configured.", iae);
                }
            }
        }
    }

    public void loadPackageNames(String packageNameString) {
        if (packageNameString != null && packageNameString.length() > 0) {
            if (packageNameString.trim().equals(";")) {
                // allows configuration of the root package only (ie. gremlin)
                this.packageNames = new HashSet<String>();
                this.packageNames.add("");
            } else {
                // allows configuration of the root package plus anything else
                this.packageNames = new HashSet<String>(Arrays.asList(packageNameString.split(";")));
            }
        } else {
            // no packages when empty
            this.packageNames = new HashSet<String>();
        }
    }

    public Set<ExtensionConfiguration> getExtensionConfigurations() {
        return extensionConfigurations;
    }
}

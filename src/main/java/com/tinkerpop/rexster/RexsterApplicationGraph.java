package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.rexster.extension.*;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Holds a graph, its assigned traversals and packages.
 */
public class RexsterApplicationGraph {

    private static final Logger logger = Logger.getLogger(RexsterApplication.class);

    private Graph graph;
    private String graphName;
    private Set<ExtensionAllowed> extensionAllowables;
    private Set<ExtensionConfiguration> extensionConfigurations;

    public RexsterApplicationGraph(String graphName, Graph graph) {
        this.graphName = graphName;
        this.graph = graph;
    }

    public String getGraphName() {
        return graphName;
    }

    public Graph getGraph() {
        return graph;
    }

    /**
     * Determines if a particular extension is allowed given configured allowables from rexster.xml.
     *
     * Ensure that loadAllowableExtensions is called prior to this method.
     */
    public boolean isExtensionAllowed(ExtensionSegmentSet extensionSegmentSet) {
        boolean allowed = false;
        for (ExtensionAllowed extensionAllowed : this.extensionAllowables) {
            if (extensionAllowed.isExtensionAllowed(extensionSegmentSet)) {
                allowed = true;
                break;
            }
        }

        return allowed;
    }

    public ExtensionConfiguration findExtensionConfiguration(String namespace, String extensionName) {
        ExtensionConfiguration extensionConfigurationFound = null;

        if (this.extensionConfigurations != null) {
            for (ExtensionConfiguration extensionConfiguration : this.extensionConfigurations) {
                if (extensionConfiguration.getExtensionName().equals(extensionName)
                    && extensionConfiguration.getNamespace().equals(namespace)) {
                    extensionConfigurationFound = extensionConfiguration;
                    break;
                }
            }
        }

        return extensionConfigurationFound;
    }

    public void loadExtensionsConfigurations(List<HierarchicalConfiguration> extensionConfigurations) {
        this.extensionConfigurations = new HashSet<ExtensionConfiguration>();

        if (extensionConfigurations != null) {
            for (HierarchicalConfiguration configuration : extensionConfigurations) {
                String namespace = configuration.getString("namespace", "");
                String name = configuration.getString("name", "");
                HierarchicalConfiguration extensionConfig = configuration.configurationAt("configuration");

                if (!namespace.isEmpty() && !name.isEmpty() && extensionConfig != null) {
                    this.extensionConfigurations.add(new ExtensionConfiguration(namespace, name, extensionConfig));
                } else {
                    logger.warn("Extension [" + namespace + ":" + name + "] does not have a valid configuration.  Please check rexster.xml");
                }
            }
        }
    }

    /**
     * Loads a list of namespaces extension patterns that are allowed for this graph.
     */
    public void loadAllowableExtensions(List allowableNamespaces) {
        this.extensionAllowables = new HashSet<ExtensionAllowed>();

        if (allowableNamespaces != null) {
            for (int ix = 0; ix < allowableNamespaces.size(); ix++) {
                String namespace = allowableNamespaces.get(ix).toString();

                try {
                    this.getExtensionAllowables().add(new ExtensionAllowed(namespace));
                } catch (IllegalArgumentException iae) {
                    logger.warn("Extension defined with an invalid namespace: " + namespace
                        + ".  It will not be configured.", iae);
                }
            }
        }
    }

    public Set<ExtensionAllowed> getExtensionAllowables() {
        return this.extensionAllowables;
    }
}

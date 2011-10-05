package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.TransactionalGraph;
import com.tinkerpop.rexster.extension.ExtensionAllowed;
import com.tinkerpop.rexster.extension.ExtensionConfiguration;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionSegmentSet;
import com.tinkerpop.rexster.extension.RexsterExtension;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Holds a graph and its assigned extensions.
 */
public class RexsterApplicationGraph {

    private static final Logger logger = Logger.getLogger(RexsterApplicationGraph.class);

    private Graph graph;
    private String graphName;
    private Set<ExtensionAllowed> extensionAllowables;
    private Set<ExtensionConfiguration> extensionConfigurations;

    private static final ServiceLoader<? extends RexsterExtension> extensions = ServiceLoader.load(RexsterExtension.class);

    private static final Map<ExtensionPoint, JSONArray> hypermediaCache = new HashMap<ExtensionPoint, JSONArray>();

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

    public TransactionalGraph tryGetTransactionalGraph() {
        TransactionalGraph transactionalGraph = null;
        if (this.graph instanceof TransactionalGraph) {
            transactionalGraph = (TransactionalGraph) graph;
        }

        return transactionalGraph;
    }

    public boolean isTransactionalGraph() {
        TransactionalGraph transactionalGraph = tryGetTransactionalGraph();
        return transactionalGraph != null;
    }

    public void tryStartTransaction() {
        TransactionalGraph transactionalGraph = tryGetTransactionalGraph();
        if (transactionalGraph != null) {
            transactionalGraph.setMaxBufferSize(0);
            transactionalGraph.startTransaction();
        }
    }

    public void tryStopTransactionSuccess() {
        TransactionalGraph transactionalGraph = tryGetTransactionalGraph();
        if (transactionalGraph != null) {
            transactionalGraph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
            transactionalGraph.setMaxBufferSize(1);
        }
    }

    public void tryStopTransactionFailure() {
        TransactionalGraph transactionalGraph = tryGetTransactionalGraph();
        if (transactionalGraph != null) {
            transactionalGraph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
            transactionalGraph.setMaxBufferSize(1);
        }
    }

    public void trySetTransactionalModeAutomatic() {
        TransactionalGraph transactionalGraph = tryGetTransactionalGraph();
        if (transactionalGraph != null) {
            transactionalGraph.setMaxBufferSize(1);
        }
    }

    /**
     * Determines if a particular extension is allowed given configured allowables from rexster.xml.
     * <p/>
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
                    logger.warn("Graph [" + graphName + "] - Extension [" + namespace + ":" + name + "] does not have a valid configuration.  Please check rexster.xml");
                }
            }

            // cache the extension hypermedia once configurations are loaded and cached.
            this.initializeExtensionHypermediaCache();
        }
    }

    private void initializeExtensionHypermediaCache(){
        this.getExtensionHypermedia(ExtensionPoint.GRAPH);
        this.getExtensionHypermedia(ExtensionPoint.VERTEX);
        this.getExtensionHypermedia(ExtensionPoint.EDGE);
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

                    logger.info("Graph [" + graphName + "] - configured with allowable namespace [" + namespace + "]");
                } catch (IllegalArgumentException iae) {
                    logger.warn("Graph [" + graphName + "] - Extension defined with an invalid namespace: " + namespace
                            + ".  It will not be configured.");
                }
            }
        }
    }

    public Set<ExtensionAllowed> getExtensionAllowables() {
        return this.extensionAllowables;
    }

    protected JSONArray getExtensionHypermedia(ExtensionPoint extensionPoint) {

        JSONArray hypermediaLinks = new JSONArray();
        if (hypermediaCache.containsKey(extensionPoint)) {
            hypermediaLinks = hypermediaCache.get(extensionPoint);
        } else {

            for (RexsterExtension extension : extensions) {

                Class clazz = extension.getClass();
                ExtensionNaming extensionNaming = (ExtensionNaming) clazz.getAnnotation(ExtensionNaming.class);

                // initialize the defaults
                String currentExtensionNamespace = "g";
                String currentExtensionName = clazz.getName();

                if (extensionNaming != null) {

                    // naming annotation is present to try to override the defaults
                    // if the values are valid.
                    if (extensionNaming.name() != null && !extensionNaming.name().isEmpty()) {
                        currentExtensionName = extensionNaming.name();
                    }

                    // naming annotation is defaulted to "g" anyway but checking anyway to make sure
                    // no one tries to pull any funny business.
                    if (extensionNaming.namespace() != null && !extensionNaming.namespace().isEmpty()) {
                        currentExtensionNamespace = extensionNaming.namespace();
                    }
                }

                // test the configuration to see if the extension should even be available
                ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(
                        currentExtensionNamespace, currentExtensionName);

                if (this.isExtensionAllowed(extensionSegmentSet)) {
                    ExtensionConfiguration extensionConfig = this.findExtensionConfiguration(
                            currentExtensionNamespace, currentExtensionName);
                    RexsterExtension rexsterExtension = null;
                    try {
                        rexsterExtension = (RexsterExtension) clazz.newInstance();
                    } catch (Exception ex) {
                        logger.warn("Failed extension configuration check for " + currentExtensionNamespace + ":"
                                + currentExtensionName + "on graph " + graphName);
                    }

                    if (rexsterExtension != null) {
                        if (rexsterExtension.isConfigurationValid(extensionConfig)) {
                            Method[] methods = clazz.getMethods();
                            for (Method method : methods) {
                                ExtensionDescriptor descriptor = method.getAnnotation(ExtensionDescriptor.class);
                                ExtensionDefinition definition = method.getAnnotation(ExtensionDefinition.class);

                                if (definition != null && definition.extensionPoint() == extensionPoint) {
                                    String href = currentExtensionNamespace + "/" + currentExtensionName;
                                    if (!definition.path().isEmpty()) {
                                        href = href + "/" + definition.path();
                                    }

                                    HashMap hypermediaLink = new HashMap();
                                    hypermediaLink.put("href", href);
                                    hypermediaLink.put("method", definition.method().name());

                                    // descriptor is not a required annotation for extensions.
                                    if (descriptor != null) {
                                        hypermediaLink.put("title", descriptor.description());
                                    }

                                    hypermediaLinks.put(new JSONObject(hypermediaLink));
                                }
                            }
                        } else {
                            logger.warn("An extension [" + currentExtensionNamespace + ":" + currentExtensionName + "] does not have a valid configuration.  Check rexster.xml and ensure that the configuration section matches what the extension expects.");
                        }
                    }
                }
            }

            if (hypermediaLinks.length() == 0) {
                hypermediaCache.put(extensionPoint, null);
            } else {
                hypermediaCache.put(extensionPoint, hypermediaLinks);
            }
        }

        return hypermediaLinks;
    }
}

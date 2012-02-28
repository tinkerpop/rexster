package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.TransactionalGraph;
import com.tinkerpop.blueprints.pgm.util.wrappers.event.EventGraph;
import com.tinkerpop.blueprints.pgm.util.wrappers.readonly.ReadOnlyGraph;
import com.tinkerpop.rexster.extension.ExtensionAllowed;
import com.tinkerpop.rexster.extension.ExtensionApi;
import com.tinkerpop.rexster.extension.ExtensionApiBehavior;
import com.tinkerpop.rexster.extension.ExtensionConfiguration;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionRequestParameter;
import com.tinkerpop.rexster.extension.ExtensionSegmentSet;
import com.tinkerpop.rexster.extension.RexsterExtension;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

    private final Map<ExtensionPoint, List<HashMap<String, Object>>> hypermediaCache = new HashMap<ExtensionPoint, List<HashMap<String, Object>>>();
    private final Map<ExtensionSegmentSet, Boolean> extensionAllowedCache = new HashMap<ExtensionSegmentSet, Boolean>();

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

    public Graph getUnwrappedGraph() {
        return unwrapGraph(this.graph);
    }

    public static Graph unwrapGraph(Graph g) {
        Graph unwrapped = g;
        if (g instanceof ReadOnlyGraph) {
            unwrapped = unwrapGraph(((ReadOnlyGraph) g).getRawGraph());
        } else if (g instanceof EventGraph) {
            unwrapped = unwrapGraph(((EventGraph) g).getRawGraph());
        }

        return unwrapped;
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

        if (!extensionAllowedCache.containsKey(extensionSegmentSet)) {
            for (ExtensionAllowed extensionAllowed : this.extensionAllowables) {
                if (extensionAllowed.isExtensionAllowed(extensionSegmentSet)) {
                    allowed = true;
                    break;
                }
            }

            this.extensionAllowedCache.put(extensionSegmentSet, allowed);
        } else {
            allowed = this.extensionAllowedCache.get(extensionSegmentSet);
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

    private void initializeExtensionHypermediaCache() {
        this.getExtensionHypermedia(ExtensionPoint.GRAPH, "");
        this.getExtensionHypermedia(ExtensionPoint.VERTEX, "");
        this.getExtensionHypermedia(ExtensionPoint.EDGE, "");
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

    protected JSONArray getExtensionHypermedia(ExtensionPoint extensionPoint, String baseUri) {

        List<HashMap<String, Object>> hypermediaLinks = new ArrayList<HashMap<String, Object>>();
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

                                    final HashMap<String, Object> hypermediaLink = new HashMap<String, Object>();
                                    hypermediaLink.put("href", href);
                                    hypermediaLink.put("op", definition.method().name());
                                    hypermediaLink.put("namespace", currentExtensionNamespace);
                                    hypermediaLink.put("name", currentExtensionName);

                                    String path = definition.path();
                                    if (path != null && !path.isEmpty()) {
                                        hypermediaLink.put("path", path);
                                        hypermediaLink.put("title", currentExtensionNamespace + ":" + currentExtensionName + "-" + path);
                                    } else {
                                        hypermediaLink.put("title", currentExtensionNamespace + ":" + currentExtensionName);
                                    }

                                    // descriptor is not a required annotation for extensions.
                                    if (descriptor != null) {
                                        hypermediaLink.put("description", descriptor.description());
                                    }

                                    hypermediaLinks.add(hypermediaLink);

                                    final JSONArray queryStringParameters = new JSONArray();
                                    if (descriptor != null && (descriptor.apiBehavior() == ExtensionApiBehavior.DEFAULT
                                            || descriptor.apiBehavior() == ExtensionApiBehavior.EXTENSION_DESCRIPTOR_ONLY)) {
                                        for (final ExtensionApi extensionApi : descriptor.api()) {
                                            queryStringParameters.put(new HashMap<String, String>() {{
                                                put("name", extensionApi.parameterName());
                                                put("description", extensionApi.description());
                                            }});
                                        }
                                    }

                                    if (descriptor != null && (descriptor.apiBehavior() == ExtensionApiBehavior.DEFAULT
                                            || descriptor.apiBehavior() == ExtensionApiBehavior.EXTENSION_PARAMETER_ONLY)) {
                                        final Annotation[][] parametersAnnotationSets = method.getParameterAnnotations();
                                        for (Annotation[] parameterAnnotationSet : parametersAnnotationSets) {
                                            for (Annotation annotation : parameterAnnotationSet) {
                                                if (annotation instanceof ExtensionRequestParameter) {
                                                    final ExtensionRequestParameter extensionRequestParameter = (ExtensionRequestParameter) annotation;
                                                    queryStringParameters.put(new HashMap<String, String>() {{
                                                        put("name", extensionRequestParameter.name());
                                                        put("description", extensionRequestParameter.description());
                                                    }});
                                                }
                                            }
                                        }
                                    }

                                    if (queryStringParameters.length() > 0) {
                                        hypermediaLink.put("parameters", queryStringParameters);
                                    }
                                }
                            }
                        } else {
                            logger.warn("An extension [" + currentExtensionNamespace + ":" + currentExtensionName + "] does not have a valid configuration.  Check rexster.xml and ensure that the configuration section matches what the extension expects.");
                        }
                    }
                }
            }

            if (hypermediaLinks.size() == 0) {
                hypermediaCache.put(extensionPoint, null);
            } else {
                hypermediaCache.put(extensionPoint, hypermediaLinks);
            }
        }

        JSONArray composedLinks = null;

        if (hypermediaLinks != null) {
            composedLinks = new JSONArray();
            for (Map<String, Object> hypermediaLink : hypermediaLinks) {
                HashMap link = new HashMap();
                for (Map.Entry<String, Object> linkEntry : hypermediaLink.entrySet()) {
                    if (linkEntry.getKey().equals("href")) {
                        link.put(linkEntry.getKey(), baseUri + linkEntry.getValue());
                    } else {
                        link.put(linkEntry.getKey(), linkEntry.getValue());
                    }
                }

                composedLinks.put(new JSONObject(link));
            }
        }

        return composedLinks;
    }
}

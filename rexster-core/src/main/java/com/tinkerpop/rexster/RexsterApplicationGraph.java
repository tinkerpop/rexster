package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.util.wrappers.WrapperGraph;
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
 * Holds a graph and its assigned extensions. This wrapper is what is supplied to the RexsterResourceContext
 * to be passed to extensions and other Rexster resources.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexsterApplicationGraph {

    private static final Logger logger = Logger.getLogger(RexsterApplicationGraph.class);

    private static final String QUERY_STRING_PARAMETER_NAME = "name";
    private static final String QUERY_STRING_PARAMETER_DESCRIPTION = "description";

    private final Graph graph;
    private final String graphName;
    private Set<ExtensionAllowed> extensionAllowables;
    private Set<ExtensionConfiguration> extensionConfigurations;

    private static final ServiceLoader<? extends RexsterExtension> extensions = ServiceLoader.load(RexsterExtension.class);

    private final Map<ExtensionPoint, List<HashMap<String, Object>>> hypermediaCache = new HashMap<ExtensionPoint, List<HashMap<String, Object>>>();
    private final Map<ExtensionSegmentSet, Boolean> extensionAllowedCache = new HashMap<ExtensionSegmentSet, Boolean>();

    public RexsterApplicationGraph(final String graphName, final Graph graph) {
        this(graphName, graph, null);
    }

    public RexsterApplicationGraph(final String graphName, final Graph graph,
                                   final HierarchicalConfiguration graphConfig) {
        this(graphName, graph, graphConfig == null ? null : graphConfig.getList(Tokens.REXSTER_GRAPH_EXTENSIONS_ALLOWS_PATH),
                graphConfig == null ? null : graphConfig.configurationsAt(Tokens.REXSTER_GRAPH_EXTENSIONS_PATH));
    }

    public RexsterApplicationGraph(final String graphName, final Graph graph, final List<String> allowableNamespaces,
                                   final List<HierarchicalConfiguration> extensionConfigurations) {
        this.graphName = graphName;
        this.graph = graph;
        this.loadAllowableExtensions(allowableNamespaces);
        this.loadExtensionsConfigurations(extensionConfigurations);
    }

    /**
     * Gets the name of the graph as supplied in rexster.xml as part of the configuration.
     *
     * @return the name of the graph
     */
    public String getGraphName() {
        return graphName;
    }

    /**
     * Gets the graph instance itself.
     *
     * @return the graph instance.
     */
    public Graph getGraph() {
        return graph;
    }

    /**
     * If this graph is an instance of WrapperGraph, this method recursively unwraps the graph to get its
     * base implementation.
     *
     * For purposes of Rexster, graph wrapping occurs when the graph is marked as read-only in rexster.xml.
     *
     * @return the unwrapped graph.
     */
    public Graph getUnwrappedGraph() {
        return unwrapGraph(this.graph);
    }

    /**
     * Helper method that will attempt to get a transactional graph instance if the graph can be cast to such.
     *
     * @return the transactional graph or null if it is not transactional
     */
    public TransactionalGraph tryGetTransactionalGraph() {
        TransactionalGraph transactionalGraph = null;
        if (this.graph.getFeatures().supportsTransactions
                && this.graph instanceof TransactionalGraph) {
            transactionalGraph = (TransactionalGraph) graph;
        }

        return transactionalGraph;
    }

    /**
     * Determines if the graph is transactional or not.
     *
     * @return true if transactional and false otherwise.
     */
    public boolean isTransactionalGraph() {
        final TransactionalGraph transactionalGraph = tryGetTransactionalGraph();
        return transactionalGraph != null;
    }

    /**
     * Stops a transaction with success if the graph is transactional.  If the graph is not transactional,
     * the method does nothing.
     */
    public void tryCommit() {
        final TransactionalGraph transactionalGraph = tryGetTransactionalGraph();
        if (transactionalGraph != null) {
            // will leave this as stopTransaction until we completely remove it from blueprints so as to get as
            // much backward compatibility as we can
            //transactionalGraph.commit();
            transactionalGraph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
        }
    }

    /**
     * Stops a transaction with failure if the graph is transactional.  If the graph is not transactional,
     * the method does nothing.
     */
    public void tryRollback() {
        final TransactionalGraph transactionalGraph = tryGetTransactionalGraph();
        if (transactionalGraph != null) {
            // will leave this as stopTransaction until we completely remove it from blueprints so as to get as
            // much backward compatibility as we can
            //transactionalGraph.rollback();
            transactionalGraph.stopTransaction(TransactionalGraph.Conclusion.FAILURE);
        }
    }

    /**
     * Determines if a particular extension is allowed given configured allowables from rexster.xml. Ensure
     * that loadAllowableExtensions is called prior to this method.
     */
    public boolean isExtensionAllowed(final ExtensionSegmentSet extensionSegmentSet) {
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

    /**
     * Gets an extension configuration given a namespace and extension name.
     */
    public ExtensionConfiguration findExtensionConfiguration(final String namespace, final String extensionName) {
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

    /**
     * Generally speaking this method should not be called directly.
     */
    void loadExtensionsConfigurations(final List<HierarchicalConfiguration> extensionConfigurations) {
        this.extensionConfigurations = new HashSet<ExtensionConfiguration>();

        if (extensionConfigurations != null) {
            for (HierarchicalConfiguration configuration : extensionConfigurations) {
                final String namespace = configuration.getString("namespace", "");
                final String name = configuration.getString("name", "");
                final HierarchicalConfiguration extensionConfig = configuration.configurationAt("configuration");

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

    /**
     * Loads a list of namespaces extension patterns that are allowed for this graph.  Generally speaking this
     * method should not be called directly.
     */
    void loadAllowableExtensions(final List allowableNamespaces) {
        this.extensionAllowables = new HashSet<ExtensionAllowed>();

        if (allowableNamespaces != null) {
            for (int ix = 0; ix < allowableNamespaces.size(); ix++) {
                final String namespace = allowableNamespaces.get(ix).toString();

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

    public Set<ExtensionConfiguration> getExtensionConfigurations() {
        return extensionConfigurations;
    }

    static Graph unwrapGraph(final Graph g) {
        return g instanceof WrapperGraph ? unwrapGraph(((WrapperGraph) g).getBaseGraph()) : g;
    }

    @Override
    public String toString() {
        return this.graphName + "-" + this.graph.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final RexsterApplicationGraph that = (RexsterApplicationGraph) o;

        if (!graphName.equals(that.graphName)) return false;
        if (!graph.getClass().equals(that.graph.getClass())) return false;

        for (ExtensionAllowed extensionAllowed : extensionAllowables) {
            if (!that.getExtensionAllowables().contains(extensionAllowed)) {
                return false;
            }
        }

        for (ExtensionConfiguration configuration : extensionConfigurations) {
            if (!that.getExtensionConfigurations().contains(configuration)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = graph.hashCode();
        result = 31 * result + graphName.hashCode();
        result = 31 * result + extensionAllowables.hashCode();
        result = 31 * result + extensionConfigurations.hashCode();
        return result;
    }

    protected JSONArray getExtensionHypermedia(final ExtensionPoint extensionPoint, final String baseUri) {
        final List<HashMap<String, Object>> hypermediaLinks = hypermediaCache.containsKey(extensionPoint) ?
            hypermediaCache.get(extensionPoint) : createHyperMediaLinks(extensionPoint);
        return getComposedLinks(baseUri, hypermediaLinks);
    }

    private List<HashMap<String, Object>> createHyperMediaLinks(final ExtensionPoint extensionPoint) {
        final List<HashMap<String, Object>> hypermediaLinks = new ArrayList<HashMap<String, Object>>();
        for (RexsterExtension extension : extensions) {

            final Class clazz = extension.getClass();
            final ExtensionNaming extensionNaming = (ExtensionNaming) clazz.getAnnotation(ExtensionNaming.class);

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

            final String currentNamespaceAndName = makeExtensionName(currentExtensionNamespace, currentExtensionName);

            // test the configuration to see if the extension should even be available
            final ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(
                    currentExtensionNamespace, currentExtensionName);

            if (this.isExtensionAllowed(extensionSegmentSet)) {
                final ExtensionConfiguration extensionConfig = this.findExtensionConfiguration(
                        currentExtensionNamespace, currentExtensionName);
                RexsterExtension rexsterExtension = null;
                try {
                    rexsterExtension = (RexsterExtension) clazz.newInstance();
                } catch (Exception ex) {
                    logger.warn(String.format("Failed extension configuration check for %s on graph %s",
                            currentNamespaceAndName, graphName));
                }

                if (rexsterExtension != null) {
                    if (rexsterExtension.isConfigurationValid(extensionConfig)) {
                        final Method[] methods = clazz.getMethods();
                        for (Method method : methods) {
                            final ExtensionDescriptor descriptor = method.getAnnotation(ExtensionDescriptor.class);
                            final ExtensionDefinition definition = method.getAnnotation(ExtensionDefinition.class);

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

                                final String path = definition.path();
                                if (path != null && !path.isEmpty()) {
                                    hypermediaLink.put("path", path);
                                    hypermediaLink.put("title", currentNamespaceAndName + "-" + path);
                                } else {
                                    hypermediaLink.put("title", currentNamespaceAndName);
                                }

                                // descriptor is not a required annotation for extensions.
                                if (descriptor != null) {
                                    hypermediaLink.put("description", descriptor.description());
                                }

                                final JSONArray queryStringParameters = buildQueryStringParameters(method, descriptor);
                                if (queryStringParameters.length() > 0) {
                                    hypermediaLink.put("parameters", queryStringParameters);
                                }

                                hypermediaLinks.add(hypermediaLink);
                            }
                        }
                    } else {
                        logger.warn(String.format("An extension [%s] does not have a valid configuration.  Check rexster.xml and ensure that the configuration section matches what the extension expects.",
                                currentNamespaceAndName));
                    }
                }
            }
        }

        // only put something in the cache if there are actually links to
        if (hypermediaLinks.size() == 0) {
            hypermediaCache.put(extensionPoint, null);
        } else {
            hypermediaCache.put(extensionPoint, hypermediaLinks);
        }

        return hypermediaLinks;
    }

    private String makeExtensionName(final String extensionNamespace, String extensionName) {
        return String.format("%s:%s", extensionNamespace, extensionName);
    }

    private JSONArray buildQueryStringParameters(final Method method, final ExtensionDescriptor descriptor) {
        final JSONArray queryStringParameters = new JSONArray();
        if (descriptor != null && (descriptor.apiBehavior() == ExtensionApiBehavior.DEFAULT
                || descriptor.apiBehavior() == ExtensionApiBehavior.EXTENSION_DESCRIPTOR_ONLY)) {
            for (final ExtensionApi extensionApi : descriptor.api()) {
                queryStringParameters.put(new HashMap<String, String>() {{
                    put(QUERY_STRING_PARAMETER_NAME, extensionApi.parameterName());
                    put(QUERY_STRING_PARAMETER_DESCRIPTION, extensionApi.description());
                }});
            }
        }

        // possible override for the previous settings
        if (descriptor != null && (descriptor.apiBehavior() == ExtensionApiBehavior.DEFAULT
                || descriptor.apiBehavior() == ExtensionApiBehavior.EXTENSION_PARAMETER_ONLY)) {
            final Annotation[][] parametersAnnotationSets = method.getParameterAnnotations();
            for (Annotation[] parameterAnnotationSet : parametersAnnotationSets) {
                for (Annotation annotation : parameterAnnotationSet) {
                    if (annotation instanceof ExtensionRequestParameter) {
                        final ExtensionRequestParameter extensionRequestParameter = (ExtensionRequestParameter) annotation;
                        queryStringParameters.put(new HashMap<String, String>() {{
                            put(QUERY_STRING_PARAMETER_NAME, extensionRequestParameter.name());
                            put(QUERY_STRING_PARAMETER_DESCRIPTION, extensionRequestParameter.description());
                        }});
                    }
                }
            }
        }
        return queryStringParameters;
    }

    private JSONArray getComposedLinks(final String baseUri, final List<HashMap<String, Object>> hypermediaLinks) {
        JSONArray composedLinks = null;

        if (hypermediaLinks != null) {
            composedLinks = new JSONArray();
            for (Map<String, Object> hypermediaLink : hypermediaLinks) {
                final HashMap<String, Object> link = new HashMap<String, Object>();
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

    private void initializeExtensionHypermediaCache() {
        this.getExtensionHypermedia(ExtensionPoint.GRAPH, "");
        this.getExtensionHypermedia(ExtensionPoint.VERTEX, "");
        this.getExtensionHypermedia(ExtensionPoint.EDGE, "");
    }
}

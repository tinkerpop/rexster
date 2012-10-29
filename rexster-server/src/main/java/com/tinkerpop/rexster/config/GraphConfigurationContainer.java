package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.IndexableGraph;
import com.tinkerpop.blueprints.util.wrappers.readonly.ReadOnlyGraph;
import com.tinkerpop.blueprints.util.wrappers.readonly.ReadOnlyIndexableGraph;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class GraphConfigurationContainer {

    protected static final Logger logger = Logger.getLogger(GraphConfigurationContainer.class);

    private final Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();

    private final List<HierarchicalConfiguration> failedConfigurations = new ArrayList<HierarchicalConfiguration>();

    public GraphConfigurationContainer(final List<HierarchicalConfiguration> configurations) throws GraphConfigurationException {

        if (configurations == null) {
            throw new GraphConfigurationException("No graph configurations");
        }

        // create one graph for each configuration for each <graph> element
        final Iterator<HierarchicalConfiguration> it = configurations.iterator();
        while (it.hasNext()) {
            final HierarchicalConfiguration graphConfig = it.next();
            final String graphName = graphConfig.getString(Tokens.REXSTER_GRAPH_NAME, "");

            if (graphName.equals("")) {
                // all graphs must have a graph name
                logger.warn("Could not load graph " + graphName + ".  The graph-name element was not set.");
                this.failedConfigurations.add(graphConfig);
            } else {
                // check for duplicate graph configuration
                if (!this.graphs.containsKey(graphName)) {

                    if (graphConfig.getBoolean(Tokens.REXSTER_GRAPH_ENABLED, true)) {

                        // one graph failing initialization will not prevent the rest in
                        // their attempt to be created
                        try {
                            final Graph graph = getGraphFromConfiguration(graphConfig);
                            final RexsterApplicationGraph rag = new RexsterApplicationGraph(graphName, graph);

                            // loads extensions that are allowed to be served for this graph
                            final List extensionConfigs = graphConfig.getList(Tokens.REXSTER_GRAPH_EXTENSIONS_ALLOWS_PATH);
                            rag.loadAllowableExtensions(extensionConfigs);

                            // loads extension configuration for this graph
                            final List<HierarchicalConfiguration> extensionConfigurations = graphConfig.configurationsAt(Tokens.REXSTER_GRAPH_EXTENSIONS_PATH);
                            rag.loadExtensionsConfigurations(extensionConfigurations);

                            this.graphs.put(rag.getGraphName(), rag);

                            logger.info("Graph " + graphName + " - " + graph + " loaded");
                        } catch (GraphConfigurationException gce) {
                            logger.warn("Could not load graph " + graphName + ". Please check the XML configuration.");
                            logger.warn(gce.getMessage());

                            if (gce.getCause() != null) {
                                logger.warn(gce.getCause().getMessage());
                            }

                            failedConfigurations.add(graphConfig);
                        } catch (Exception e) {
                            logger.warn("Could not load graph " + graphName + ".", e);

                            failedConfigurations.add(graphConfig);
                        }
                    } else {
                        logger.info("Graph " + graphName + " - " + " not enabled and not loaded.");
                    }
                } else {
                    logger.warn("A graph with the name " + graphName + " was already configured.  Please check the XML configuration.");

                    failedConfigurations.add(graphConfig);
                }
            }
        }
    }

    public Map<String, RexsterApplicationGraph> getApplicationGraphs() {
        return this.graphs;
    }

    public List<HierarchicalConfiguration> getFailedConfigurations() {
        return this.failedConfigurations;
    }

    private Graph getGraphFromConfiguration(final HierarchicalConfiguration graphConfiguration) throws GraphConfigurationException {
        String graphConfigurationType = graphConfiguration.getString(Tokens.REXSTER_GRAPH_TYPE);
        final boolean isReadOnly = graphConfiguration.getBoolean(Tokens.REXSTER_GRAPH_READ_ONLY, false);

        if (graphConfigurationType.equals("neo4jgraph")) {
            graphConfigurationType = Neo4jGraphConfiguration.class.getName();
        } else if (graphConfigurationType.equals("orientgraph")) {
            graphConfigurationType = OrientGraphConfiguration.class.getName();
        } else if (graphConfigurationType.equals("tinkergraph")) {
            graphConfigurationType = TinkerGraphGraphConfiguration.class.getName();
        } else if (graphConfigurationType.equals("rexstergraph")) {
            graphConfigurationType = RexsterGraphGraphConfiguration.class.getName();
        } else if (graphConfigurationType.equals("memorystoresailgraph")) {
            graphConfigurationType = MemoryStoreSailGraphConfiguration.class.getName();
        } else if (graphConfigurationType.equals("nativestoresailgraph")) {
            graphConfigurationType = NativeStoreSailGraphConfiguration.class.getName();
        } else if (graphConfigurationType.equals("sparqlrepositorysailgraph")) {
            graphConfigurationType = SparqlRepositorySailGraphConfiguration.class.getName();
        } else if (graphConfigurationType.equals("dexgraph")) {
            graphConfigurationType = DexGraphConfiguration.class.getName();
        }

        final Graph graph;
        try {
            final Class clazz = Class.forName(graphConfigurationType, true, Thread.currentThread().getContextClassLoader());
            final GraphConfiguration graphConfigInstance = (GraphConfiguration) clazz.newInstance();
            Graph readWriteGraph = graphConfigInstance.configureGraphInstance(graphConfiguration);

            if (isReadOnly) {
                // the graph is configured to be readonly so wrap it up
                if (readWriteGraph instanceof IndexableGraph) {
                    graph = new ReadOnlyIndexableGraph((IndexableGraph) readWriteGraph);
                } else {
                    graph = new ReadOnlyGraph(readWriteGraph);
                }
            } else {
                graph = readWriteGraph;
            }

        } catch (NoClassDefFoundError err) {
            throw new GraphConfigurationException("GraphConfiguration [" + graphConfigurationType + "] could not instantiate a class [" + err.getMessage() + "].  Ensure that it is in Rexste's path.");
        } catch (Exception ex) {
            throw new GraphConfigurationException("GraphConfiguration could not be found or otherwise instantiated:." + graphConfigurationType, ex);
        }

        return graph;
    }
}

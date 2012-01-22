package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.IndexableGraph;
import com.tinkerpop.blueprints.pgm.impls.readonly.ReadOnlyGraph;
import com.tinkerpop.blueprints.pgm.impls.readonly.ReadOnlyIndexableGraph;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GraphConfigurationContainer {

    protected static final Logger logger = Logger.getLogger(GraphConfigurationContainer.class);

    private Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();

    private List<HierarchicalConfiguration> failedConfigurations = new ArrayList<HierarchicalConfiguration>();

    public GraphConfigurationContainer(List<HierarchicalConfiguration> configurations) throws GraphConfigurationException {

        if (configurations == null) {
            throw new GraphConfigurationException("No graph configurations");
        }

        // create one graph for each configuration for each <graph> element
        Iterator<HierarchicalConfiguration> it = configurations.iterator();
        while (it.hasNext()) {
            HierarchicalConfiguration graphConfig = it.next();
            String graphName = graphConfig.getString(Tokens.REXSTER_GRAPH_NAME, "");

            if (graphName.equals("")) {
                // all graphs must have a graph name
                logger.warn("Could not load graph " + graphName + ".  The graph-name element was not set.");
                this.failedConfigurations.add(graphConfig);
            } else {
                // check for duplicate graph configuration
                if (!this.graphs.containsKey(graphName)) {

                    boolean enabled = graphConfig.getBoolean(Tokens.REXSTER_GRAPH_ENABLED, true);

                    if (enabled) {

                        // one graph failing initialization will not prevent the rest in
                        // their attempt to be created
                        try {
                            Graph graph = getGraphFromConfiguration(graphConfig);
                            RexsterApplicationGraph rag = new RexsterApplicationGraph(graphName, graph);

                            // loads extensions that are allowed to be served for this graph
                            List extensionConfigs = graphConfig.getList(Tokens.REXSTER_GRAPH_EXTENSIONS_ALLOWS_PATH);
                            rag.loadAllowableExtensions(extensionConfigs);

                            // loads extension configuration for this graph
                            List<HierarchicalConfiguration> extensionConfigurations = graphConfig.configurationsAt(Tokens.REXSTER_GRAPH_EXTENSIONS_PATH);
                            rag.loadExtensionsConfigurations(extensionConfigurations);

                            this.graphs.put(rag.getGraphName(), rag);

                            logger.info("Graph " + graphName + " - " + graph + " loaded");
                        } catch (GraphConfigurationException gce) {
                            logger.warn("Could not load graph " + graphName + ". Please check the XML configuration.");

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

    private Graph getGraphFromConfiguration(HierarchicalConfiguration graphConfiguration) throws GraphConfigurationException {
        String graphConfigurationType = graphConfiguration.getString(Tokens.REXSTER_GRAPH_TYPE);
        boolean isReadOnly = graphConfiguration.getBoolean(Tokens.REXSTER_GRAPH_READ_ONLY, false);

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

        Graph graph = null;
        Class clazz = null;
        GraphConfiguration graphConfigInstance = null;
        try {
            clazz = Class.forName(graphConfigurationType, true, Thread.currentThread().getContextClassLoader());
            graphConfigInstance = (GraphConfiguration) clazz.newInstance();
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

        } catch (Exception ex) {
            throw new GraphConfigurationException("GraphConfiguration could not be found or otherwise instantiated:." + graphConfigurationType, ex);
        }

        return graph;
    }
}

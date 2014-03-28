package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.sail.SailGraph;
import com.tinkerpop.blueprints.impls.sail.impls.LinkedDataSailGraph;
import com.tinkerpop.blueprints.impls.sail.impls.MemoryStoreSailGraph;
import com.tinkerpop.blueprints.impls.sail.impls.NativeStoreSailGraph;
import com.tinkerpop.blueprints.impls.sail.impls.SparqlRepositorySailGraph;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.log4j.Logger;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public abstract class AbstractSailGraphConfiguration implements GraphConfiguration {
    private static final Logger logger = Logger.getLogger(AbstractSailGraphConfiguration.class);

    public static final String SAIL_TYPE_MEMORY = "memory";
    public static final String SAIL_TYPE_LINKED_DATA = "linked-data";
    public static final String SAIL_TYPE_NATIVE = "native";
    public static final String SAIL_TYPE_SPARQL = "sparql";

    protected String sailType;

    public Graph configureGraphInstance(final GraphConfigurationContext context) throws GraphConfigurationException {
        final String graphFile = context.getProperties().getString(Tokens.REXSTER_GRAPH_LOCATION, null);

        // get the <properties> section of the xml configuration
        final HierarchicalConfiguration graphSectionConfig = (HierarchicalConfiguration) context.getProperties();
        SubnodeConfiguration sailSpecificConfiguration = null;

        try {
            sailSpecificConfiguration = graphSectionConfig.configurationAt(Tokens.REXSTER_GRAPH_PROPERTIES);
        } catch (IllegalArgumentException iae) {
            // it's ok if this is missing.  it is optional depending on the settings
        }

        // graph-file and data-directory must be present for native, sparql, and linked-data
        if ((sailType.equals(SAIL_TYPE_NATIVE) || sailType.equals(SAIL_TYPE_SPARQL) || sailType.equals(SAIL_TYPE_LINKED_DATA))
                && (graphFile == null || graphFile.trim().length() == 0)) {
            throw new GraphConfigurationException("Check graph configuration. Missing or empty configuration element: " + Tokens.REXSTER_GRAPH_LOCATION);
        }

        try {
            SailGraph graph = null;

            if (this.sailType.equals(SAIL_TYPE_MEMORY)) {

                if (graphFile != null && !graphFile.isEmpty()) {
                    logger.warn("[" + MemoryStoreSailGraph.class.getSimpleName() + "] doesn't support the graph-file parameter.  It will be ignored.");
                }

                graph = new MemoryStoreSailGraph();
            } else if (this.sailType.equals(SAIL_TYPE_LINKED_DATA)) {
                RexsterApplicationGraph baseGraph = context.getGraphs().get(graphFile);
                if (null == baseGraph) {
                    throw new GraphConfigurationException("no such base graph for LinkedDataSail graph: " + graphFile);
                }
                if (!(baseGraph.getGraph() instanceof SailGraph)) {
                    throw new GraphConfigurationException("base graph for LinkedDataSail graph must be an instance of SailGraph");
                }
                if (baseGraph.getGraph() instanceof LinkedDataSailGraph) {
                    throw new GraphConfigurationException("LinkedDataSail graph based on another LinkedDataSail graph");
                }

                graph = new LinkedDataSailGraph((SailGraph) baseGraph.getGraph());
            } else if (this.sailType.equals(SAIL_TYPE_NATIVE)) {
                String configTripleIndices = "";
                if (sailSpecificConfiguration != null) {
                    configTripleIndices = sailSpecificConfiguration.getString("triple-indices", "");
                }

                if (configTripleIndices != null && configTripleIndices.trim().length() > 0) {
                    graph = new NativeStoreSailGraph(graphFile, configTripleIndices);
                } else {
                    graph = new NativeStoreSailGraph(graphFile);
                }
            } else if (this.sailType.equals(SAIL_TYPE_SPARQL)) {
                graph = new SparqlRepositorySailGraph(graphFile);
            }

            return graph;
        } catch (Exception ex) {
            throw new GraphConfigurationException(ex);
        }
    }

}

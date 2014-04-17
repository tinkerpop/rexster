package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.blueprints.util.wrappers.id.IdGraph;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class IdGraphConfiguration implements GraphConfiguration {

    public Graph configureGraphInstance(final GraphConfigurationContext context) throws GraphConfigurationException {

        final String graphFile = context.getProperties().getString(Tokens.REXSTER_GRAPH_LOCATION, null);

        if (graphFile == null || graphFile.trim().length() == 0) {
            throw new GraphConfigurationException("Check graph configuration. Missing or empty configuration element: " + Tokens.REXSTER_GRAPH_LOCATION);
        }

        RexsterApplicationGraph baseGraph = context.getGraphs().get(graphFile);
        if (null == baseGraph) {
            throw new GraphConfigurationException("no such base graph for IdGraph: " + graphFile);
        }

        if (!(baseGraph.getGraph() instanceof KeyIndexableGraph)) {
            throw new GraphConfigurationException("base graph for IdGraph must be an instance of KeyIndexableGraph");
        }

        // get the <properties> section of the xml configuration
        final HierarchicalConfiguration graphSectionConfig = (HierarchicalConfiguration) context.getProperties();
        SubnodeConfiguration specificConfiguration;

        try {
            specificConfiguration = graphSectionConfig.configurationAt(Tokens.REXSTER_GRAPH_PROPERTIES);
        } catch (IllegalArgumentException iae) {
            throw new GraphConfigurationException("Check graph configuration. Missing or empty configuration element: " + Tokens.REXSTER_GRAPH_PROPERTIES);
        }

        boolean supportVertexIds = specificConfiguration.getBoolean("supportVertexIds", true);
        boolean supportEdgeIds = specificConfiguration.getBoolean("supportEdgeIds", true);

        return new IdGraph((KeyIndexableGraph) baseGraph.getGraph(), supportVertexIds, supportEdgeIds);
    }
}

package com.tinkerpop.rexster;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds references and settings needed for a graph to be tested.
 * <p/>
 * This holder is created as a result of the configurations in the rexster-integration-test.xml file which
 * is used as configuration for the integration tests.  The type of graph will play a role in the tests
 * that will execute.
 */
public class GraphTestHolder {

    private String graphName;

    private String graphType;

    private Map<String, String> vertexIdSet = new HashMap<String, String>();

    private Map<String, String> edgeIdSet = new HashMap<String, String>();

    public GraphTestHolder(String graphName, String graphType) {
        this.graphName = graphName;
        this.graphType = graphType;
    }

    public String getGraphName() {
        return graphName;
    }

    public String getGraphType() {
        return graphType;
    }

    public Map<String, String> getVertexIdSet() {
        return this.vertexIdSet;
    }

    public Map<String, String> getEdgeIdSet() {
        return edgeIdSet;
    }
}

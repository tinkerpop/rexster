package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.Features;

import java.lang.reflect.Field;
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

    private final String graphName;

    private final String graphType;

    private final Map<String, String> vertexIdSet = new HashMap<String, String>();

    private final Map<String, String> edgeIdSet = new HashMap<String, String>();
    
    private final Features features = new Features();

    public GraphTestHolder(final String graphName, final String graphType, final Map<String, Boolean> graphFeatures) {
        this.graphName = graphName;
        this.graphType = graphType;

        for(Map.Entry<String, Boolean> entry : graphFeatures.entrySet()) {
            try {
                Field field = Features.class.getField(entry.getKey());
                field.set(features, entry.getValue().booleanValue());
            } catch (Exception e) {
                throw new RuntimeException("There is disparity between the features returned from Rexster and the Features class.");                
            }
        }
    }

    public String getGraphName() {
        return graphName;
    }

    public String getGraphType() {
        return graphType;
    }
    
    public Features getFeatures() {
        return features;
    }

    public Map<String, String> getVertexIdSet() {
        return this.vertexIdSet;
    }

    public Map<String, String> getEdgeIdSet() {
        return edgeIdSet;
    }
}

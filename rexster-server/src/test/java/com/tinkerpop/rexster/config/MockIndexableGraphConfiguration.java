package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import org.apache.commons.configuration.Configuration;

import java.util.Map;

public class MockIndexableGraphConfiguration implements GraphConfiguration {

    public Graph configureGraphInstance(Configuration properties,
                                        Map<String, RexsterApplicationGraph> graphs) throws GraphConfigurationException {
        return new TinkerGraph();
    }

}

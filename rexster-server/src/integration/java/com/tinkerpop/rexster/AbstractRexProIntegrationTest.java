package com.tinkerpop.rexster;

import com.tinkerpop.rexster.client.RexsterClientFactory;
import com.tinkerpop.rexster.server.DefaultRexsterApplication;
import com.tinkerpop.rexster.server.RexProRexsterServer;
import com.tinkerpop.rexster.server.RexsterApplication;
import com.tinkerpop.rexster.server.RexsterServer;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.After;
import org.junit.Before;

import java.util.List;

public abstract class AbstractRexProIntegrationTest {
    protected static final RexsterClientFactory factory = RexsterClientFactory.getInstance();

    protected RexsterServer rexsterServer;

    @Before
    public void setUp() throws Exception {
        final XMLConfiguration properties = new XMLConfiguration();
        properties.load(Application.class.getResourceAsStream("rexster-integration-test.xml"));
        rexsterServer = new RexProRexsterServer(properties);

        final List<HierarchicalConfiguration> graphConfigs = properties.configurationsAt(Tokens.REXSTER_GRAPH_PATH);
        final RexsterApplication application = new DefaultRexsterApplication(graphConfigs);
        rexsterServer.start(application);
    }

    @After
    public void tearDown() throws Exception {
        rexsterServer.stop();
    }
}

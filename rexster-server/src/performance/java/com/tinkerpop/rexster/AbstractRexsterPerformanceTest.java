package com.tinkerpop.rexster;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.tinkerpop.rexster.client.RexsterClient;
import com.tinkerpop.rexster.client.RexsterClientFactory;
import com.tinkerpop.rexster.client.RexsterClientTokens;
import com.tinkerpop.rexster.protocol.EngineController;
import com.tinkerpop.rexster.protocol.serializer.json.JSONSerializer;
import com.tinkerpop.rexster.server.HttpRexsterServer;
import com.tinkerpop.rexster.server.RexProRexsterServer;
import com.tinkerpop.rexster.server.RexsterApplication;
import com.tinkerpop.rexster.server.RexsterServer;
import com.tinkerpop.rexster.server.XmlRexsterApplication;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public abstract class AbstractRexsterPerformanceTest {

    protected static RexsterServer rexProServer;
    protected static RexsterServer httpServer;

    private static String host;
    private static String rexproPort;
    private static String httpPort;

    protected static Client httpClient;
    protected static final ClientConfig clientConfiguration = new DefaultClientConfig();
    protected static final ThreadLocal<RexsterClient> rexproClientMsgPackEmpty = new ThreadLocal<RexsterClient>();
    protected static final ThreadLocal<RexsterClient> rexproClientMsgPackGrateful = new ThreadLocal<RexsterClient>();
    protected static final ThreadLocal<RexsterClient> rexproClientJsonEmpty = new ThreadLocal<RexsterClient>();
    protected static final ThreadLocal<RexsterClient> rexproClientJsonGrateful = new ThreadLocal<RexsterClient>();

    static {
        // kill all but the worst logging
        BasicConfigurator.configure();
        LogManager.getRootLogger().setLevel(Level.ERROR);

        // don't need any of the grizzly stuff for this
        java.util.logging.LogManager.getLogManager().reset();

        EngineController.configure(-1, null);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        final XMLConfiguration properties = new XMLConfiguration();
        properties.load(AbstractRexsterPerformanceTest.class.getResourceAsStream("rexster-performance-test.xml"));
        rexProServer = new RexProRexsterServer(properties);
        httpServer = new HttpRexsterServer(properties);

        final List<HierarchicalConfiguration> graphConfigs = properties.configurationsAt(Tokens.REXSTER_GRAPH_PATH);
        final RexsterApplication application = new XmlRexsterApplication(graphConfigs);
        EngineController.configure(-1, null);
        rexProServer.start(application);
        httpServer.start(application);

        host = System.getProperty("host", "localhost");
        rexproPort = System.getProperty("rexproPort", "8184");
        httpPort = System.getProperty("httpPort", "8182");

        httpClient = Client.create(clientConfiguration);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        rexProServer.stop();
        httpServer.stop();
    }

    protected RexsterClient getRexsterClientMsgPackEmptyGraph() throws Exception {
        RexsterClient rexsterClient = rexproClientMsgPackEmpty.get();
        if (rexsterClient == null) {
            rexsterClient = RexsterClientFactory.open(host, Integer.parseInt(rexproPort), "emptygraph");
            rexproClientMsgPackEmpty.set(rexsterClient);
        }

        return rexsterClient;
    }

    protected RexsterClient getRexsterClientMsgPackGratefulGraph() throws Exception {
        RexsterClient rexsterClient = rexproClientMsgPackGrateful.get();
        if (rexsterClient == null) {
            rexsterClient = RexsterClientFactory.open(host, Integer.parseInt(rexproPort), "gratefulgraph");
            rexproClientMsgPackGrateful.set(rexsterClient);
        }

        return rexsterClient;
    }

    protected RexsterClient getRexsterClientJsonEmptyGraph() throws Exception {
        RexsterClient rexsterClient = rexproClientJsonEmpty.get();
        if (rexsterClient == null) {
            final Map<String,Object> config = new HashMap<String, Object>() {{
                put(RexsterClientTokens.CONFIG_GRAPH_NAME, "emptygraph");
                put(RexsterClientTokens.CONFIG_SERIALIZER, JSONSerializer.SERIALIZER_ID);
                put(RexsterClientTokens.CONFIG_HOSTNAME, host);
                put(RexsterClientTokens.CONFIG_PORT, Integer.parseInt(rexproPort));
            }};
            rexsterClient = RexsterClientFactory.open(config);
            rexproClientJsonEmpty.set(rexsterClient);
        }

        return rexsterClient;
    }

    protected RexsterClient getRexsterClientJsonGratefulGraph() throws Exception {
        RexsterClient rexsterClient = rexproClientJsonGrateful.get();
        if (rexsterClient == null) {
            final Map<String,Object> config = new HashMap<String, Object>() {{
                put(RexsterClientTokens.CONFIG_GRAPH_NAME, "gratefulgraph");
                put(RexsterClientTokens.CONFIG_SERIALIZER, JSONSerializer.SERIALIZER_ID);
                put(RexsterClientTokens.CONFIG_HOSTNAME, host);
                put(RexsterClientTokens.CONFIG_PORT, Integer.parseInt(rexproPort));
            }};
            rexsterClient = RexsterClientFactory.open(config);
            rexproClientJsonGrateful.set(rexsterClient);
        }

        return rexsterClient;
    }

    protected static String getRexProHost() {
        return host + ":" + rexproPort;
    }

    protected static String getHttpBaseUri() {
        return "http://" + host + ":" + httpPort + "/";
    }
}

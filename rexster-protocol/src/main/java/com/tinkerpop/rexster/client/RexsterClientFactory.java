package com.tinkerpop.rexster.client;

import com.tinkerpop.rexster.protocol.serializer.msgpack.MsgPackSerializer;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.LeaderFollowerNIOStrategy;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Creates RexsterClient instances.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class RexsterClientFactory {
    private static final Logger logger = Logger.getLogger(RexsterClientFactory.class);

    private static final BaseConfiguration defaultConfiguration = new BaseConfiguration() {{
        addProperty(RexsterClientTokens.CONFIG_HOSTNAME, "localhost");
        addProperty(RexsterClientTokens.CONFIG_PORT, 8184);
        addProperty(RexsterClientTokens.CONFIG_TIMEOUT_CONNECTION_MS, 8000);
        addProperty(RexsterClientTokens.CONFIG_TIMEOUT_WRITE_MS, 4000);
        addProperty(RexsterClientTokens.CONFIG_TIMEOUT_READ_MS, 16000);
        addProperty(RexsterClientTokens.CONFIG_MAX_ASYNC_WRITE_QUEUE_BYTES, 512000);
        addProperty(RexsterClientTokens.CONFIG_MESSAGE_RETRY_COUNT, 16);
        addProperty(RexsterClientTokens.CONFIG_MESSAGE_RETRY_WAIT_MS, 50);
        addProperty(RexsterClientTokens.CONFIG_LANGUAGE, "groovy");
        addProperty(RexsterClientTokens.CONFIG_GRAPH_OBJECT_NAME, "g");
        addProperty(RexsterClientTokens.CONFIG_GRAPH_NAME, null);
        addProperty(RexsterClientTokens.CONFIG_TRANSACTION, true);
        addProperty(RexsterClientTokens.CONFIG_SERIALIZER, MsgPackSerializer.SERIALIZER_ID);
    }};

    /**
     * The transport used by all instantiated RexsterClient objects.
     */
    private static TCPNIOTransport transport;

    /**
     * A list of all clients that were opened by the factory.
     */
    private static Set<RexsterClient> registeredClients = new HashSet<RexsterClient>();

    /**
     * Creates a RexsterClient instance with default settings for the factory using localhost and 8184 for the port.
     */
    public static RexsterClient open() throws Exception {
        return open(defaultConfiguration);
    }

    /**
     * Creates a RexsterClient instance with default settings for the factory using 8184 for the port.
     */
    public static RexsterClient open(final String host) throws Exception {
        final BaseConfiguration specificConfiguration = new BaseConfiguration();
        specificConfiguration.addProperty(RexsterClientTokens.CONFIG_HOSTNAME, host);

        return open(specificConfiguration);
    }

    /**
     * Creates a RexsterClient instance allowing override of host and port.
     */
    public static RexsterClient open(final String host, final int port) throws Exception {
        final BaseConfiguration specificConfiguration = new BaseConfiguration();
        specificConfiguration.addProperty(RexsterClientTokens.CONFIG_HOSTNAME, host);
        specificConfiguration.addProperty(RexsterClientTokens.CONFIG_PORT, port);

        return open(specificConfiguration);
    }

    /**
     * Creates a RexsterClient instance using 8184 for the port and allowing explicit specification of the
     * name of the graph to connect to.  Passing a value other than null will automatically establish a binding
     * variable called "g" for the graph name specified.
     */
    public static RexsterClient open(final String host, final String graphName) throws Exception {
        final BaseConfiguration specificConfiguration = new BaseConfiguration();
        specificConfiguration.addProperty(RexsterClientTokens.CONFIG_HOSTNAME, host);
        specificConfiguration.addProperty(RexsterClientTokens.CONFIG_GRAPH_NAME, graphName);

        return open(specificConfiguration);
    }

    /**
     * Creates a RexsterClient instance allowing explicit specification of the name of the graph to connect to.
     * Passing a value other than null will automatically establish a binding variable called "g" for the graph
     * name specified.
     */
    public static RexsterClient open(final String host, final int port, final String graphName) throws Exception {
        final BaseConfiguration specificConfiguration = new BaseConfiguration();
        specificConfiguration.addProperty(RexsterClientTokens.CONFIG_HOSTNAME, host);
        specificConfiguration.addProperty(RexsterClientTokens.CONFIG_PORT, port);
        specificConfiguration.addProperty(RexsterClientTokens.CONFIG_GRAPH_NAME, graphName);

        return open(specificConfiguration);
    }

    /**
     * Create a RexsterClient instance allowing override of all settings.
     */
    public static RexsterClient open(final Map<String,Object> configuration) throws Exception {
        return open(new MapConfiguration(configuration));
    }

    /**
     * Create a RexsterClient instance allowing override of all settings.
     */
    public static synchronized RexsterClient open(final Configuration specificConfiguration) throws Exception {

        final CompositeConfiguration jointConfig = new CompositeConfiguration();
        jointConfig.addConfiguration(specificConfiguration);
        jointConfig.addConfiguration(defaultConfiguration);

        final RexsterClient client = new RexsterClient(jointConfig, getTransport());
        registeredClients.add(client);

        logger.info(String.format("Create RexsterClient instance: [%s]", ConfigurationUtils.toString(jointConfig)));

        return client;
    }

    /**
     * Calling this method will release resources for all clients.
     */
    public synchronized static void releaseClients()  throws Exception {
        registeredClients.clear();

        if (transport != null) {
            transport.stop();
            transport = null;
        }
    }

    static synchronized void removeClient(final RexsterClient client) {
        registeredClients.remove(client);
    }

    private synchronized static TCPNIOTransport getTransport() throws Exception {
        if (transport == null) {
            final RexsterClientHandler handler = new RexsterClientHandler();
            final FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
            filterChainBuilder.add(new TransportFilter());
            filterChainBuilder.add(new RexProClientFilter());
            filterChainBuilder.add(handler);

            transport = TCPNIOTransportBuilder.newInstance().build();
            transport.setIOStrategy(LeaderFollowerNIOStrategy.getInstance());
            final ThreadPoolConfig workerThreadPoolConfig = ThreadPoolConfig.defaultConfig()
                    .setCorePoolSize(4)
                    .setMaxPoolSize(12);
            transport.setWorkerThreadPoolConfig(workerThreadPoolConfig);
            final ThreadPoolConfig kernalThreadPoolConfig = ThreadPoolConfig.defaultConfig()
                    .setCorePoolSize(4)
                    .setMaxPoolSize(12);
            transport.setKernelThreadPoolConfig(kernalThreadPoolConfig);
            transport.setProcessor(filterChainBuilder.build());
            transport.start();
        }

        return transport;
    }
}

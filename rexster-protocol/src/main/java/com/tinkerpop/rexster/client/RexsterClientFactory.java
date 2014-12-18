package com.tinkerpop.rexster.client;

import com.tinkerpop.rexster.protocol.filter.RexsterClientSslFilterHelper;
import com.tinkerpop.rexster.protocol.serializer.msgpack.MsgPackSerializer;
import com.tinkerpop.rexster.util.RexsterSslHelper;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.Processor;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.LeaderFollowerNIOStrategy;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

import java.util.Map;

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
        addProperty(RexsterClientTokens.CONFIG_ENABLE_CLIENT_SSL, false);
        addProperty(RexsterClientTokens.CONFIG_CLIENT_SSL_CONFIGURATION, null);
    }};

    /**
     * The transport used by all instantiated RexsterClient objects not using SSL.
     */
    private static TCPNIOTransport transport;

    /**
     * The transport used by all instantiated RexsterClientObjects that use SSL.
     */
    private static TCPNIOTransport secureTransport;

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
     * Creates a RexsterClient instance allowing the override of host and port and secured using
     * SSL properties found at the default location.
     */
    public static RexsterClient openSecure(final String host, final int port) throws Exception{
        final BaseConfiguration specificConfiguration = new BaseConfiguration();
        specificConfiguration.addProperty(RexsterClientTokens.CONFIG_HOSTNAME, host);
        specificConfiguration.addProperty(RexsterClientTokens.CONFIG_PORT, port);
        specificConfiguration.addProperty(RexsterClientTokens.CONFIG_ENABLE_CLIENT_SSL, true);

        return open(specificConfiguration);
    }

    /**
     * Creates a RexsterClient instance allowing the override of host and port and secured using
     * SSL properties found at the passed sslConfigFile location.
     */
    public static RexsterClient openSecure(final String host, final int port, String sslConfigFile) throws Exception{
        final BaseConfiguration specificConfiguration = new BaseConfiguration();
        specificConfiguration.addProperty(RexsterClientTokens.CONFIG_HOSTNAME, host);
        specificConfiguration.addProperty(RexsterClientTokens.CONFIG_PORT, port);
        specificConfiguration.addProperty(RexsterClientTokens.CONFIG_ENABLE_CLIENT_SSL, true);
        specificConfiguration.addProperty(RexsterClientTokens.CONFIG_CLIENT_SSL_CONFIGURATION, sslConfigFile);

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

        final CompositeConfiguration jointConfig = new CompositeConfiguration(defaultConfiguration);
        jointConfig.addConfiguration(specificConfiguration);

        final TCPNIOTransport tcpnioTransport =
                jointConfig.getBoolean(RexsterClientTokens.CONFIG_ENABLE_CLIENT_SSL) ? getSecureTransport(jointConfig) :
                        getTransport();
        final RexsterClient client = new RexsterClient(jointConfig, tcpnioTransport);

        logger.info(String.format("Create RexsterClient instance: [%s]", ConfigurationUtils.toString(jointConfig)));

        return client;
    }

    private synchronized static TCPNIOTransport getTransport() throws Exception {
        if (transport == null) {
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
            transport.setProcessor(getRexsterProcessor(false, null));
            transport.start();
        }

        return transport;
    }

    private synchronized static TCPNIOTransport getSecureTransport(Configuration sslConfiguration) throws Exception {
        //Recreating the transport is necessary as SSL settings may have changed
        if (secureTransport != null) {
            secureTransport.stop();
        }

        secureTransport = TCPNIOTransportBuilder.newInstance().build();
        secureTransport.setIOStrategy(LeaderFollowerNIOStrategy.getInstance());
        final ThreadPoolConfig workerThreadPoolConfig = ThreadPoolConfig.defaultConfig()
                .setCorePoolSize(4)
                .setMaxPoolSize(12);
        secureTransport.setWorkerThreadPoolConfig(workerThreadPoolConfig);
        final ThreadPoolConfig kernalThreadPoolConfig = ThreadPoolConfig.defaultConfig()
                .setCorePoolSize(4)
                .setMaxPoolSize(12);
        secureTransport.setKernelThreadPoolConfig(kernalThreadPoolConfig);
        secureTransport.setProcessor(getRexsterProcessor(true, sslConfiguration));
        secureTransport.start();

        return secureTransport;
    }

    private static Processor getRexsterProcessor(boolean useSsl, Configuration sslConfiguration) {
        final RexsterClientHandler handler = new RexsterClientHandler();
        final FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        if (useSsl) {
            filterChainBuilder.add(new RexsterClientSslFilterHelper(sslConfiguration).getSslFilter());
        }
        filterChainBuilder.add(new RexProClientFilter());
        filterChainBuilder.add(handler);
        return filterChainBuilder.build();
    }
}

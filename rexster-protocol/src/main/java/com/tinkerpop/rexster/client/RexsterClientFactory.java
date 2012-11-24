package com.tinkerpop.rexster.client;

import com.tinkerpop.rexster.protocol.filter.RexProMessageFilter;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;

import java.util.Map;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexsterClientFactory {

    private static final RexsterClientFactory factory = new RexsterClientFactory();

    private static final BaseConfiguration defaultConfiguration = new BaseConfiguration() {{
        addProperty(RexsterClientTokens.CONFIG_HOSTNAME, "localhost");
        addProperty(RexsterClientTokens.CONFIG_PORT, 8184);
        addProperty(RexsterClientTokens.CONFIG_TIMEOUT_CONNECTION_MS, 8000);
        addProperty(RexsterClientTokens.CONFIG_TIMEOUT_WRITE_MS, 4000);
        addProperty(RexsterClientTokens.CONFIG_TIMEOUT_READ_MS, 16000);
        addProperty(RexsterClientTokens.CONFIG_MAX_ASYNC_WRITE_QUEUE_BYTES, 512000);
        addProperty(RexsterClientTokens.CONFIG_MESSAGE_RETRY_COUNT, 16);
        addProperty(RexsterClientTokens.CONFIG_MESSAGE_RETRY_WAIT_MS, 50);
        addProperty(RexsterClientTokens.CONFIG_DESERIALIZE_ARRAY_SIZE_LIMIT, 4194304);
        addProperty(RexsterClientTokens.CONFIG_DESERIALIZE_MAP_SIZE_LIMIT, 2097152);
        addProperty(RexsterClientTokens.CONFIG_DESERIALIZE_RAW_SIZE_LIMIT, 134217728);
    }};

    private RexsterClientFactory() {

    }

    public static RexsterClientFactory getInstance() {
        return factory;
    }

    public RexsterClient createClient() throws Exception {
        return createClient(defaultConfiguration);
    }

    public RexsterClient createClient(final String host) throws Exception {
        final BaseConfiguration specificConfiguration = new BaseConfiguration();
        specificConfiguration.addProperty(RexsterClientTokens.CONFIG_HOSTNAME, host);

        final CompositeConfiguration jointConfig = new CompositeConfiguration();
        jointConfig.addConfiguration(specificConfiguration);
        jointConfig.addConfiguration(defaultConfiguration);
        return createClient(jointConfig);
    }

    public RexsterClient createClient(final String host, final int port) throws Exception {
        final BaseConfiguration specificConfiguration = new BaseConfiguration();
        specificConfiguration.addProperty(RexsterClientTokens.CONFIG_HOSTNAME, host);
        specificConfiguration.addProperty(RexsterClientTokens.CONFIG_PORT, port);

        final CompositeConfiguration jointConfig = new CompositeConfiguration();
        jointConfig.addConfiguration(specificConfiguration);
        jointConfig.addConfiguration(defaultConfiguration);
        return createClient(jointConfig);
    }

    public RexsterClient createClient(final Map<String,Object> configuration) throws Exception {
        return createClient(new MapConfiguration(configuration));
    }

    public RexsterClient createClient(final Configuration configuration) throws Exception {
        final RexsterClientHandler handler = new RexsterClientHandler();
        final FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new RexProMessageFilter());
        filterChainBuilder.add(handler);

        final TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setIOStrategy(SameThreadIOStrategy.getInstance());
        transport.setProcessor(filterChainBuilder.build());
        transport.start();

        final RexsterClient client = new RexsterClient(configuration, transport);
        handler.setClient(client);
        return client;
    }
}

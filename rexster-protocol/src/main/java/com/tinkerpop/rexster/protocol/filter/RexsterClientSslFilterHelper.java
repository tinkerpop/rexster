package com.tinkerpop.rexster.protocol.filter;

import javax.net.ssl.SSLContext;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.ssl.SSLFilter;

import com.tinkerpop.rexster.client.RexsterClientTokens;
import com.tinkerpop.rexster.util.RexsterSslHelper;

/**
 * Helps set up SSL for Rexpro clients by providing a {@link org.glassfish.grizzly.ssl.SSLFilter} that can be configured
 * to work with RexPro servers. The {@link com.tinkerpop.rexster.util.RexsterSslHelper} is used to build the {@link
 * javax.net.ssl.SSLContext}.
 */
public class RexsterClientSslFilterHelper {
    private static final Logger logger = Logger.getLogger(RexsterClientSslFilterHelper.class);

    /**
     * Configuration to pass to the {@link com.tinkerpop.rexster.util.RexsterSslHelper} when creating the {@link
     * javax.net.ssl.SSLContext}.
     */
    private final Configuration sslConfiguration;

    /**
     * Construct a new RexsterClientSslFilterHelper with a desired SSL configuration.
     *
     * @param sslConfiguration The SSL configuration to use.
     */
    public RexsterClientSslFilterHelper(Configuration sslConfiguration) {
        this.sslConfiguration = sslConfiguration;
    }

    /**
     * Construct a new RexsterClientSslFilterHelper with the location of a file containing the desired SSL
     * configuration.
     *
     * @param sslConfigurationFile The path of an XML formatted SSL configuration file to get SSL configuration
     * from.
     */
    public RexsterClientSslFilterHelper(String sslConfigurationFile) {
        final Configuration config = new BaseConfiguration();
        config.setProperty(RexsterClientTokens.CONFIG_CLIENT_SSL_CONFIGURATION, sslConfigurationFile);
        this.sslConfiguration = config;
    }

    /**
     * Get an {@link org.glassfish.grizzly.ssl.SSLFilter} based on the configuration this RexsterClientSslFilterHelper
     * was constructed with.
     *
     * @return An SSL filter that can be used to secure a RexPro client with SSL.
     */
    public SSLFilter getSslFilter() {
        final RexsterSslHelper clientSslHelper = new RexsterSslHelper(
                sslConfiguration.getString(RexsterClientTokens.CONFIG_CLIENT_SSL_CONFIGURATION), sslConfiguration);
        logger.info("Attempting to configure SSLFilter for RexPro client.");
        SSLContext sslContext = null;
        try {
            sslContext = clientSslHelper.createRexsterSslContext();
        } catch (final Exception e) {
            final String msg = "Failed to initialize SSLContext for RexPro client.";
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
        final SSLEngineConfigurator server =
                new SSLEngineConfigurator(sslContext).setNeedClientAuth(clientSslHelper.getNeedClientAuth())
                        .setWantClientAuth(clientSslHelper.getWantClientAuth()).setClientMode(false);
        final SSLEngineConfigurator client = new SSLEngineConfigurator(sslContext).setClientMode(true);

        logger.info("SSLFilter configured for RexPro client!");
        return new SSLFilter(server, client);
    }
}
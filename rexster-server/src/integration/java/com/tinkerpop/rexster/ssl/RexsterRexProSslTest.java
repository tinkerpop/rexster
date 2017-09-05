package com.tinkerpop.rexster.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.tinkerpop.rexster.Application;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.client.RexsterClient;
import com.tinkerpop.rexster.client.RexsterClientFactory;
import com.tinkerpop.rexster.client.RexsterClientTokens;
import com.tinkerpop.rexster.rexpro.AbstractRexProIntegrationTest;
import com.tinkerpop.rexster.server.RexProRexsterServer;
import com.tinkerpop.rexster.server.RexsterApplication;
import com.tinkerpop.rexster.server.RexsterServer;
import com.tinkerpop.rexster.server.XmlRexsterApplication;
import com.tinkerpop.rexster.util.RexsterSslHelper;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class RexsterRexProSslTest {
    private static final Logger logger = LoggerFactory.getLogger(RexsterRexProSslTest.class);
    private static final String REXSTER_INTEGRATION_TEST_DIR = "/tmp/rexster-integration-tests";
    private static final String CLIENT_KEYSTORE_PATH = "clientSslKeys.jks";
    private static final String UNTRUSTED_KEYSTORE = CLIENT_KEYSTORE_PATH + "_untrusted";
    private static final String REXSTER_SSL_BASE_CONFIGURATION = "rexster-integration-test-ssl.xml";
    private static final String PASSWORD = "password";

    private RexsterServer rexsterServer;
    private RexsterApplication application;

    @Before
    public void setUp() throws Exception {
        RexsterHttpSslTest.clean();

        new File(REXSTER_INTEGRATION_TEST_DIR).mkdirs();
        RexsterHttpSslTest.buildSslKeys();

        final XMLConfiguration properties = new XMLConfiguration();
        properties.load(Application.class.getResourceAsStream(REXSTER_SSL_BASE_CONFIGURATION));

        rexsterServer = new RexProRexsterServer(properties);
        final List<HierarchicalConfiguration> graphConfigs = properties.configurationsAt(Tokens.REXSTER_GRAPH_PATH);
        application = new XmlRexsterApplication(graphConfigs);
        rexsterServer.start(application);
    }

    @After
    public void tearDown() throws Exception {
        rexsterServer.stop();
        application.stop();
    }

    @Test
    @SuppressWarnings({"JUnitTestMethodWithNoAssertions"})
    public void testSslExceptionOccursIfSslIsEnabledForServerAndClientDoesntTrustServer() throws Exception {
        final BaseConfiguration config = getRexproClientBaseConfig();

        // remove all keystores from client so client does not trust server
        config.setProperty(RexsterSslHelper.KEY_SSL_KEY_STORE, "");
        config.setProperty(RexsterSslHelper.KEY_SSL_TRUST_STORE, "");

        final RexsterClient client = RexsterClientFactory.open(config);

        try {
            AbstractRexProIntegrationTest.getAvailableGraphs(client).toString();
            fail("Expected exception did not occur!");
        } catch (final Exception ex) {
            assertEquals("Could not send message.", ex.getCause().getMessage());
        }

        client.close();
    }

    @Test
    @SuppressWarnings({"JUnitTestMethodWithNoAssertions"})
    public void testOneWaySslWorksWhenClientTrustsServer() throws Exception {
        final BaseConfiguration config = getRexproClientBaseConfig();

        // remove the keystore to show it isnt necessary here (without client auth)
        config.setProperty(RexsterSslHelper.KEY_SSL_KEY_STORE, "");

        final RexsterClient client = RexsterClientFactory.open(config);
        logger.debug(AbstractRexProIntegrationTest.getAvailableGraphs(client).toString());
        client.close();
    }

    @Test
    public void testRequireClientAuthRequestThrowsExceptionIfServerDoesntTrustClient() throws Exception {
        reinitializeRexsterWithRequireClientAuth();

        final BaseConfiguration config = getRexproClientBaseConfig();

        // use 'untrusted' client certs so server should reject client
        config.setProperty(RexsterSslHelper.KEY_SSL_KEY_STORE, REXSTER_INTEGRATION_TEST_DIR + '/' + UNTRUSTED_KEYSTORE);
        config.setProperty(
                RexsterSslHelper.KEY_SSL_TRUST_STORE, REXSTER_INTEGRATION_TEST_DIR + '/' + UNTRUSTED_KEYSTORE);

        final RexsterClient client = RexsterClientFactory.open(config);

        try {
            AbstractRexProIntegrationTest.getAvailableGraphs(client).toString();
            fail("Expected exception did not occur!");
        } catch (final Exception ex) {
            assertEquals("Could not send message.", ex.getCause().getMessage());
        }

        client.close();
    }

    @Test
    @SuppressWarnings({"JUnitTestMethodWithNoAssertions"})
    public void testRequireClientAuthWorksWhenServerTrustsClient() throws Exception {
        reinitializeRexsterWithRequireClientAuth();

        // use the 'base' configuration in which client and server trust each other
        final RexsterClient client = RexsterClientFactory.open(getRexproClientBaseConfig());
        logger.debug(AbstractRexProIntegrationTest.getAvailableGraphs(client).toString());
        client.close();
    }

    /**
     * Gets configuration for the Rexpro client in which the client trusts the server and server will trust the client.

     * @return configuration for Rexpro client
     */
    private static BaseConfiguration getRexproClientBaseConfig() {
        final BaseConfiguration conf = new BaseConfiguration();
        conf.setProperty(RexsterClientTokens.CONFIG_ENABLE_CLIENT_SSL, true);
        conf.setProperty(RexsterSslHelper.KEY_SSL_KEY_STORE, REXSTER_INTEGRATION_TEST_DIR + '/' + CLIENT_KEYSTORE_PATH);
        conf.setProperty(
                RexsterSslHelper.KEY_SSL_TRUST_STORE, REXSTER_INTEGRATION_TEST_DIR + '/' + CLIENT_KEYSTORE_PATH);
        conf.setProperty(RexsterSslHelper.KEY_SSL_KEY_STORE_PASSWORD, PASSWORD);
        conf.setProperty(RexsterSslHelper.KEY_SSL_TRUST_STORE_PASSWORD, PASSWORD);
        conf.setProperty(RexsterClientTokens.CONFIG_MESSAGE_RETRY_COUNT, 1);

        return conf;
    }

    /**
     * Restarts the Rexster server and configures its SSL to require client authentication.
     */
    private void reinitializeRexsterWithRequireClientAuth() throws Exception {
        rexsterServer.stop();
        final XMLConfiguration properties = new XMLConfiguration();
        properties.load(Application.class.getResourceAsStream(REXSTER_SSL_BASE_CONFIGURATION));
        properties.setProperty("ssl.need-client-auth", "true");
        properties.setProperty("ssl.want-client-auth", "true");

        rexsterServer = new RexProRexsterServer(properties);

        final List<HierarchicalConfiguration> graphConfigs = properties.configurationsAt(Tokens.REXSTER_GRAPH_PATH);
        final RexsterApplication application = new XmlRexsterApplication(graphConfigs);
        rexsterServer.start(application);
    }
}

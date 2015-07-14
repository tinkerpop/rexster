package com.tinkerpop.rexster.ssl;

import static org.junit.Assert.fail;
import static javax.ws.rs.HttpMethod.GET;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.tinkerpop.rexster.AbstractResourceIntegrationTest;
import com.tinkerpop.rexster.Application;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.server.HttpRexsterServer;
import com.tinkerpop.rexster.server.RexsterApplication;
import com.tinkerpop.rexster.server.RexsterServer;
import com.tinkerpop.rexster.server.XmlRexsterApplication;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

/**
 * Tests Rexster HTTP SSL support. Note that the same keystore is used as both keystore and truststore for brevity, this
 * shouldn't be done in production. See {@code rexster-integration-test-ssl.xml} in the test resources dir for the SSL
 * configuration being used by Rexster.
 */
public class RexsterHttpSslTest {
    private static final Logger logger = LoggerFactory.getLogger(RexsterHttpSslTest.class);
    private static final String REXSTER_INTEGRATION_TEST_DIR = "/tmp/rexster-integration-tests";
    private static final String CLIENT_KEYSTORE_PATH = "clientSslKeys.jks";
    private static final String UNTRUSTED_KEYSTORE = CLIENT_KEYSTORE_PATH + "_untrusted";
    private static final String SERVER_KEYSTORE_PATH = "serverSslKeys.jks";
    private static final String CLIENT_CERT = "client.cert";
    private static final String SERVER_CERT = "server.cert";
    private static final String BASE_URI = "https://127.0.0.1:8182";
    private static final URI GRAPHS_URI = URI.create(BASE_URI + "/graphs");
    private static final String REXSTER_SSL_BASE_CONFIGURATION = "rexster-integration-test-ssl.xml";
    private static final String PASSWORD = "password";

    private RexsterServer rexsterServer;
    private RexsterApplication application;

    private final ClientConfig clientConfiguration = new DefaultClientConfig();
    private Client client;

    @Before
    public void setUp() throws Exception {
        clean();

        new File(REXSTER_INTEGRATION_TEST_DIR).mkdirs();
        buildSslKeys();

        final XMLConfiguration properties = new XMLConfiguration();
        properties.load(Application.class.getResourceAsStream(REXSTER_SSL_BASE_CONFIGURATION));

        rexsterServer = new HttpRexsterServer(properties);
        final List<HierarchicalConfiguration> graphConfigs = properties.configurationsAt(Tokens.REXSTER_GRAPH_PATH);
        application = new XmlRexsterApplication(graphConfigs);
        rexsterServer.start(application);

        client = Client.create(clientConfiguration);
    }

    @After
    public void tearDown() throws Exception {
        rexsterServer.stop();
        application.stop();
    }

    @Test
    public void testSslExceptionOccursIfSslIsEnabledForServerAndClientDoesntTrustServer()
            throws NoSuchAlgorithmException {
        final HTTPSProperties httpsProperties = new HTTPSProperties(DONT_VERIFY_HOSTNAME, SSLContext.getDefault());
        client.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, httpsProperties);

        // should fail because the client doesn't trust the server:
        final ClientRequest graphRequest = ClientRequest.create().build(GRAPHS_URI, GET);
        try {
            this.client.handle(graphRequest);
            fail("Expected exception did not occur.");
        } catch (ClientHandlerException e) {
            if (!e.getCause().getClass().equals(SSLHandshakeException.class)) {
                fail("Unexpected exception.");
            }
        }
    }

    @Test
    @SuppressWarnings({"JUnitTestMethodWithNoAssertions"})
    public void testOneWaySslWorksWhenClientTrustsServer() throws Exception {
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        final TrustManagerFactory trustManagerFactory = initAndGetClientTrustManagerFactory(CLIENT_KEYSTORE_PATH);
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

        final HTTPSProperties httpsProperties = new HTTPSProperties(DONT_VERIFY_HOSTNAME, sslContext);
        client.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, httpsProperties);

        // client trusts server, this should succeed:
        final ClientRequest graphRequest = ClientRequest.create().build(GRAPHS_URI, GET);
        this.client.handle(graphRequest);
    }

    @Test
    public void testRequireClientAuthRequestThrowsSocketExceptionIfServerDoesntTrustClient() throws Exception {
        reinitializeRexsterWithRequireClientAuth();

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        final TrustManagerFactory trustManagerFactory = initAndGetClientTrustManagerFactory(UNTRUSTED_KEYSTORE);
        final KeyManagerFactory keyManagerFactory = initAndGetClientKeyManagerFactory(UNTRUSTED_KEYSTORE);
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        final HTTPSProperties httpsProperties = new HTTPSProperties(DONT_VERIFY_HOSTNAME, sslContext);
        client.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, httpsProperties);

        final ClientRequest graphRequest = ClientRequest.create().build(GRAPHS_URI, GET);

        // server should throw an SSLHandshakeException internally and reset the connection:
        try {
            this.client.handle(graphRequest);
            fail("Expected exception did not occur.");
        } catch (ClientHandlerException e) {
            if (!e.getCause().getClass().equals(SocketException.class)) {
                final String errMsg = "Unexpected exception.";
                logger.error(errMsg, e);
                fail(errMsg);
            }
        }
    }

    @Test
    @SuppressWarnings({"JUnitTestMethodWithNoAssertions"})
    public void testRequireClientAuthWorksWhenServerTrustsClient() throws Exception {
        reinitializeRexsterWithRequireClientAuth();

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        final TrustManagerFactory trustManagerFactory = initAndGetClientTrustManagerFactory(CLIENT_KEYSTORE_PATH);
        final KeyManagerFactory keyManagerFactory = initAndGetClientKeyManagerFactory(CLIENT_KEYSTORE_PATH);
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        final HTTPSProperties httpsProperties = new HTTPSProperties(DONT_VERIFY_HOSTNAME, sslContext);
        client.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, httpsProperties);

        final ClientRequest graphRequest = ClientRequest.create().build(GRAPHS_URI, GET);

        // client and server both trust each other, this should succeed:
        this.client.handle(graphRequest);
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

        rexsterServer = new HttpRexsterServer(properties);

        final List<HierarchicalConfiguration> graphConfigs = properties.configurationsAt(Tokens.REXSTER_GRAPH_PATH);
        final RexsterApplication application = new XmlRexsterApplication(graphConfigs);
        rexsterServer.start(application);
    }

    /**
     * Generates a {@code TrustManagerFactory} that provides trust for the Rexster server.
     *
     * @return a {@code TrustManagerFactory} that can be used for SSL operations and trusts the Rexster server
     */
    private static TrustManagerFactory initAndGetClientTrustManagerFactory(String pathToStore)
            throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException {
        final TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

        final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

        FileInputStream trustStoreInputStream = null;
        try {
            trustStoreInputStream = new FileInputStream(REXSTER_INTEGRATION_TEST_DIR + '/' + pathToStore);
            trustStore.load(trustStoreInputStream, PASSWORD.toCharArray());
        } finally {
            if (trustStoreInputStream != null) {
                trustStoreInputStream.close();
            }
        }
        trustManagerFactory.init(trustStore);

        return trustManagerFactory;
    }

    /**
     * Generates a {@code KeyManagerFactory} loaded with the key store at the given path.
     *
     * @param keyStorePath path to the keystore with which to load the returned {@code KeyManagerFactory}
     * @return a {@code KeyManagerFactory} that can be used for SSL operations
     */
    private static KeyManagerFactory initAndGetClientKeyManagerFactory(final String keyStorePath) throws Exception {
        final KeyManagerFactory keyManagerFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

        final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream keyStoreInputStream = null;
        try {
            keyStoreInputStream = new FileInputStream(REXSTER_INTEGRATION_TEST_DIR + '/' + keyStorePath);
            keyStore.load(keyStoreInputStream, PASSWORD.toCharArray());
        } finally {
            if (keyStoreInputStream != null) {
                keyStoreInputStream.close();
            }
        }
        keyManagerFactory.init(keyStore, PASSWORD.toCharArray());

        return keyManagerFactory;
    }

    /**
     * Generates SSL keys for the client and server. Imports the client cert into the server keystore and the server
     * cert into the client keystore. Also generates a client keystore that imports the sever cert but doesnt have its
     * cert imported into the server keystore - the 'untrusted client'. Note that a single keystore serves as both
     * keystore and trust store in these tests
     */
    public static void buildSslKeys() throws IOException, InterruptedException {
        final String[] generateClientKeyStore =
                {"keytool", "-genkey", "-v", "-alias", "client", "-keypass", PASSWORD, "-keystore",
                        CLIENT_KEYSTORE_PATH, "-storepass", PASSWORD, "-storetype", "jks", "-dname",
                        "CN=client, O=client, C=US", "-keyalg", "RSA"};

        final String[] exportClientCertificate =
                {"keytool", "-export", "-v", "-alias", "client", "-file", CLIENT_CERT, "-rfc", "-keystore",
                        CLIENT_KEYSTORE_PATH, "-storepass", PASSWORD, "-storetype", "jks"};

        final String[] generateServerKeyStore =
                {"keytool", "-genkey", "-v", "-alias", "server", "-keypass", PASSWORD, "-keystore",
                        SERVER_KEYSTORE_PATH, "-storepass", PASSWORD, "-storetype", "jks", "-dname",
                        "CN=server, O=server, C=US", "-keyalg", "RSA"};

        final String[] exportServerCertificate =
                {"keytool", "-export", "-v", "-alias", "server", "-file", SERVER_CERT, "-rfc", "-keystore",
                        SERVER_KEYSTORE_PATH, "-storepass", PASSWORD, "-storetype", "jks"};

        final String[] importServerCertToClient =
                {"keytool", "-import", "-v", "-alias", "server", "-noprompt", "-file", SERVER_CERT, "-keystore",
                        CLIENT_KEYSTORE_PATH, "-storepass", PASSWORD, "-storetype", "jks"};

        final String[] importClientCertToServer =
                {"keytool", "-import", "-v", "-alias", "client", "-noprompt", "-file", CLIENT_CERT, "-keystore",
                        SERVER_KEYSTORE_PATH, "-storepass", PASSWORD, "-storetype", "jks"};

        final String[] generateUntrustedClientKeys =
                {"keytool", "-genkey", "-v", "-alias", "untrusted", "-keypass", PASSWORD, "-keystore",
                        UNTRUSTED_KEYSTORE, "-storepass", PASSWORD, "-storetype", "jks", "-dname",
                        "CN=untrusted, O=client, C=US", "-keyalg", "RSA"};

        final String[] importServerCertToUntrustedClientKeys =
                {"keytool", "-import", "-v", "-alias", "server", "-noprompt", "-file", SERVER_CERT, "-keystore",
                        UNTRUSTED_KEYSTORE, "-storepass", PASSWORD, "-storetype", "jks"};

        final String[][] keystoreGenerationCommands =
                {generateClientKeyStore, exportClientCertificate, generateServerKeyStore, exportServerCertificate,
                        importServerCertToClient, importClientCertToServer, generateUntrustedClientKeys,
                        importServerCertToUntrustedClientKeys};

        for (final String[] command : keystoreGenerationCommands) {
            final ProcessBuilder pb = new ProcessBuilder();
            pb.command(command);
            pb.directory(new File(REXSTER_INTEGRATION_TEST_DIR));
            pb.redirectErrorStream();

            final Process process = pb.start();
            final InputStream sterr = process.getErrorStream();

            final BufferedReader reader = new BufferedReader(new InputStreamReader(sterr));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug(line);
                }
            } finally {
                reader.close();
            }
            process.waitFor();
        }
    }

    /**
     * Deletes the test directory where we store the SSL keys used for testing.
     */
    public static void clean() {
        AbstractResourceIntegrationTest.removeDirectory(new File(REXSTER_INTEGRATION_TEST_DIR));
    }

    /**
     * Ignoring host name verification by using this {@code HostnameVerifier}.
     */
    private static final HostnameVerifier DONT_VERIFY_HOSTNAME = new HostnameVerifier() {
        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    };
}

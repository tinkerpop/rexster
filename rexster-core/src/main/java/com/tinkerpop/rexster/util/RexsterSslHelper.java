package com.tinkerpop.rexster.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

/**
 * Assists in various parts of SSL setup for Rexster, particularly creating the SSLContext from a configuration. The
 * default configuration is for clients relying on the default JVM TrustStore, it is expected that servers will specify
 * at least some SSL properties.
 */
public class RexsterSslHelper {
    public static final String KEY_HTTP_SSL_ENABLED = "http.enable-ssl";
    public static final String KEY_REXPRO_SSL_ENABLED = "rexpro.enable-ssl";
    public static final String KEY_SSL_PROTOCOL = "ssl.protocol";
    public static final String KEY_SSL_TRUST_STORE = "ssl.trust-store";
    public static final String KEY_SSL_TRUST_STORE_PASSWORD = "ssl.trust-store-password";
    public static final String KEY_SSL_TRUST_STORE_PROVIDER = "ssl.trust-store-provider";
    public static final String KEY_SSL_TRUST_MANAGER_FACTORY_ALGORITHM = "ssl.trust-manager-factory.algorithm";
    public static final String KEY_SSL_KEY_STORE = "ssl.key-store";
    public static final String KEY_SSL_KEY_STORE_PASSWORD = "ssl.key-store-password";
    public static final String KEY_SSL_KEY_STORE_PROVIDER = "ssl.key-store-provider";
    public static final String KEY_SSL_KEY_MANAGER_FACTORY_ALGORITHM = "ssl.key-manager-factory.algorithm";
    public static final String KEY_SSL_NEED_CLIENT_AUTH = "ssl.need-client-auth";
    public static final String KEY_SSL_WANT_CLIENT_AUTH = "ssl.want-client-auth";

    private static final Logger logger = Logger.getLogger(RexsterSslHelper.class);
    private static final String DEFAULT_STORE_PROVIDER = "JKS";
    private static final String DEFAULT_SSL_PROTOCOL = "TLS";

    /**
     * An empty String. Not specifying a keystore results in no keystore being used.  Not having a keystore is
     * appropriate for clients when client-auth is disabled, in which case only a truststore is needed.
     */
    private static final String DEFAULT_KEY_STORE_PATH = "";

    /**
     * No default keystore password.
     */
    private static final char[] DEFAULT_KEY_STORE_PASSWORD = null;

    /**
     * Use the default JVM truststore if omit.
     */
    private static final String DEFAULT_TRUST_STORE_PATH = System.getenv("JAVA_HOME") + "/jre/lib/security/cacerts";

    /**
     * Standard JVM default truststore password.
     */
    private static final String DEFAULT_TRUST_STORE_PASSWORD = "changeit";

    /**
     * Configuration this class utilizes for it's SSL functions.
     */
    private final Configuration configuration;

    /**
     * Constructor.
     *
     * @param configuration Configuration which includes SSL properties.
     */
    public RexsterSslHelper(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Merges a configuration from a file source (if it exists) with a passed in configuration. The passed in
     * configuration will override properties on the configuration loaded from a file.
     *
     * @param fileLocation Location of the file to load properties from.
     * @param sslConfiguration Existing configuration whose properties take priority.
     */
    public RexsterSslHelper(String fileLocation, Configuration sslConfiguration) {
        XMLConfiguration xmlConfiguration = new XMLConfiguration();

        if (fileLocation != null) {
            File rexsterClientConfig = new File(fileLocation);

            if (rexsterClientConfig.exists()) {
                logger.info(
                        String.format(
                                "Attempting to get base SSL configuration from file: %s",
                                rexsterClientConfig.getName()));
                try {
                    xmlConfiguration.load(
                            new FileReader(rexsterClientConfig));
                    logger.info(
                            String.format(
                                    "Using [%s] as base SSL configuration source.",
                                    rexsterClientConfig.getAbsolutePath()));
                } catch (Exception e) {
                    final String msg = String.format(
                            "Could not load configuration from [%s]", rexsterClientConfig.getAbsolutePath());
                    logger.warn(msg);
                    throw new RuntimeException(msg);
                }
            } else {
                final String msg = String.format(
                        "No configuration found for client SSL at: [%s], using default SSL configuration as base",
                        rexsterClientConfig.getAbsolutePath());
                logger.info(msg);
            }
        } else {
            final String msg = "No configuration specified for client SSL, using default SSL configuration as base.";
            logger.info(msg);
        }

        final CompositeConfiguration jointSslConfig = new CompositeConfiguration(sslConfiguration);
        jointSslConfig.addConfiguration(xmlConfiguration);
        this.configuration = jointSslConfig;
        logger.info("Existing SSL configuration successfully merged into base configuration.");
    }

    /**
     * Logs a message and throws an SSLException with the same message.
     *
     * @param msg The message to log and pass into the exception.
     * @param e The exception to throw with the message.
     * @throws SSLException Rethrown exception indicating the cause exception was SSL-related.
     */
    private static void logAndRethrow(String msg, Exception e) throws SSLException {
        logger.error(msg, e);
        throw new SSLException(msg, e);
    }

    /**
     * Creates an {@link javax.net.ssl.SSLContext} object using {@code configuration}.
     *
     * @return An SSLContext object that can be used for securing Rexster servers with SSL.
     * @throws SSLException If a variety of SSL related exceptions occur.
     */
    public SSLContext createRexsterSslContext() throws SSLException {
        String rexsterHome = System.getenv("REXSTER_HOME");
        rexsterHome = rexsterHome == null ? "" : rexsterHome;

        logger.info("Creating SSLContext.");
        final char[] secretServerPassword = getKeyStorePassword();

        TrustManagerFactory tmf = null;
        KeyManagerFactory kmf = null;

        try {
            tmf = initTrustManagerFactory(rexsterHome);
            // Keystore only used if it has been specified.
            if (!getKeyStore().isEmpty()) {
                kmf = initKeyManagerFactory(rexsterHome, secretServerPassword);
            }
        } catch (final IOException e) {
            logAndRethrow("Problem loading KeyStore files!", e);
        } catch (final CertificateException e) {
            logAndRethrow("Problem with certificate while loading KeyStore!", e);
        } catch (final NoSuchAlgorithmException e) {
            logAndRethrow("Invalid KeyStore algorithm!", e);
        } catch (final UnrecoverableKeyException e) {
            logAndRethrow("Problem initializing KeyManagerFactory!", e);
        } catch (final KeyStoreException e) {
            logAndRethrow("Unable to load Keystore!", e);
        }

        return initSslContext(tmf, kmf);
    }

    public final String getSslProtocol() {
        return configuration.getString(KEY_SSL_PROTOCOL, DEFAULT_SSL_PROTOCOL);
    }

    public final String getTrustStore() {
        return configuration.getString(KEY_SSL_TRUST_STORE, DEFAULT_TRUST_STORE_PATH);
    }

    public final char[] getTrustStorePassword() {
        return configuration.getString(KEY_SSL_TRUST_STORE_PASSWORD, DEFAULT_TRUST_STORE_PASSWORD).toCharArray();
    }

    public final String getTrustStoreProvider() {
        return configuration.getString(KEY_SSL_TRUST_STORE_PROVIDER, DEFAULT_STORE_PROVIDER);
    }

    public final String getKeyStore() {
        return configuration.getString(KEY_SSL_KEY_STORE, DEFAULT_KEY_STORE_PATH);
    }

    public final String getKeyStoreProvider() {
        return configuration.getString(KEY_SSL_KEY_STORE_PROVIDER, DEFAULT_STORE_PROVIDER);
    }

    public final boolean getNeedClientAuth() {
        return configuration.getBoolean(KEY_SSL_NEED_CLIENT_AUTH, false);
    }

    public final boolean getWantClientAuth() {
        return configuration.getBoolean(KEY_SSL_WANT_CLIENT_AUTH, false);
    }

    private char[] getKeyStorePassword() {
        final String keyStorePassword = configuration.getString(KEY_SSL_KEY_STORE_PASSWORD, null);
        if (keyStorePassword == null) {
            return DEFAULT_KEY_STORE_PASSWORD;
        }
        return keyStorePassword.toCharArray();
    }

    /**
     * Initializes the SSL Context based on {@code configuration};
     *
     * @param trustManagerFactory Provides trust managers to use with the produced SSLContext, if any.
     * @param keyManagerFactory Provides key managers to use with the produced SSLContext, if any.
     * @return an SSLContext based on this class' configuration.
     * @throws SSLException If a variety of SSL related errors occur.
     */
    private SSLContext initSslContext(TrustManagerFactory trustManagerFactory, KeyManagerFactory keyManagerFactory)
            throws SSLException {
        SSLContext sslContext = null;
        final String sslProtocol = getSslProtocol();

        KeyManager[] keyManagers = null;

        if (keyManagerFactory != null) {
            keyManagers = keyManagerFactory.getKeyManagers();
        }
        final TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        try {
            sslContext = SSLContext.getInstance(sslProtocol);
            sslContext.init(keyManagers, trustManagers, null);
        } catch (final NoSuchAlgorithmException e) {
            logAndRethrow(String.format("Invalid SSL Protocol '%s'", sslProtocol), e);
        } catch (final KeyManagementException e) {
            logAndRethrow("Unable to initialize SSLContext.", e);
        }
        return sslContext;
    }

    private TrustManagerFactory initTrustManagerFactory(String rexsterHome)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        final String tmfAlgorithm = this.configuration.getString(
                KEY_SSL_TRUST_MANAGER_FACTORY_ALGORITHM, TrustManagerFactory.getDefaultAlgorithm());
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);

        final KeyStore trustStore = KeyStore.getInstance(getTrustStoreProvider());
        InputStream trustStoreInputStream = null;

        final String trustStorePath = getTrustStore();

        // If truststore is intentionally blank, do not use a truststore.
        if (!trustStorePath.isEmpty()) {
            try {
                trustStoreInputStream = new FileInputStream(rexsterHome + trustStorePath);
                trustStore.load(trustStoreInputStream, getTrustStorePassword());
            } finally {
                if (trustStoreInputStream != null) {
                    trustStoreInputStream.close();
                }
            }
        }
        tmf.init(trustStore);

        return tmf;
    }

    private KeyManagerFactory initKeyManagerFactory(
            String rexsterHome, char[] secretServerPassword)
            throws NoSuchAlgorithmException, IOException, CertificateException, UnrecoverableKeyException,
            KeyStoreException {
        final String kmfAlgorithm = this.configuration.getString(
                KEY_SSL_KEY_MANAGER_FACTORY_ALGORITHM, KeyManagerFactory.getDefaultAlgorithm());

        final KeyManagerFactory kmf = KeyManagerFactory.getInstance(kmfAlgorithm);

        final KeyStore keyStore = KeyStore.getInstance(getKeyStoreProvider());
        final String keyStorePath = getKeyStore();
        InputStream keyStoreInputStream = null;

        try {
            keyStoreInputStream = new FileInputStream(rexsterHome + keyStorePath);
            keyStore.load(keyStoreInputStream, secretServerPassword);
            kmf.init(keyStore, secretServerPassword);
        } finally {
            if (keyStoreInputStream != null) {
                keyStoreInputStream.close();
            }
        }

        return kmf;
    }
}

package com.tinkerpop.rexster.server;

import com.codahale.metrics.JmxAttributeGauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jersey.InstrumentedResourceMethodDispatchAdapter;
import com.codahale.metrics.servlets.MetricsServlet;
import com.sun.jersey.api.container.filter.LoggingFilter;
import com.sun.jersey.api.core.ClassNamesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;
import com.tinkerpop.rexster.EdgeResource;
import com.tinkerpop.rexster.GraphResource;
import com.tinkerpop.rexster.IndexResource;
import com.tinkerpop.rexster.KeyIndexResource;
import com.tinkerpop.rexster.PrefixResource;
import com.tinkerpop.rexster.RexsterResource;
import com.tinkerpop.rexster.RootResource;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.VertexResource;
import com.tinkerpop.rexster.filter.AbstractSecurityFilter;
import com.tinkerpop.rexster.filter.DefaultSecurityFilter;
import com.tinkerpop.rexster.filter.HeaderResponseFilter;
import com.tinkerpop.rexster.server.metrics.AbstractReporterConfig;
import com.tinkerpop.rexster.servlet.DogHouseServlet;
import com.tinkerpop.rexster.servlet.EvaluatorServlet;
import com.tinkerpop.rexster.servlet.RexsterStaticHttpHandler;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.IOStrategy;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.grizzly.threadpool.GrizzlyExecutorService;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.ws.rs.core.Context;
import java.io.File;

/**
 * Initializes the HTTP server for Rexster serving REST and Dog House.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class HttpRexsterServer implements RexsterServer {
    private static final Logger logger = Logger.getLogger(HttpRexsterServer.class);

    private RexsterApplication app;
    private final RexsterProperties properties;
    private Integer rexsterServerPort;
    private String rexsterServerHost;
    private String webRootPath;
    private String baseUri;
    private int maxWorkerThreadPoolSize;
    private int coreWorkerThreadPoolSize;
    private int maxKernalThreadPoolSize;
    private int coreKernalThreadPoolSize;
    private int maxPostSize;
    private int maxHeaderSize;
    private int uploadTimeoutMillis;
    private boolean enableJmx;
    private String ioStrategy;
    private final HttpServer httpServer;
    private boolean debugMode;
    private boolean enableHttpReporter;
    private boolean enableDogHouse;
    private String convertRateTo;
    private String convertDurationTo;
    private String securityFilterType;
    private String defaultCharacterEncoding;

    private HttpHandler staticHttpHandler = null;
    private WebappContext wacDogHouse;
    private WebappContext wacJersey;
    private WebappContext wacMetrics;

    private String lastDefaultCharacterEncoding;
    private String lastSecurityFilterType;
    private Integer lastRexsterServerPort;
    private String lastRexsterServerHost;
    private String lastIoStrategy;
    private boolean lastEnableJmx;
    private int lastMaxWorkerThreadPoolSize;
    private int lastCoreWorkerThreadPoolSize;
    private int lastMaxKernalThreadPoolSize;
    private int lastCoreKernalThreadPoolSize;
    private boolean lastEnableDogHouse;
    private String lastWebRootPath;
    private String lastBaseUri;
    private boolean lastDebugMode;
    private boolean lastEnableHttpReporter;

    public HttpRexsterServer(final XMLConfiguration configuration) {
        this(new RexsterProperties(configuration));
    }

    public HttpRexsterServer(final RexsterProperties properties) {
        this.properties = properties;
        this.httpServer = new HttpServer();
        updateSettings(properties.getConfiguration());

        properties.addListener(new RexsterProperties.RexsterPropertiesListener() {
            @Override
            public void propertiesChanged(final XMLConfiguration configuration) {
                // maintain history of previous settings
                lastRexsterServerHost = rexsterServerHost;
                lastRexsterServerPort = rexsterServerPort;
                lastEnableJmx = enableJmx;
                lastIoStrategy = ioStrategy;
                lastMaxWorkerThreadPoolSize = maxWorkerThreadPoolSize;
                lastCoreWorkerThreadPoolSize = coreWorkerThreadPoolSize;
                lastMaxKernalThreadPoolSize = maxKernalThreadPoolSize;
                lastCoreKernalThreadPoolSize = coreKernalThreadPoolSize;
                lastEnableDogHouse = enableDogHouse;
                lastWebRootPath = webRootPath;
                lastBaseUri = baseUri;
                lastSecurityFilterType = securityFilterType;
                lastDefaultCharacterEncoding = defaultCharacterEncoding;
                lastDebugMode = debugMode;
                lastEnableHttpReporter = enableHttpReporter;

                updateSettings(configuration);

                try {
                    reconfigure(app);
                } catch (Exception ex) {
                    logger.error("Could not modify Rexster configuration.  Please restart Rexster to allow changes to be applied.", ex);
                }
            }
        });
    }

    @Override
    public void stop() throws Exception {
        this.httpServer.stop();
    }

    @Override
    public void start(final RexsterApplication application) throws Exception {
        this.app = application;
        reconfigure(application);
    }

    /**
     * Reconfigures and starts the server if not already started.
     */
    public void reconfigure(final RexsterApplication application) throws Exception {
        // Seems to be a bug in WebappContext.undeploy() of grizzly that not only undeploy's the context,but
        // all the servlet registrations from other contexts as well.  hence....it is not possible to undeploy
        // just a single context with tearing everything down and building it back. unfortunately this means that
        // a full undeploy and redeploy of all apps installed to the web server need to be removed and put back
        // on any change.  ....either that, or I don't get the undeploy() method and what it's supposed to do
        if (hasAnythingChanged()) {

            if (this.wacJersey != null) {
                this.wacJersey.undeploy();
                this.wacJersey = null;
            }

            if (this.wacDogHouse != null) {
                this.wacDogHouse.undeploy();
                this.wacDogHouse = null;
            }

            if (this.wacMetrics != null) {
                this.wacMetrics.undeploy();
                this.wacMetrics = null;
            }
        }

        deployRestApi(application);
        deployStaticResourceServer();
        deployDogHouse(application);
        deployMetricsAdmin(application);

        this.configureNetworkListener();

        // the JMX settings below pipe in metrics from Grizzly.
        if (hasEnableJmxChanged()) {
            this.httpServer.getServerConfiguration().setJmxEnabled(enableJmx);
            manageJmxMetrics(application, enableJmx);

            logger.info(this.enableJmx ? "JMX enabled on HTTP/REST." : "JMX disabled on HTTP/REST.");
        }

        if (!this.httpServer.isStarted()) {
            this.httpServer.start();
        }

        logger.info("Rexster Server running on: [" + baseUri + ":" + rexsterServerPort + "]");
    }

    private void manageJmxMetrics(final RexsterApplication application, final boolean register) throws MalformedObjectNameException {
        // the JMX settings below pipe in metrics from Grizzly.
        final MetricRegistry metricRegistry = application.getMetricRegistry();
        manageMetricsFromJmx(metricRegistry, register);
        logger.info(register ? "Registered JMX Metrics." : "Removed JMX Metrics.");
    }

    private boolean hasAnythingChanged() {
        return hasRestConfigurationsChanged() || hasWebRootChanged() || hasEnableDogHouseChanged()
                || hasBaseUriChanged() || hasEnableHttpReporterChanged();
    }

    private boolean hasWebRootChanged() {
        return !this.webRootPath.equals(this.lastWebRootPath);
    }

    private boolean hasPortHostChanged() {
        return !this.rexsterServerPort.equals(this.lastRexsterServerPort) || !this.lastRexsterServerHost.equals(this.rexsterServerHost);
    }

    private boolean hasEnableJmxChanged() {
        return this.enableJmx != this.lastEnableJmx;
    }

    private boolean hasEnableDogHouseChanged() {
        return this.enableDogHouse != this.lastEnableDogHouse;
    }

    private boolean hasBaseUriChanged() {
        return !this.baseUri.equals(this.lastBaseUri);
    }

    private boolean hasIoStrategyChanged() {
        return !this.ioStrategy.equals(this.lastIoStrategy);
    }

    private boolean hasThreadPoolSizeChanged() {
        return this.maxKernalThreadPoolSize != lastMaxKernalThreadPoolSize || this.maxWorkerThreadPoolSize != lastMaxWorkerThreadPoolSize
                || this.coreKernalThreadPoolSize != lastCoreKernalThreadPoolSize || this.coreWorkerThreadPoolSize != this.lastCoreWorkerThreadPoolSize;
    }

    private boolean hasRestConfigurationsChanged() {
        return !this.securityFilterType.equals(this.lastSecurityFilterType) || !this.defaultCharacterEncoding.equals(this.lastDefaultCharacterEncoding)
                || this.debugMode != lastDebugMode;
    }

    private boolean hasEnableHttpReporterChanged() {
        return this.enableHttpReporter != this.lastEnableHttpReporter;
    }

    private void updateSettings(final XMLConfiguration configuration) {
        this.debugMode = configuration.getBoolean("debug", false);
        this.enableDogHouse = configuration.getBoolean("http.enable-doghouse", true);
        this.enableHttpReporter = configuration.getBoolean("http-reporter-enabled", false);
        this.convertRateTo = configuration.getString("http-reporter-convert", AbstractReporterConfig.DEFAULT_TIME_UNIT.toString());
        this.convertDurationTo = configuration.getString("http-reporter-duration", AbstractReporterConfig.DEFAULT_TIME_UNIT.toString());
        this.rexsterServerPort = configuration.getInteger("http.server-port", new Integer(RexsterSettings.DEFAULT_HTTP_PORT));
        this.rexsterServerHost = configuration.getString("http.server-host", "0.0.0.0");
        this.webRootPath = configuration.getString("http.web-root", RexsterSettings.DEFAULT_WEB_ROOT_PATH);
        this.baseUri = configuration.getString("http.base-uri", RexsterSettings.DEFAULT_BASE_URI);
        this.coreWorkerThreadPoolSize = configuration.getInt("http.thread-pool.worker.core-size", 8);
        this.maxWorkerThreadPoolSize = configuration.getInt("http.thread-pool.worker.max-size", 8);
        this.coreKernalThreadPoolSize = configuration.getInt("http.thread-pool.kernal.core-size", 4);
        this.maxKernalThreadPoolSize = configuration.getInt("http.thread-pool.kernal.max-size", 4);
        this.maxPostSize = configuration.getInt("http.max-post-size", 2097152);
        this.maxHeaderSize = configuration.getInt("http.max-header-size", 8192);
        this.uploadTimeoutMillis = configuration.getInt("http.upload-timeout-millis", 300000);
        this.enableJmx = configuration.getBoolean("http.enable-jmx", false);
        this.ioStrategy = configuration.getString("http.io-strategy", "leader-follower");
        this.defaultCharacterEncoding = configuration.getString("http.character-set", "ISO-8859-1");

        HierarchicalConfiguration securityConfiguration = null;
        try {
            securityConfiguration = configuration.configurationAt(Tokens.REXSTER_SECURITY_AUTH);
        } catch (IllegalArgumentException iae) {
            // do nothing...null is cool
        }

        securityFilterType = securityConfiguration != null ? securityConfiguration.getString("type") : Tokens.REXSTER_SECURITY_NONE;

    }

    private static void manageMetricsFromJmx(final MetricRegistry metricRegistry, final boolean register) throws MalformedObjectNameException {
        final String jmxObjectMemoryManager = "org.glassfish.grizzly:pp=/gmbal-root/TCPNIOTransport[RexPro],type=HeapMemoryManager,name=MemoryManager";
        final String metricGroupMemoryManager = "heap-memory-manager";
        final String[] heapMemoryManagerMetrics = new String[] {
            "pool-allocated-bytes", "pool-released-bytes", "real-allocated-bytes", "total-allocated-bytes"
        };

        manageJmxKeysAsMetric(metricRegistry, jmxObjectMemoryManager, metricGroupMemoryManager, heapMemoryManagerMetrics, register);

        final String jmxObjectHttpServerFilter = "org.glassfish.grizzly:pp=/gmbal-root/HttpServer[HttpServer]/NetworkListener[NetworkListener[grizzly]],type=HttpServerFilter,name=HttpServerFilter";
        final String metricGroupHttpServerFilter = "http-server";
        final String[] httpServerManagerMetrics = new String [] {
            "current-suspended-request-count", "requests-cancelled-count", "requests-completed-count", "requests-received-count", "requests-timed-out-count"
        };

        manageJmxKeysAsMetric(metricRegistry, jmxObjectHttpServerFilter, metricGroupHttpServerFilter, httpServerManagerMetrics, register);

        final String jmxObjectHttpKeepAlive = "org.glassfish.grizzly:pp=/gmbal-root/HttpServer[HttpServer]/NetworkListener[NetworkListener[grizzly]],type=KeepAlive,name=Keep-Alive";
        final String metricGroupHttpKeepAlive = "http-keep-alive";
        final String[] httpKeepAliveMetrics = new String [] {
            "hits-count", "idle-timeout-seconds", "live-connections-count", "max-requests-count", "refuses-count", "timeouts-count"
        };

        manageJmxKeysAsMetric(metricRegistry, jmxObjectHttpKeepAlive, metricGroupHttpKeepAlive, httpKeepAliveMetrics, register);

        final String jmxObjectNetworkListener = "org.glassfish.grizzly:pp=/gmbal-root/HttpServer[HttpServer],type=NetworkListener,name=NetworkListener[grizzly]";
        final String metricGroupNetworkListener = "network-listener";
        final String [] networkListenerMetrics = new String[] {
            "chunking-enabled", "host", "idle-timeout-seconds", "max-http-header-size", "max-pending-bytes", "port"
        };

        manageJmxKeysAsMetric(metricRegistry, jmxObjectNetworkListener, metricGroupNetworkListener, networkListenerMetrics, register);

        final String jmxObjectTcpNioTransport = "org.glassfish.grizzly:pp=/gmbal-root/HttpServer[HttpServer]/NetworkListener[NetworkListener[grizzly]],type=TCPNIOTransport,name=Transport";
        final String metricGroupTcpNioTransport = "tcp-nio-transport";
        final String [] tcpNioTransportMetrics = new String[] {
            "bound-addresses", "bytes-read", "bytes-written", "client-connect-timeout-millis", "io-strategy",
            "open-connections-count", "read-buffer-size", "selector-threads-count", "server-socket-so-timeout",
            "total-connections-count", "write-buffer-size"
        };

        manageJmxKeysAsMetric(metricRegistry, jmxObjectTcpNioTransport, metricGroupTcpNioTransport, tcpNioTransportMetrics, register);

        final String jmxObjectThreadPool = "org.glassfish.grizzly:pp=/gmbal-root/HttpServer[HttpServer]/NetworkListener[NetworkListener[grizzly]]/TCPNIOTransport[Transport],type=ThreadPool,name=ThreadPool";
        final String metricGroupThreadPool = "thread-pool";
        final String [] threadPoolMetrics = new String[] {
            "thread-pool-allocated-thread-count", "thread-pool-core-pool-size", "thread-pool-max-num-threads",
            "thread-pool-queued-task-count", "thread-pool-task-queue-overflow-count",
            "thread-pool-total-allocated-thread-count", "thread-pool-total-completed-tasks-count", "thread-pool-type"
        };

        manageJmxKeysAsMetric(metricRegistry, jmxObjectThreadPool, metricGroupThreadPool, threadPoolMetrics, register);
    }

    private static void manageJmxKeysAsMetric(final MetricRegistry metricRegistry, final String jmxObjectName,
                                                     final String metricGroup, final String[] metricKeys,
                                                     final boolean register) throws MalformedObjectNameException {
        for (String metricKey : metricKeys) {
            if (register)
                registerJmxKeyAsMetric(metricRegistry, metricGroup, jmxObjectName, metricKey);
            else
                deregisterJmxKeyAsMetric(metricRegistry, metricGroup, metricKey);
        }
    }

    private static void registerJmxKeyAsMetric(final MetricRegistry metricRegistry, final String metricGroup,
                                               final String jmxObjectName, final String jmxAttributeName) throws MalformedObjectNameException  {
        metricRegistry.register(MetricRegistry.name("http", "core", metricGroup, jmxAttributeName),
                new JmxAttributeGauge(new ObjectName(jmxObjectName), jmxAttributeName));
    }

    private static void deregisterJmxKeyAsMetric(final MetricRegistry metricRegistry, final String metricGroup,
                                                 final String jmxAttributeName) throws MalformedObjectNameException  {
        metricRegistry.remove(MetricRegistry.name("http", "core", metricGroup, jmxAttributeName));
    }

    private void deployRestApi(final RexsterApplication application) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (hasAnythingChanged()) {
            wacJersey = new WebappContext("jersey", "");

            // explicitly load resources so that the "RexsterApplicationProvider" class is not loaded
            final ResourceConfig rc = constructResourceConfig();

            // constructs an injectable for the RexsterApplication instance.  This get constructed externally
            // and is passed into the HttpRexsterServer.  The SingletonTypeInjectableProvider is responsible for
            // pushing that instance into the context.
            rc.getSingletons().add(new SingletonTypeInjectableProvider<Context, RexsterApplication>(
                    RexsterApplication.class, application) {
            });
            rc.getSingletons().add(new InstrumentedResourceMethodDispatchAdapter(application.getMetricRegistry()));

            if (this.debugMode) {
                rc.getContainerRequestFilters().add(new LoggingFilter());
                rc.getContainerResponseFilters().add(new LoggingFilter());
            }

            rc.getContainerResponseFilters().add(new HeaderResponseFilter(defaultCharacterEncoding));

            if (!securityFilterType.equals(Tokens.REXSTER_SECURITY_NONE)) {
                final AbstractSecurityFilter securityFilter;
                if (securityFilterType.equals(Tokens.REXSTER_SECURITY_DEFAULT)) {
                    wacJersey.addContextInitParameter(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, DefaultSecurityFilter.class.getName());
                    securityFilter = new DefaultSecurityFilter();
                } else {
                    wacJersey.addContextInitParameter(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, securityFilterType);
                    final Class clazz = Class.forName(securityFilterType, true, Thread.currentThread().getContextClassLoader());
                    securityFilter = (AbstractSecurityFilter) clazz.newInstance();
                }

                securityFilter.configure(properties.getConfiguration());
                rc.getContainerRequestFilters().add(securityFilter);
            }

            final ServletRegistration sg = wacJersey.addServlet("jersey", new ServletContainer(rc));
            sg.addMapping("/*");
            wacJersey.deploy(this.httpServer);
        }
    }

    private ResourceConfig constructResourceConfig() {
        ResourceConfig rc;
        if (enableDogHouse) {
            rc = new ClassNamesResourceConfig(
                EdgeResource.class,
                GraphResource.class,
                IndexResource.class,
                KeyIndexResource.class,
                PrefixResource.class,
                RexsterResource.class,
                RootResource.class,
                VertexResource.class);
        } else {
            // need to disable the root resource which shows the splash page. not needed when dog house is disabled
            rc = new ClassNamesResourceConfig(
                EdgeResource.class,
                GraphResource.class,
                IndexResource.class,
                KeyIndexResource.class,
                PrefixResource.class,
                RexsterResource.class,
                VertexResource.class);
        }
        return rc;
    }

    private void deployStaticResourceServer() {
        if (hasAnythingChanged()) {
            final ServerConfiguration config = this.httpServer.getServerConfiguration();
            final String absoluteWebRootPath = (new File(webRootPath)).getAbsolutePath();

            if (staticHttpHandler != null) {
                config.removeHttpHandler(staticHttpHandler);
            }

            if (enableDogHouse) {
                staticHttpHandler = new RexsterStaticHttpHandler(absoluteWebRootPath);
                config.addHttpHandler(staticHttpHandler, "/static");
            }
        }
    }

    private void deployDogHouse(final RexsterApplication application) {
        // servlet that services all url from "main" by simply sending
        // main.html back to the calling client.  main.html handles its own
        // state given the uri
        if (hasAnythingChanged()) {
            if (enableDogHouse) {
                this.wacDogHouse = new WebappContext("doghouse", "");
                final ServletRegistration sgDogHouse = wacDogHouse.addServlet("doghouse", new DogHouseServlet());
                sgDogHouse.addMapping("/doghouse/*");
                sgDogHouse.setInitParameter("com.tinkerpop.rexster.config.rexsterApiBaseUri", baseUri + ":" + rexsterServerPort.toString());

                final ServletRegistration sgDogHouseEval = wacDogHouse.addServlet("doghouse-evaluator", new EvaluatorServlet(application));
                sgDogHouseEval.addMapping("/doghouse/exec");

                wacDogHouse.deploy(this.httpServer);
            }
        }
    }

    private void deployMetricsAdmin(final RexsterApplication application) {
        if (hasAnythingChanged()) {
            if (this.enableHttpReporter) {
                // deploys the metrics servlet into rexster
                wacMetrics = new WebappContext("metrics", "");
                wacMetrics.setAttribute("com.codahale.metrics.servlets.MetricsServlet.registry", application.getMetricRegistry());
                wacMetrics.setAttribute("com.codahale.metrics.servlets.MetricsServlet.rateUnit", this.convertRateTo);
                wacMetrics.setAttribute("com.codahale.metrics.servlets.MetricsServlet.durationUnit", this.convertDurationTo);

                final ServletRegistration sgMetrics = wacMetrics.addServlet("metrics", new MetricsServlet());
                sgMetrics.addMapping("/metrics/*");

                wacMetrics.deploy(this.httpServer);
            }
        }
    }

    private void configureNetworkListener() throws Exception {
        boolean allowPortChange = true;
        NetworkListener listener = this.httpServer.getListener("grizzly");
        if (listener == null) {
            listener = new NetworkListener("grizzly", rexsterServerHost, rexsterServerPort);
            this.httpServer.addListener(listener);
            allowPortChange = false;
        }

        if (allowPortChange && hasPortHostChanged()) {
            listener.getTransport().unbindAll();
            listener.getTransport().bind(rexsterServerHost, rexsterServerPort);

            logger.info(String.format("RexPro Server bound to [%s:%s]", rexsterServerHost, rexsterServerPort));
        }

        if (hasThreadPoolSizeChanged()) {
            final ThreadPoolConfig workerThreadPoolConfig = ThreadPoolConfig.defaultConfig()
                    .setCorePoolSize(coreWorkerThreadPoolSize)
                    .setMaxPoolSize(maxWorkerThreadPoolSize);
            listener.getTransport().setWorkerThreadPoolConfig(workerThreadPoolConfig);
            final ThreadPoolConfig kernalThreadPoolConfig = ThreadPoolConfig.defaultConfig()
                    .setCorePoolSize(coreKernalThreadPoolSize)
                    .setMaxPoolSize(maxKernalThreadPoolSize);
            listener.getTransport().setKernelThreadPoolConfig(kernalThreadPoolConfig);

            if (listener.getTransport().getKernelThreadPool() != null) {
                ((GrizzlyExecutorService) listener.getTransport().getKernelThreadPool()).reconfigure(kernalThreadPoolConfig);
            }

            if (listener.getTransport().getWorkerThreadPool() != null) {
                ((GrizzlyExecutorService) listener.getTransport().getWorkerThreadPool()).reconfigure(workerThreadPoolConfig);
            }

            logger.info(String.format("HTTP/REST thread pool configuration: kernal[%s / %s] worker[%s / %s] ",
                    coreKernalThreadPoolSize, maxKernalThreadPoolSize,
                    coreWorkerThreadPoolSize, maxWorkerThreadPoolSize));
        }

        listener.setMaxPostSize(maxPostSize);
        listener.setMaxHttpHeaderSize(maxHeaderSize);
        listener.setUploadTimeout(uploadTimeoutMillis);
        listener.setDisableUploadTimeout(false);

        if (this.hasIoStrategyChanged()) {
            final IOStrategy strategy = GrizzlyIoStrategyFactory.createIoStrategy(this.ioStrategy);
            listener.getTransport().setIOStrategy(strategy);

            logger.info(String.format("Using %s IOStrategy for HTTP/REST.", strategy.getClass().getName()));
        }
    }
}

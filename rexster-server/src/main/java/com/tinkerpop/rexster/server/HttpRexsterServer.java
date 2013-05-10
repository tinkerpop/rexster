package com.tinkerpop.rexster.server;

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
import com.yammer.metrics.JmxAttributeGauge;
import com.yammer.metrics.JmxReporter;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.jersey.InstrumentedResourceMethodDispatchAdapter;
import com.yammer.metrics.servlets.AdminServlet;
import com.yammer.metrics.servlets.MetricsServlet;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.IOStrategy;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
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

    private final XMLConfiguration properties;
    private final Integer rexsterServerPort;
    private final String rexsterServerHost;
    private final String webRootPath;
    private final String baseUri;
    private final int maxWorkerThreadPoolSize;
    private final int coreWorkerThreadPoolSize;
    private final int maxKernalThreadPoolSize;
    private final int coreKernalThreadPoolSize;
    private final int maxPostSize;
    private final int maxHeaderSize;
    private final int uploadTimeoutMillis;
    private final boolean enableJmx;
    private final String ioStrategy;
    private final HttpServer httpServer;
    private final boolean debugMode;
    private final boolean enableHttpReporter;
    private final boolean enableDogHouse;
    private final String convertRateTo;
    private final String convertDurationTo;

    public HttpRexsterServer(final XMLConfiguration properties) {
        this.properties = properties;
        this.debugMode = properties.getBoolean("debug", false);
        this.enableDogHouse = properties.getBoolean("http.enable-doghouse", true);
        this.enableHttpReporter = properties.getBoolean("http-reporter-enabled", false);
        this.convertRateTo = properties.getString("http-reporter-convert", AbstractReporterConfig.DEFAULT_TIME_UNIT.toString());
        this.convertDurationTo = properties.getString("http-reporter-duration", AbstractReporterConfig.DEFAULT_TIME_UNIT.toString());
        this.rexsterServerPort = properties.getInteger("http.server-port", new Integer(RexsterSettings.DEFAULT_HTTP_PORT));
        this.rexsterServerHost = properties.getString("http.server-host", "0.0.0.0");
        this.webRootPath = properties.getString("http.web-root", RexsterSettings.DEFAULT_WEB_ROOT_PATH);
        this.baseUri = properties.getString("http.base-uri", RexsterSettings.DEFAULT_BASE_URI);
        this.coreWorkerThreadPoolSize = properties.getInt("http.thread-pool.worker.core-size", 8);
        this.maxWorkerThreadPoolSize = properties.getInt("http.thread-pool.worker.max-size", 8);
        this.coreKernalThreadPoolSize = properties.getInt("http.thread-pool.kernal.core-size", 4);
        this.maxKernalThreadPoolSize = properties.getInt("http.thread-pool.kernal.max-size", 4);
        this.maxPostSize = properties.getInt("http.max-post-size", 2097152);
        this.maxHeaderSize = properties.getInt("http.max-header-size", 8192);
        this.uploadTimeoutMillis = properties.getInt("http.upload-timeout-millis", 300000);
        this.enableJmx = properties.getBoolean("http.enable-jmx", false);
        this.ioStrategy = properties.getString("http.io-strategy", "leader-follower");

        this.httpServer = new HttpServer();
    }

    public HttpRexsterServer(final RexsterProperties properties) {
        this(properties.getConfiguration());
    }

    @Override
    public void stop() throws Exception {
        this.httpServer.stop();
    }

    @Override
    public void start(final RexsterApplication application) throws Exception {

        deployRestApi(application);

        if (enableDogHouse) {
            // serves images
            deployStaticResourceServer();
            deployDogHouse(application);
        }

        if (enableHttpReporter) {
            deployMetricsAdmin(application);
        }

        final NetworkListener listener = configureNetworkListener();
        final IOStrategy strategy = GrizzlyIoStrategyFactory.createIoStrategy(this.ioStrategy);

        logger.info(String.format("Using %s IOStrategy for HTTP/REST.", strategy.getClass().getName()));

        listener.getTransport().setIOStrategy(strategy);
        this.httpServer.addListener(listener);

        // the commented out line below enables raw JMX settings from grizzly
        this.httpServer.getServerConfiguration().setJmxEnabled(enableJmx);

        final MetricRegistry metricRegistry = application.getMetricRegistry();

        // the JMX settings below pipe in metrics from Grizzly.
        if (enableJmx) {
            registerMetricsFromJmx(metricRegistry);
        }

        this.httpServer.start();

        logger.info("Rexster Server running on: [" + baseUri + ":" + rexsterServerPort + "]");
    }

    private static void registerMetricsFromJmx(final MetricRegistry metricRegistry) throws MalformedObjectNameException {
        final String jmxObjectMemoryManager = "org.glassfish.grizzly:pp=/gmbal-root/TCPNIOTransport[RexPro],type=HeapMemoryManager,name=MemoryManager";
        final String metricGroupMemoryManager = "heap-memory-manager";
        registerJmxKeyAsMetric(metricRegistry, metricGroupMemoryManager, jmxObjectMemoryManager, "pool-allocated-bytes");
        registerJmxKeyAsMetric(metricRegistry, metricGroupMemoryManager, jmxObjectMemoryManager, "pool-released-bytes");
        registerJmxKeyAsMetric(metricRegistry, metricGroupMemoryManager, jmxObjectMemoryManager, "real-allocated-bytes");
        registerJmxKeyAsMetric(metricRegistry, metricGroupMemoryManager, jmxObjectMemoryManager, "total-allocated-bytes");

        final String jmxObjectHttpServerFilter = "org.glassfish.grizzly:pp=/gmbal-root/HttpServer[HttpServer]/NetworkListener[NetworkListener[grizzly]],type=HttpServerFilter,name=HttpServerFilter";
        final String metricGroupHttpServerFilter = "http-server";
        registerJmxKeyAsMetric(metricRegistry, metricGroupHttpServerFilter, jmxObjectHttpServerFilter, "current-suspended-request-count");
        registerJmxKeyAsMetric(metricRegistry, metricGroupHttpServerFilter, jmxObjectHttpServerFilter, "requests-cancelled-count");
        registerJmxKeyAsMetric(metricRegistry, metricGroupHttpServerFilter, jmxObjectHttpServerFilter, "requests-completed-count");
        registerJmxKeyAsMetric(metricRegistry, metricGroupHttpServerFilter, jmxObjectHttpServerFilter, "requests-received-count");
        registerJmxKeyAsMetric(metricRegistry, metricGroupHttpServerFilter, jmxObjectHttpServerFilter, "requests-timed-out-count");

        final String jmxObjectHttpKeepAlive = "org.glassfish.grizzly:pp=/gmbal-root/HttpServer[HttpServer]/NetworkListener[NetworkListener[grizzly]],type=KeepAlive,name=Keep-Alive";
        final String metricGroupHttpKeepAlive = "http-keep-alive";
        registerJmxKeyAsMetric(metricRegistry, metricGroupHttpKeepAlive, jmxObjectHttpKeepAlive, "hits-count");
        registerJmxKeyAsMetric(metricRegistry, metricGroupHttpKeepAlive, jmxObjectHttpKeepAlive, "idle-timeout-seconds");
        registerJmxKeyAsMetric(metricRegistry, metricGroupHttpKeepAlive, jmxObjectHttpKeepAlive, "live-connections-count");
        registerJmxKeyAsMetric(metricRegistry, metricGroupHttpKeepAlive, jmxObjectHttpKeepAlive, "max-requests-count");
        registerJmxKeyAsMetric(metricRegistry, metricGroupHttpKeepAlive, jmxObjectHttpKeepAlive, "refuses-count");
        registerJmxKeyAsMetric(metricRegistry, metricGroupHttpKeepAlive, jmxObjectHttpKeepAlive, "timeouts-count");

        final String jmxObjectNetworkListener = "org.glassfish.grizzly:pp=/gmbal-root/HttpServer[HttpServer],type=NetworkListener,name=NetworkListener[grizzly]";
        final String metricGroupNetworkListener = "network-listener";
        registerJmxKeyAsMetric(metricRegistry, metricGroupNetworkListener, jmxObjectNetworkListener, "chunking-enabled");
        registerJmxKeyAsMetric(metricRegistry, metricGroupNetworkListener, jmxObjectNetworkListener, "host");
        registerJmxKeyAsMetric(metricRegistry, metricGroupNetworkListener, jmxObjectNetworkListener, "idle-timeout-seconds");
        registerJmxKeyAsMetric(metricRegistry, metricGroupNetworkListener, jmxObjectNetworkListener, "max-http-header-size");
        registerJmxKeyAsMetric(metricRegistry, metricGroupNetworkListener, jmxObjectNetworkListener, "max-pending-bytes");
        registerJmxKeyAsMetric(metricRegistry, metricGroupNetworkListener, jmxObjectNetworkListener, "port");

        final String jmxObjectTcpNioTransport = "org.glassfish.grizzly:pp=/gmbal-root/HttpServer[HttpServer]/NetworkListener[NetworkListener[grizzly]],type=TCPNIOTransport,name=Transport";
        final String metricGroupTcpNioTransport = "tcp-nio-transport";
        registerJmxKeyAsMetric(metricRegistry, metricGroupTcpNioTransport, jmxObjectTcpNioTransport, "bound-addresses");
        registerJmxKeyAsMetric(metricRegistry, metricGroupTcpNioTransport, jmxObjectTcpNioTransport, "bytes-read");
        registerJmxKeyAsMetric(metricRegistry, metricGroupTcpNioTransport, jmxObjectTcpNioTransport, "bytes-written");
        registerJmxKeyAsMetric(metricRegistry, metricGroupTcpNioTransport, jmxObjectTcpNioTransport, "client-connect-timeout-millis");
        registerJmxKeyAsMetric(metricRegistry, metricGroupTcpNioTransport, jmxObjectTcpNioTransport, "io-strategy");
        registerJmxKeyAsMetric(metricRegistry, metricGroupTcpNioTransport, jmxObjectTcpNioTransport, "open-connections-count");
        registerJmxKeyAsMetric(metricRegistry, metricGroupTcpNioTransport, jmxObjectTcpNioTransport, "read-buffer-size");
        registerJmxKeyAsMetric(metricRegistry, metricGroupTcpNioTransport, jmxObjectTcpNioTransport, "selector-threads-count");
        registerJmxKeyAsMetric(metricRegistry, metricGroupTcpNioTransport, jmxObjectTcpNioTransport, "server-socket-so-timeout");
        registerJmxKeyAsMetric(metricRegistry, metricGroupTcpNioTransport, jmxObjectTcpNioTransport, "total-connections-count");
        registerJmxKeyAsMetric(metricRegistry, metricGroupTcpNioTransport, jmxObjectTcpNioTransport, "write-buffer-size");

        final String jmxObjectThreadPool = "org.glassfish.grizzly:pp=/gmbal-root/HttpServer[HttpServer]/NetworkListener[NetworkListener[grizzly]]/TCPNIOTransport[Transport],type=ThreadPool,name=ThreadPool";
        final String metricGroupThreadPool = "thread-pool";
        registerJmxKeyAsMetric(metricRegistry, metricGroupThreadPool, jmxObjectThreadPool, "thread-pool-allocated-thread-count");
        registerJmxKeyAsMetric(metricRegistry, metricGroupThreadPool, jmxObjectThreadPool, "thread-pool-core-pool-size");
        registerJmxKeyAsMetric(metricRegistry, metricGroupThreadPool, jmxObjectThreadPool, "thread-pool-max-num-threads");
        registerJmxKeyAsMetric(metricRegistry, metricGroupThreadPool, jmxObjectThreadPool, "thread-pool-queued-task-count");
        registerJmxKeyAsMetric(metricRegistry, metricGroupThreadPool, jmxObjectThreadPool, "thread-pool-task-queue-overflow-count");
        registerJmxKeyAsMetric(metricRegistry, metricGroupThreadPool, jmxObjectThreadPool, "thread-pool-total-allocated-thread-count");
        registerJmxKeyAsMetric(metricRegistry, metricGroupThreadPool, jmxObjectThreadPool, "thread-pool-total-completed-tasks-count");
        registerJmxKeyAsMetric(metricRegistry, metricGroupThreadPool, jmxObjectThreadPool, "thread-pool-type");
    }

    private static void registerJmxKeyAsMetric(final MetricRegistry metricRegistry, final String metricGroup, final String jmxObjectName, final String jmxAttributeName) throws MalformedObjectNameException  {
        metricRegistry.register(MetricRegistry.name("http", "core", metricGroup, jmxAttributeName),
                new JmxAttributeGauge(new ObjectName(jmxObjectName), jmxAttributeName));
    }

    private void deployRestApi(final RexsterApplication application) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

        final WebappContext wacJersey = new WebappContext("jersey", "");

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

        final String defaultCharacterEncoding = properties.getString("http.character-set", "ISO-8859-1");
        rc.getContainerResponseFilters().add(new HeaderResponseFilter(defaultCharacterEncoding));

        HierarchicalConfiguration securityConfiguration = null;
        try {
            securityConfiguration = properties.configurationAt(Tokens.REXSTER_SECURITY_AUTH);
        } catch (IllegalArgumentException iae) {
            // do nothing...null is cool
        }

        final String securityFilterType = securityConfiguration != null ? securityConfiguration.getString("type") : Tokens.REXSTER_SECURITY_NONE;
        if (!securityFilterType.equals(Tokens.REXSTER_SECURITY_NONE)) {
            if (securityFilterType.equals(Tokens.REXSTER_SECURITY_DEFAULT)) {
                wacJersey.addContextInitParameter(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, DefaultSecurityFilter.class.getName());
                rc.getContainerRequestFilters().add(new DefaultSecurityFilter());
            } else {
                wacJersey.addContextInitParameter(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, securityFilterType);
                final Class clazz = Class.forName(securityFilterType, true, Thread.currentThread().getContextClassLoader());
                final AbstractSecurityFilter securityFilter = (AbstractSecurityFilter) clazz.newInstance();
                rc.getContainerRequestFilters().add(securityFilter);
            }
        }

        if (LogManager.getLoggerRepository().getThreshold().isGreaterOrEqual(Level.TRACE)) {

        }

        final ServletRegistration sg = wacJersey.addServlet("jersey", new ServletContainer(rc));
        sg.addMapping("/*");
        wacJersey.deploy(this.httpServer);
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
        final String absoluteWebRootPath = (new File(webRootPath)).getAbsolutePath();
        final ServerConfiguration config = this.httpServer.getServerConfiguration();
        config.addHttpHandler(new RexsterStaticHttpHandler(absoluteWebRootPath), "/static");
    }

    private void deployDogHouse(final RexsterApplication application) {
        // servlet that services all url from "main" by simply sending
        // main.html back to the calling client.  main.html handles its own
        // state given the uri
        final WebappContext wacDogHouse = new WebappContext("doghouse", "");
        final ServletRegistration sgDogHouse = wacDogHouse.addServlet("doghouse", new DogHouseServlet());
        sgDogHouse.addMapping("/doghouse/*");
        sgDogHouse.setInitParameter("com.tinkerpop.rexster.config.rexsterApiBaseUri", baseUri + ":" + rexsterServerPort.toString());

        final ServletRegistration sgDogHouseEval = wacDogHouse.addServlet("doghouse-evaluator", new EvaluatorServlet(application));
        sgDogHouseEval.addMapping("/doghouse/exec");

        wacDogHouse.deploy(this.httpServer);
    }

    private void deployMetricsAdmin(final RexsterApplication application) {
        // deploys the metrics servlet into rexster
        final WebappContext wacMetrics = new WebappContext("metrics", "");
        wacMetrics.setAttribute("com.yammer.metrics.servlets.MetricsServlet.registry", application.getMetricRegistry());
        wacMetrics.setAttribute("com.yammer.metrics.servlets.MetricsServlet.rateUnit", this.convertRateTo);
        wacMetrics.setAttribute("com.yammer.metrics.servlets.MetricsServlet.durationUnit", this.convertDurationTo);

        final ServletRegistration sgMetrics = wacMetrics.addServlet("metrics", new MetricsServlet());
        sgMetrics.addMapping("/metrics/*");

        wacMetrics.deploy(this.httpServer);
    }

    private NetworkListener configureNetworkListener() {
        final NetworkListener listener = new NetworkListener("grizzly", rexsterServerHost, rexsterServerPort);
        final ThreadPoolConfig workerThreadPoolConfig = ThreadPoolConfig.defaultConfig()
                .setCorePoolSize(coreWorkerThreadPoolSize)
                .setMaxPoolSize(maxWorkerThreadPoolSize);
        listener.getTransport().setWorkerThreadPoolConfig(workerThreadPoolConfig);
        final ThreadPoolConfig kernalThreadPoolConfig = ThreadPoolConfig.defaultConfig()
                .setCorePoolSize(coreKernalThreadPoolSize)
                .setMaxPoolSize(maxKernalThreadPoolSize);
        listener.getTransport().setKernelThreadPoolConfig(kernalThreadPoolConfig);

        listener.setMaxPostSize(maxPostSize);
        listener.setMaxHttpHeaderSize(maxHeaderSize);
        listener.setUploadTimeout(uploadTimeoutMillis);
        listener.setDisableUploadTimeout(false);

        return listener;
    }
}

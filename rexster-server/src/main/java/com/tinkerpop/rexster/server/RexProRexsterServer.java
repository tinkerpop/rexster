package com.tinkerpop.rexster.server;

import com.codahale.metrics.JmxAttributeGauge;
import com.codahale.metrics.MetricRegistry;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.filter.AbstractSecurityFilter;
import com.tinkerpop.rexster.filter.DefaultSecurityFilter;
import com.tinkerpop.rexster.protocol.session.RexProSessionMonitor;
import com.tinkerpop.rexster.protocol.filter.*;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.IOStrategy;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.monitoring.jmx.GrizzlyJmxManager;
import org.glassfish.grizzly.monitoring.jmx.JmxObject;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.threadpool.GrizzlyExecutorService;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.grizzly.utils.IdleTimeoutFilter;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.concurrent.TimeUnit;

/**
 * Initializes the TCP server that serves RexPro.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexProRexsterServer implements RexsterServer {

    private static final Logger logger = Logger.getLogger(RexProRexsterServer.class);

    private RexsterApplication app;
    private RexsterProperties properties;
    private Integer rexproServerPort;
    private String rexproServerHost;
    private final TCPNIOTransport tcpTransport;
    private boolean allowSessions;
    private int maxWorkerThreadPoolSize;
    private int coreWorkerThreadPoolSize;
    private int maxKernalThreadPoolSize;
    private int coreKernalThreadPoolSize;
    private long connectionIdleMax;
    private long connectionIdleInterval;
    private boolean enableJmx;
    private String ioStrategy;

    private JmxObject jmx;
    private RexProSessionMonitor rexProSessionMonitor = new RexProSessionMonitor();

    private String lastIoStrategy;
    private boolean lastEnableJmx;
    private int lastMaxWorkerThreadPoolSize;
    private int lastCoreWorkerThreadPoolSize;
    private int lastMaxKernalThreadPoolSize;
    private int lastCoreKernalThreadPoolSize;
    private long lastConnectionIdleMax;
    private long lastConnectionIdleInterval;

    public RexProRexsterServer(final XMLConfiguration configuration) {
        this(configuration, true);
    }

    public RexProRexsterServer(final XMLConfiguration configuration, final boolean allowSessions) {
        this(new RexsterProperties(configuration), allowSessions);
    }

    public RexProRexsterServer(final RexsterProperties properties, final boolean allowSessions) {
        this.allowSessions = allowSessions;
        this.properties = properties;
        updateSettings(properties.getConfiguration());

        // initialize the transport
        this.tcpTransport = TCPNIOTransportBuilder.newInstance().build();

        properties.addListener(new RexsterProperties.RexsterPropertiesListener() {
            @Override
            public void propertiesChanged(final XMLConfiguration configuration) {
            // maintain history of previous settings
            lastEnableJmx = enableJmx;
            lastIoStrategy = ioStrategy;
            lastMaxWorkerThreadPoolSize = maxWorkerThreadPoolSize;
            lastCoreWorkerThreadPoolSize = coreWorkerThreadPoolSize;
            lastMaxKernalThreadPoolSize = maxKernalThreadPoolSize;
            lastCoreKernalThreadPoolSize = coreKernalThreadPoolSize;
            lastConnectionIdleInterval = connectionIdleInterval;
            lastConnectionIdleMax = connectionIdleMax;

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
        this.tcpTransport.stop();
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

        // configure the tcp/nio transport
        this.configureTransport();

        if (hasEnableJmxChanged()) {
            if (this.enableJmx) {
                jmx = this.tcpTransport.getMonitoringConfig().createManagementObject();
                GrizzlyJmxManager.instance().registerAtRoot(jmx, "RexPro");
                manageJmxMetrics(application, true);
                logger.info("JMX enabled on RexPro.");
            } else {
                // only need to deregister if this is a restart.  on initial run, no jmx is enabled.
                if (jmx != null) {
                    try {
                        GrizzlyJmxManager.instance().deregister(jmx);
                        manageJmxMetrics(application, false);
                    } catch (IllegalArgumentException iae) {
                        logger.debug("Could not deregister JMX object on restart.  Perhaps it was never initially registered.");
                    } finally {
                        jmx = null;
                    }

                    logger.info("JMX disabled on RexPro.");
                }
            }
        }

        // start the transport if not already running.
        if (this.tcpTransport.isStopped()) {
            this.tcpTransport.start();
        }

        if (hasSessionIdleChanged()) {
            // initialize the session monitor for rexpro to clean up dead sessions.
            this.rexProSessionMonitor.reconfigure(this.connectionIdleInterval, this.connectionIdleMax);
        }
    }

    private void manageJmxMetrics(final RexsterApplication application, final boolean register) throws MalformedObjectNameException {
        // the JMX settings below pipe in metrics from Grizzly.
        final MetricRegistry metricRegistry = application.getMetricRegistry();
            manageMetricsFromJmx(metricRegistry, register);
            logger.info(register ? "Registered JMX Metrics." : "Removed JMX Metrics.");
    }

    private boolean hasEnableJmxChanged() {
        return this.enableJmx != this.lastEnableJmx;
    }

    private boolean hasIoStrategyChanged() {
        return !this.ioStrategy.equals(this.lastIoStrategy);
    }

    private boolean hasThreadPoolSizeChanged() {
        return this.maxKernalThreadPoolSize != lastMaxKernalThreadPoolSize || this.maxWorkerThreadPoolSize != lastMaxWorkerThreadPoolSize
                || this.coreKernalThreadPoolSize != lastCoreKernalThreadPoolSize || this.coreWorkerThreadPoolSize != this.lastCoreWorkerThreadPoolSize;
    }

    private boolean hasSessionIdleChanged() {
        return this.connectionIdleInterval != this.lastConnectionIdleInterval || this.connectionIdleMax != this.lastConnectionIdleMax;
    }

    private void updateSettings(final XMLConfiguration configuration) {
        this.rexproServerPort = configuration.getInteger("rexpro.server-port", new Integer(RexsterSettings.DEFAULT_REXPRO_PORT));
        this.rexproServerHost = configuration.getString("rexpro.server-host", "0.0.0.0");
        this.coreWorkerThreadPoolSize = configuration.getInt("rexpro.thread-pool.worker.core-size", 8);
        this.maxWorkerThreadPoolSize = configuration.getInt("rexpro.thread-pool.worker.max-size", 8);
        this.coreKernalThreadPoolSize = configuration.getInt("rexpro.thread-pool.kernal.core-size", 4);
        this.maxKernalThreadPoolSize = configuration.getInt("rexpro.thread-pool.kernal.max-size", 4);
        this.connectionIdleMax = configuration.getLong("rexpro.connection-max-idle", new Long(RexsterSettings.DEFAULT_REXPRO_SESSION_MAX_IDLE));
        this.connectionIdleInterval = configuration.getLong("rexpro.connection-check-interval", new Long(RexsterSettings.DEFAULT_REXPRO_SESSION_CHECK_INTERVAL));
        this.enableJmx = configuration.getBoolean("rexpro.enable-jmx", false);
        this.ioStrategy = configuration.getString("rexpro.io-strategy", "leader-follower");
    }

    private FilterChain constructFilterChain(final RexsterApplication application) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        final FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new IdleTimeoutFilter(
                IdleTimeoutFilter.createDefaultIdleDelayedExecutor(this.connectionIdleInterval, TimeUnit.MILLISECONDS),
                this.connectionIdleMax, TimeUnit.MILLISECONDS));
        filterChainBuilder.add(new RexProServerFilter(application));

        HierarchicalConfiguration securityConfiguration = properties.getSecuritySettings();
        final String securityFilterType = securityConfiguration != null ? securityConfiguration.getString("type") : Tokens.REXSTER_SECURITY_NONE;
        if (securityFilterType.equals(Tokens.REXSTER_SECURITY_NONE)) {
            logger.info("Rexster configured with no security.");
        } else {
            final AbstractSecurityFilter filter;
            if (securityFilterType.equals(Tokens.REXSTER_SECURITY_DEFAULT)) {
                filter = new DefaultSecurityFilter();
                filterChainBuilder.add(filter);
            } else {
                filter = (AbstractSecurityFilter) Class.forName(securityFilterType).newInstance();
                filterChainBuilder.add(filter);
            }

            filter.configure(properties.getConfiguration());

            logger.info("Rexster configured with [" + filter.getName() + "].");
        }

        filterChainBuilder.add(new RexProProcessorFilter());
        return filterChainBuilder.build();
    }

    private static void manageMetricsFromJmx(final MetricRegistry metricRegistry, final boolean register) throws MalformedObjectNameException {
        final String jmxObjectMemoryManager = "org.glassfish.grizzly:pp=/gmbal-root/TCPNIOTransport[RexPro],type=HeapMemoryManager,name=MemoryManager";
        final String metricGroupMemoryManager = "heap-memory-manager";
        final String[] heapMemoryManagerMetrics = new String[] {
                "pool-allocated-bytes", "pool-released-bytes", "real-allocated-bytes", "total-allocated-bytes"
        };

        manageJmxKeysAsMetric(metricRegistry, jmxObjectMemoryManager, metricGroupMemoryManager, heapMemoryManagerMetrics, register);

        final String jmxObjectTcpNioTransport = "org.glassfish.grizzly:pp=/gmbal-root,type=TCPNIOTransport,name=RexPro";
        final String metricGroupTcpNioTransport = "tcp-nio-transport";
        final String[] tcpNioTransportMetrics = new String[] {
                "bound-addresses", "bytes-read" , "bytes-written", "client-connect-timeout-millis", "io-strategy",
                "open-connections-count", "read-buffer-size", "selector-threads-count", "server-socket-so-timeout",
                "total-connections-count", "write-buffer-size"
        };

        manageJmxKeysAsMetric(metricRegistry, jmxObjectTcpNioTransport, metricGroupTcpNioTransport, tcpNioTransportMetrics, register);

        final String jmxObjectThreadPool = "org.glassfish.grizzly:pp=/gmbal-root/TCPNIOTransport[RexPro],type=ThreadPool,name=ThreadPool";
        final String metricGroupThreadPool = "thread-pool";
        final String[] threadPoolMetrics = new String [] {
                "thread-pool-allocated-thread-count", "thread-pool-core-pool-size", "thread-pool-max-num-threads",
                "thread-pool-queued-task-count", "thread-pool-task-queue-overflow-count",
                "thread-pool-total-allocated-thread-count", "thread-pool-total-completed-tasks-count",
                "thread-pool-type"
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
        metricRegistry.register(MetricRegistry.name("rexpro", "core", metricGroup, jmxAttributeName),
                new JmxAttributeGauge(new ObjectName(jmxObjectName), jmxAttributeName));
    }

    private static void deregisterJmxKeyAsMetric(final MetricRegistry metricRegistry, final String metricGroup,
                                                 final String jmxAttributeName) throws MalformedObjectNameException  {
        metricRegistry.remove(MetricRegistry.name("rexpro", "core", metricGroup, jmxAttributeName));
    }

    private void configureTransport() throws Exception {
        if (this.hasIoStrategyChanged()) {
            final IOStrategy strategy = GrizzlyIoStrategyFactory.createIoStrategy(this.ioStrategy);
            this.tcpTransport.setIOStrategy(strategy);

            logger.info(String.format("Using %s IOStrategy for RexPro.", strategy.getClass().getName()));
        }

        if (hasThreadPoolSizeChanged()) {
            final ThreadPoolConfig workerThreadPoolConfig = ThreadPoolConfig.defaultConfig()
                    .setCorePoolSize(coreWorkerThreadPoolSize)
                    .setMaxPoolSize(maxWorkerThreadPoolSize);
            tcpTransport.setWorkerThreadPoolConfig(workerThreadPoolConfig);
            final ThreadPoolConfig kernalThreadPoolConfig = ThreadPoolConfig.defaultConfig()
                    .setCorePoolSize(coreKernalThreadPoolSize)
                    .setMaxPoolSize(maxKernalThreadPoolSize);
            tcpTransport.setKernelThreadPoolConfig(kernalThreadPoolConfig);

            // if the threadpool is initialized then call reconfigure to reset the threadpool
            if (tcpTransport.getKernelThreadPool() != null) {
                ((GrizzlyExecutorService) tcpTransport.getKernelThreadPool()).reconfigure(kernalThreadPoolConfig);
            }

            if (tcpTransport.getWorkerThreadPool() != null) {
                ((GrizzlyExecutorService) tcpTransport.getWorkerThreadPool()).reconfigure(workerThreadPoolConfig);
            }

            logger.info(String.format("RexPro thread pool configuration: kernal[%s / %s] worker[%s / %s] ",
                    coreKernalThreadPoolSize, maxKernalThreadPoolSize,
                    coreWorkerThreadPoolSize, maxWorkerThreadPoolSize));

        }

        // when the processor is reset, the port/host have to be rebound.
        this.tcpTransport.setProcessor(constructFilterChain(app));
        this.tcpTransport.unbindAll();
        this.tcpTransport.bind(rexproServerHost, rexproServerPort);

        logger.info(String.format("RexPro Server bound to [%s:%s]", rexproServerHost, rexproServerPort));
    }
}

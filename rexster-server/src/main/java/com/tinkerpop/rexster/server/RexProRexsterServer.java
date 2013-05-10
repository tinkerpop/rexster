package com.tinkerpop.rexster.server;

import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.filter.AbstractSecurityFilter;
import com.tinkerpop.rexster.filter.DefaultSecurityFilter;
import com.tinkerpop.rexster.protocol.RexProSessionMonitor;
import com.tinkerpop.rexster.protocol.filter.RexProMessageFilter;
import com.tinkerpop.rexster.protocol.filter.ScriptFilter;
import com.tinkerpop.rexster.protocol.filter.SessionFilter;
import com.yammer.metrics.JmxAttributeGauge;
import com.yammer.metrics.MetricRegistry;
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
    private int connectionIdleMax;
    private int connectionIdleInterval;
    private boolean enableJmx;
    private String ioStrategy;

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

        this.tcpTransport = configureTransport();

        properties.assignListener(new RexsterProperties.RexsterPropertiesListener() {
            @Override
            public void propertiesChanged(final XMLConfiguration configuration) {
                updateSettings(configuration);

                try {
                    restart(app, true);
                } catch (Exception ex) {
                    logger.error("Could not modify Rexster configuration.  Please restart Rexster to allow changes to be applied.", ex);
                }
            }
        });
    }

    private void updateSettings(final XMLConfiguration configuration) {
        this.rexproServerPort = configuration.getInteger("rexpro.server-port", new Integer(RexsterSettings.DEFAULT_REXPRO_PORT));
        this.rexproServerHost = configuration.getString("rexpro.server-host", "0.0.0.0");
        this.coreWorkerThreadPoolSize = configuration.getInt("rexpro.thread-pool.worker.core-size", 8);
        this.maxWorkerThreadPoolSize = configuration.getInt("rexpro.thread-pool.worker.max-size", 8);
        this.coreKernalThreadPoolSize = configuration.getInt("rexpro.thread-pool.kernal.core-size", 4);
        this.maxKernalThreadPoolSize = configuration.getInt("rexpro.thread-pool.kernal.max-size", 4);
        this.connectionIdleMax = configuration.getInt("rexpro.connection-max-idle", 180000);
        this.connectionIdleInterval = configuration.getInt("rexpro.connection-check-interval", 3000000);
        this.enableJmx = configuration.getBoolean("rexpro.enable-jmx", false);
        this.ioStrategy = configuration.getString("rexpro.io-strategy", "leader-follower");
    }

    @Override
    public void stop() throws Exception {
        this.tcpTransport.stop();
    }

    @Override
    public void start(final RexsterApplication application) throws Exception {
        this.app = application;
        restart(application, false);
    }

    public void restart(final RexsterApplication application, final boolean restart) throws Exception {
        final IOStrategy strategy = GrizzlyIoStrategyFactory.createIoStrategy(this.ioStrategy);

        logger.info(String.format("Using %s IOStrategy for RexPro.", strategy.getClass().getName()));

        this.tcpTransport.setIOStrategy(strategy);
        this.tcpTransport.setProcessor(constructFilterChain(application));

        // unbind everything first then bind back if changed.
        // TODO: maybe it should only unbind if there is a change
        this.tcpTransport.unbindAll();
        this.tcpTransport.bind(rexproServerHost, rexproServerPort);

        if (this.enableJmx) {
            final JmxObject jmx = this.tcpTransport.getMonitoringConfig().createManagementObject();
            GrizzlyJmxManager.instance().registerAtRoot(jmx, "RexPro");

            // the JMX settings below pipe in metrics from Grizzly.  don't register twice.
            if (!restart) {
                final MetricRegistry metricRegistry = application.getMetricRegistry();
                registerMetricsFromJmx(metricRegistry);
            }
        } else {
            // only need to deregister if this is a restart.  on initial run, no jmx is enabled.
            if (restart) {
                final JmxObject jmx = this.tcpTransport.getMonitoringConfig().createManagementObject();
                try {
                    GrizzlyJmxManager.instance().deregister(jmx);
                } catch (IllegalArgumentException iae) {
                    logger.debug("Could not deregister JMX object on restart.  Perhaps it was never initially registered.");
                }
            }
        }

        // no need to restart the transport if already running
        if (!restart) {
            this.tcpTransport.start();
        }

        // TODO: let's just not try to reconfigure this right now....................
        if (!restart) {
            // initialize the session monitor for rexpro to clean up dead sessions.
            final Long rexProSessionMaxIdle = properties.getRexProSessionMaxIdle();
            final Long rexProSessionCheckInterval = properties.getRexProSessionCheckInterval();
            new RexProSessionMonitor(rexProSessionMaxIdle, rexProSessionCheckInterval);
        }

        logger.info("RexPro serving on port: [" + rexproServerPort + "]");
    }

    private FilterChain constructFilterChain(final RexsterApplication application) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        final FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new IdleTimeoutFilter(
                IdleTimeoutFilter.createDefaultIdleDelayedExecutor(this.connectionIdleInterval, TimeUnit.MILLISECONDS),
                this.connectionIdleMax, TimeUnit.MILLISECONDS));
        filterChainBuilder.add(new RexProMessageFilter());

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

        if (this.allowSessions) {
            filterChainBuilder.add(new SessionFilter(application));
        }

        filterChainBuilder.add(new ScriptFilter(application));
        return filterChainBuilder.build();
    }

    private static void registerMetricsFromJmx(final MetricRegistry metricRegistry) throws MalformedObjectNameException {
        final String jmxObjectMemoryManager = "org.glassfish.grizzly:pp=/gmbal-root/TCPNIOTransport[RexPro],type=HeapMemoryManager,name=MemoryManager";
        final String metricGroupMemoryManager = "heap-memory-manager";
        registerJmxKeyAsMetric(metricRegistry, metricGroupMemoryManager, jmxObjectMemoryManager, "pool-allocated-bytes");
        registerJmxKeyAsMetric(metricRegistry, metricGroupMemoryManager, jmxObjectMemoryManager, "pool-released-bytes");
        registerJmxKeyAsMetric(metricRegistry, metricGroupMemoryManager, jmxObjectMemoryManager, "real-allocated-bytes");
        registerJmxKeyAsMetric(metricRegistry, metricGroupMemoryManager, jmxObjectMemoryManager, "total-allocated-bytes");

        final String jmxObjectTcpNioTransport = "org.glassfish.grizzly:pp=/gmbal-root,type=TCPNIOTransport,name=RexPro";
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

        final String jmxObjectThreadPool = "org.glassfish.grizzly:pp=/gmbal-root/TCPNIOTransport[RexPro],type=ThreadPool,name=ThreadPool";
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
        metricRegistry.register(MetricRegistry.name("rexpro", "core", metricGroup, jmxAttributeName),
                new JmxAttributeGauge(new ObjectName(jmxObjectName), jmxAttributeName));
    }


    private TCPNIOTransport configureTransport() {
        final TCPNIOTransport tcpTransport = TCPNIOTransportBuilder.newInstance().build();
        final ThreadPoolConfig workerThreadPoolConfig = ThreadPoolConfig.defaultConfig()
                .setCorePoolSize(coreWorkerThreadPoolSize)
                .setMaxPoolSize(maxWorkerThreadPoolSize);
        tcpTransport.setWorkerThreadPoolConfig(workerThreadPoolConfig);
        final ThreadPoolConfig kernalThreadPoolConfig = ThreadPoolConfig.defaultConfig()
                .setCorePoolSize(coreKernalThreadPoolSize)
                .setMaxPoolSize(maxKernalThreadPoolSize);
        tcpTransport.setKernelThreadPoolConfig(kernalThreadPoolConfig);

        return tcpTransport;
    }
}

package com.tinkerpop.rexster.server;

import com.tinkerpop.rexster.filter.AbstractSecurityFilter;
import com.tinkerpop.rexster.filter.DefaultSecurityFilter;
import com.tinkerpop.rexster.protocol.RexProSessionMonitor;
import com.tinkerpop.rexster.protocol.filter.RexProMessageFilter;
import com.tinkerpop.rexster.protocol.filter.ScriptFilter;
import com.tinkerpop.rexster.protocol.filter.SessionFilter;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.IOStrategy;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

/**
 * Initializes the TCP server that serves RexPro.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexProRexsterServer implements RexsterServer {

    private static final Logger logger = Logger.getLogger(RexProRexsterServer.class);
    private final XMLConfiguration properties;
    private final Integer rexproServerPort;
    private final String rexproServerHost;
    private final TCPNIOTransport tcpTransport;
    private final boolean allowSessions;
    private final int maxWorkerThreadPoolSize;
    private final int coreWorkerThreadPoolSize;
    private final int maxKernalThreadPoolSize;
    private final int coreKernalThreadPoolSize;
    private final String ioStrategy;

    public RexProRexsterServer(final XMLConfiguration properties) {
        this(properties, true);
    }

    public RexProRexsterServer(final XMLConfiguration properties, final boolean allowSessions) {
        this.allowSessions = allowSessions;
        this.properties = properties;
        this.rexproServerPort = properties.getInteger("rexpro.server-port", new Integer(RexsterSettings.DEFAULT_REXPRO_PORT));
        this.rexproServerHost = properties.getString("rexpro.server-host", "0.0.0.0");
        this.coreWorkerThreadPoolSize = properties.getInt("rexpro.thread-pool.worker.core-size", 8);
        this.maxWorkerThreadPoolSize = properties.getInt("rexpro.thread-pool.worker.max-size", 8);
        this.coreKernalThreadPoolSize = properties.getInt("rexpro.thread-pool.kernal.core-size", 4);
        this.maxKernalThreadPoolSize = properties.getInt("rexpro.thread-pool.kernal.max-size", 4);
        this.ioStrategy = properties.getString("rexpro.io-strategy", "worker");

        this.tcpTransport = configureTransport();
    }

    @Override
    public void stop() throws Exception {
        this.tcpTransport.stop();
    }

    @Override
    public void start(final RexsterApplication application) throws Exception {
        final FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new RexProMessageFilter());

        final HierarchicalConfiguration securityConfiguration = properties.configurationAt("security.authentication");
        final String securityFilterType = securityConfiguration.getString("type");
        if (securityFilterType.equals("none")) {
            logger.info("Rexster configured with no security.");
        } else {
            final AbstractSecurityFilter filter;
            if (securityFilterType.equals("default")) {
                filter = new DefaultSecurityFilter();
                filterChainBuilder.add(filter);
            } else {
                filter = (AbstractSecurityFilter) Class.forName(securityFilterType).newInstance();
                filterChainBuilder.add(filter);
            }

            filter.configure(properties);

            logger.info("Rexster configured with [" + filter.getName() + "].");
        }

        if (this.allowSessions) {
            filterChainBuilder.add(new SessionFilter(application));
        }

        filterChainBuilder.add(new ScriptFilter(application));

        final IOStrategy strategy = GrizzlyIoStrategyFactory.createIoStrategy(this.ioStrategy);

        logger.info(String.format("Using %s IOStrategy for RexPro.", strategy.getClass().getName()));

        this.tcpTransport.setIOStrategy(strategy);
        this.tcpTransport.setProcessor(filterChainBuilder.build());
        this.tcpTransport.bind(rexproServerHost, rexproServerPort);

        this.tcpTransport.start();

        // initialize the session monitor for rexpro to clean up dead sessions.
        final Long rexProSessionMaxIdle = properties.getLong("rexpro.session-max-idle",
                new Long(RexsterSettings.DEFAULT_REXPRO_SESSION_MAX_IDLE));
        final Long rexProSessionCheckInterval = properties.getLong("rexpro.session-check-interval",
                new Long(RexsterSettings.DEFAULT_REXPRO_SESSION_CHECK_INTERVAL));
        new RexProSessionMonitor(rexProSessionMaxIdle, rexProSessionCheckInterval);

        logger.info("RexPro serving on port: [" + rexproServerPort + "]");
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

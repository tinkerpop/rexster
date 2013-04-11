package com.tinkerpop.rexster.server.metrics;

import com.tinkerpop.rexster.Tokens;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.graphite.Graphite;
import com.yammer.metrics.graphite.GraphiteReporter;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Configures Graphite as a reporter.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class GraphiteReporterConfig extends AbstractHostPortReporterConfig {
    private static final Logger logger = Logger.getLogger(GraphiteReporterConfig.class);

    private final String prefix;

    private final MetricRegistry metricRegistry;

    public GraphiteReporterConfig(final HierarchicalConfiguration config, final MetricRegistry metricRegistry) {
        final SubnodeConfiguration c = config.configurationAt(Tokens.REXSTER_GRAPH_PROPERTIES);

        this.metricRegistry = metricRegistry;

        this.timeUnit = c.getString(Tokens.REXSTER_REPORTER_TIME_UNIT, DEFAULT_TIME_UNIT);
        this.period = c.getLong(Tokens.REXSTER_REPORTER_PERIOD, DEFAULT_PERIOD);
        this.convertRateTo = c.getString(Tokens.REXSTER_REPORTER_RATES_TIME_UNIT, DEFAULT_TIME_UNIT);
        this.convertDurationTo = c.getString(Tokens.REXSTER_REPORTER_DURATION_TIME_UNIT, DEFAULT_TIME_UNIT);
        this.hostsString = c.getString(Tokens.REXSTER_REPORTER_HOSTS, "localhost:2003");
        this.prefix = c.getString(Tokens.REXSTER_REPORTER_PREFIX, "");
    }

    @Override
    public List<HostPort> getFullHostList()
    {
        return getHostListAndStringList();
    }

    @Override
    public boolean enable()
    {
        final List<HostPort> hosts = getFullHostList();
        if (hosts == null || hosts.isEmpty())
        {
            logger.error("No hosts specified, cannot enable GraphiteReporter");
            return false;
        }
        for (HostPort hostPort : hosts)
        {
            try
            {
                logger.info(String.format("Enabling GraphiteReporter to %s:%s", hostPort.getHost(), hostPort.getPort()));

                final Graphite graphite = new Graphite(new InetSocketAddress(hostPort.getHost(), hostPort.getPort()));
                GraphiteReporter.forRegistry(this.metricRegistry)
                        .convertDurationsTo(this.getRealConvertDurationTo())
                        .convertRatesTo(this.getRealConvertRateTo())
                        .prefixedWith(this.prefix)
                        .build(graphite).start(this.getPeriod(), this.getRealTimeUnit());

            }
            catch (Exception e)
            {
                logger.error("Failed to enable GraphiteReporter", e);
                return false;
            }
        }
        return true;
    }
}

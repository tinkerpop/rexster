package com.tinkerpop.rexster.server.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Configures Graphite as a reporter.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GraphiteReporterConfig extends AbstractHostPortReporterConfig {
    private static final Logger logger = Logger.getLogger(GraphiteReporterConfig.class);

    private final String prefix;

    private final List<GraphiteReporter> reporters = new ArrayList<GraphiteReporter>();

    public GraphiteReporterConfig(final HierarchicalConfiguration config, final MetricRegistry metricRegistry) {
        super(config, metricRegistry);
        this.hostsString = this.registryConfiguration.getString(Tokens.REXSTER_REPORTER_HOSTS, "localhost:2003");
        this.prefix = this.registryConfiguration.getString(Tokens.REXSTER_REPORTER_PREFIX, "");

        logger.info(String.format("Configured Graphite Metric Reporter [%s].", this.hostsString));
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
                final GraphiteReporter reporter = GraphiteReporter.forRegistry(this.metricRegistry)
                        .convertDurationsTo(this.getRealDurationTimeUnitConversion())
                        .convertRatesTo(this.getRealRateTimeUnitConversion())
                        .prefixedWith(this.prefix)
                        .filter(new RegexMetricFilter(this.inclusion, this.exclusion))
                        .build(graphite);
                reporter.start(this.period, this.getRealTimeUnit());

                reporters.add(reporter);

            }
            catch (Exception e)
            {
                logger.error("Failed to enable GraphiteReporter", e);
                return false;
            }
        }
        return true;
    }

    @Override
    public void disable() {
        for (GraphiteReporter reporter : reporters) {
            reporter.stop();
        }

        reporters.clear();
    }
}

package com.tinkerpop.rexster.server.metrics;

import com.tinkerpop.rexster.Tokens;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.ganglia.GangliaReporter;
import info.ganglia.gmetric4j.gmetric.GMetric;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Configures Ganglia as a reporter.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class GangliaReporterConfig extends AbstractHostPortReporterConfig {
    private static final Logger logger = Logger.getLogger(GangliaReporterConfig.class);

    private final MetricRegistry metricRegistry;

    public GangliaReporterConfig(final HierarchicalConfiguration config, final MetricRegistry metricRegistry) {
        final SubnodeConfiguration c = config.configurationAt(Tokens.REXSTER_GRAPH_PROPERTIES);

        this.metricRegistry = metricRegistry;

        this.timeUnit = c.getString(Tokens.REXSTER_REPORTER_TIME_UNIT, DEFAULT_TIME_UNIT);
        this.period = c.getLong(Tokens.REXSTER_REPORTER_PERIOD, DEFAULT_PERIOD);
        this.convertRateTo = c.getString(Tokens.REXSTER_REPORTER_RATES_TIME_UNIT, DEFAULT_TIME_UNIT);
        this.convertDurationTo = c.getString(Tokens.REXSTER_REPORTER_DURATION_TIME_UNIT, DEFAULT_TIME_UNIT);
        this.hostsString = c.getString(Tokens.REXSTER_REPORTER_HOSTS, "localhost:8649");
    }

    @Override
    public List<HostPort> getFullHostList() {
        return getHostListAndStringList();
    }

    @Override
    public boolean enable()
    {
        final List<HostPort> hosts = getFullHostList();
        if (hosts == null || hosts.isEmpty())
        {
            logger.error("No hosts specified, cannot enable GangliaReporter");
            return false;
        }

        try
        {
            for (HostPort hostPort : hosts) {
                final GMetric ganglia = new GMetric(hostPort.getHost(), hostPort.getPort(), GMetric.UDPAddressingMode.MULTICAST, 1);
                GangliaReporter.forRegistry(this.metricRegistry)
                        .convertDurationsTo(this.getRealConvertDurationTo())
                        .convertRatesTo(this.getRealConvertRateTo())
                        .build(ganglia).start(this.getPeriod(), this.getRealTimeUnit());
            }
        }
        catch (Exception e)
        {
            logger.error("Failure while enabling ganglia reporter", e);
            return false;
        }
        return true;
    }
}

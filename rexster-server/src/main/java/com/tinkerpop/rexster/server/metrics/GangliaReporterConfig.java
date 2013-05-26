package com.tinkerpop.rexster.server.metrics;

import com.tinkerpop.rexster.Tokens;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ganglia.GangliaReporter;
import info.ganglia.gmetric4j.gmetric.GMetric;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Configures Ganglia as a reporter.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GangliaReporterConfig extends AbstractHostPortReporterConfig {
    private static final Logger logger = Logger.getLogger(GangliaReporterConfig.class);

    public GangliaReporterConfig(final HierarchicalConfiguration config, final MetricRegistry metricRegistry) {
        super(config, metricRegistry);
        this.hostsString = this.registryConfiguration.getString(Tokens.REXSTER_REPORTER_HOSTS, "localhost:8649");

        logger.info(String.format("Configured Ganglia Metric Reporter [%s].", this.hostsString));
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
                        .convertDurationsTo(this.getRealDurationTimeUnitConversion())
                        .convertRatesTo(this.getRealRateTimeUnitConversion())
                        .filter(new RegexMetricFilter(this.inclusion, this.exclusion))
                        .build(ganglia).start(this.period, this.getRealTimeUnit());
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

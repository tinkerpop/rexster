package com.tinkerpop.rexster.server.metrics;

import com.tinkerpop.rexster.Tokens;
import com.yammer.metrics.ConsoleReporter;
import com.yammer.metrics.MetricRegistry;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Configures a reporter that writes to the console.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ConsoleReporterConfig extends AbstractReporterConfig {
    private static final Logger logger = Logger.getLogger(ConsoleReporterConfig.class);

    private final MetricRegistry metricRegistry;

    public ConsoleReporterConfig(final HierarchicalConfiguration config, final MetricRegistry metricRegistry) {
        final SubnodeConfiguration c = config.configurationAt(Tokens.REXSTER_GRAPH_PROPERTIES);

        this.metricRegistry = metricRegistry;

        readCommonConfiguration(c);
    }

    @Override
    public boolean enable()
    {
        try
        {
            ConsoleReporter.forRegistry(this.metricRegistry)
                    .convertDurationsTo(this.getRealConvertDurationTo())
                    .convertRatesTo(this.getRealConvertRateTo())
                    .filter(new RegexMetricFilter(this.inclusion, this.exclusion))
                    .build().start(this.period, this.getRealTimeUnit());
        }
        catch (Exception e)
        {
            logger.error("Failure while enabling console reporter", e);
            return false;
        }
        return true;
    }
}

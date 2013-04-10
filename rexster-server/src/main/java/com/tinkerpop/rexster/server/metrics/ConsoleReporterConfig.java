package com.tinkerpop.rexster.server.metrics;

import com.tinkerpop.rexster.Tokens;
import com.yammer.metrics.ConsoleReporter;
import com.yammer.metrics.MetricRegistry;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ConsoleReporterConfig extends AbstractReporterConfig {
    private static final Logger logger = Logger.getLogger(ConsoleReporterConfig.class);

    private final MetricRegistry metricRegistry;

    public ConsoleReporterConfig(final HierarchicalConfiguration config, final MetricRegistry metricRegistry) {
        final SubnodeConfiguration c = config.configurationAt(Tokens.REXSTER_GRAPH_PROPERTIES);

        this.metricRegistry = metricRegistry;

        this.setTimeunit(c.getString("report-time-unit", TimeUnit.SECONDS.toString()));
        this.setPeriod(c.getInt("report-period", 60));
        this.setConvertRateTo(c.getString("rates-time-unit", TimeUnit.SECONDS.toString()));
        this.setConvertDurationTo(c.getString("duration-time-unit", TimeUnit.SECONDS.toString()));
    }

    @Override
    public boolean enable()
    {
        try
        {
            ConsoleReporter.forRegistry(this.metricRegistry)
                    .convertDurationsTo(this.getRealConvertDurationTo())
                    .convertRatesTo(this.getRealConvertRateTo())
                    .build().start(this.getPeriod(), this.getRealTimeunit());
        }
        catch (Exception e)
        {
            logger.error("Failure while enabling console reporter", e);
            return false;
        }
        return true;
    }
}

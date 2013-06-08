package com.tinkerpop.rexster.server.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.Logger;

/**
 * Configures a reporter that writes to the console.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class ConsoleReporterConfig extends AbstractReporterConfig {
    private static final Logger logger = Logger.getLogger(ConsoleReporterConfig.class);

    private ConsoleReporter consoleReporter;

    public ConsoleReporterConfig(final HierarchicalConfiguration config, final MetricRegistry metricRegistry) {
        super(config, metricRegistry);

        logger.info("Configured Console Metric Reporter.");
    }

    @Override
    public boolean enable() {
        try
        {
            consoleReporter = ConsoleReporter.forRegistry(this.metricRegistry)
                    .convertDurationsTo(this.getRealDurationTimeUnitConversion())
                    .convertRatesTo(this.getRealRateTimeUnitConversion())
                    .filter(new RegexMetricFilter(this.inclusion, this.exclusion))
                    .build();
            consoleReporter.start(this.period, this.getRealTimeUnit());
        }
        catch (Exception e)
        {
            logger.error("Failure while enabling console reporter", e);
            return false;
        }
        return true;
    }

    @Override
    public void disable() {
        consoleReporter.stop();
    }
}

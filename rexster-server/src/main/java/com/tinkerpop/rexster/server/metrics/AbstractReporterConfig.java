package com.tinkerpop.rexster.server.metrics;

import com.codahale.metrics.MetricRegistry;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

import java.util.concurrent.TimeUnit;

/**
 * Base class for reporter configurations.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public abstract class AbstractReporterConfig
{
    public static final long DEFAULT_PERIOD = 60l;
    public static final String DEFAULT_TIME_UNIT = TimeUnit.SECONDS.toString();
    protected final MetricRegistry metricRegistry;

    protected SubnodeConfiguration registryConfiguration;

    protected long period;

    protected String timeUnit;

    protected String rateTimeUnitConversion;

    protected String durationTimeUnitConversion;

    protected String inclusion;

    protected String exclusion;

    public AbstractReporterConfig(final HierarchicalConfiguration config, final MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        try {
            this.registryConfiguration = config.configurationAt(Tokens.REXSTER_GRAPH_PROPERTIES);
        } catch (IllegalArgumentException iae) {
            this.registryConfiguration = null;
        }

        readCommonConfiguration();
    }

    public TimeUnit getRealTimeUnit() {
        return TimeUnit.valueOf(timeUnit);
    }

    public TimeUnit getRealRateTimeUnitConversion() {
        return TimeUnit.valueOf(rateTimeUnitConversion);
    }

    public TimeUnit getRealDurationTimeUnitConversion() {
        return TimeUnit.valueOf(durationTimeUnitConversion);
    }

    /**
     * Enable (start) a reporter configuration.
     * @return
     */
    public abstract boolean enable();

    public abstract void disable();

    private void readCommonConfiguration() {
        this.timeUnit = this.registryConfiguration == null ? DEFAULT_TIME_UNIT : this.registryConfiguration.getString(Tokens.REXSTER_REPORTER_TIME_UNIT, DEFAULT_TIME_UNIT);
        this.period = this.registryConfiguration == null ? DEFAULT_PERIOD : this.registryConfiguration.getLong(Tokens.REXSTER_REPORTER_PERIOD, DEFAULT_PERIOD);
        this.rateTimeUnitConversion = this.registryConfiguration == null ? DEFAULT_TIME_UNIT : this.registryConfiguration.getString(Tokens.REXSTER_REPORTER_RATES_TIME_UNIT, DEFAULT_TIME_UNIT);
        this.durationTimeUnitConversion = this.registryConfiguration == null ? DEFAULT_TIME_UNIT : this.registryConfiguration.getString(Tokens.REXSTER_REPORTER_DURATION_TIME_UNIT, DEFAULT_TIME_UNIT);
        this.inclusion = this.registryConfiguration == null ? null : this.registryConfiguration.getString(Tokens.REXSTER_REPORTER_INCLUDES, null);
        this.exclusion = this.registryConfiguration == null ? null : this.registryConfiguration.getString(Tokens.REXSTER_REPORTER_EXCLUDES, null);
    }
}
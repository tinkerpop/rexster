package com.tinkerpop.rexster.server.metrics;

import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.SubnodeConfiguration;

import java.util.concurrent.TimeUnit;

/**
 * Base class for reporter configurations.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public abstract class AbstractReporterConfig
{
    protected static final long DEFAULT_PERIOD = 60l;
    protected static final String DEFAULT_TIME_UNIT = TimeUnit.SECONDS.toString();

    protected long period;

    protected String timeUnit;

    protected String convertRateTo;

    protected String convertDurationTo;

    protected String inclusion;

    protected String exclusion;

    public TimeUnit getRealTimeUnit()
    {
        return TimeUnit.valueOf(timeUnit);
    }

    public TimeUnit getRealConvertRateTo()
    {
        return TimeUnit.valueOf(convertRateTo);
    }

    public TimeUnit getRealConvertDurationTo()
    {
        return TimeUnit.valueOf(convertDurationTo);
    }

    /**
     * Enable (start) a reporter configuration.
     * @return
     */
    public abstract boolean enable();

    protected void readCommonConfiguration(SubnodeConfiguration c) {
        this.timeUnit = c.getString(Tokens.REXSTER_REPORTER_TIME_UNIT, DEFAULT_TIME_UNIT);
        this.period = c.getLong(Tokens.REXSTER_REPORTER_PERIOD, DEFAULT_PERIOD);
        this.convertRateTo = c.getString(Tokens.REXSTER_REPORTER_RATES_TIME_UNIT, DEFAULT_TIME_UNIT);
        this.convertDurationTo = c.getString(Tokens.REXSTER_REPORTER_DURATION_TIME_UNIT, DEFAULT_TIME_UNIT);
        this.inclusion = c.getString(Tokens.REXSTER_REPORTER_INCLUDES, null);
        this.exclusion = c.getString(Tokens.REXSTER_REPORTER_EXCLUDES, null);
    }
}
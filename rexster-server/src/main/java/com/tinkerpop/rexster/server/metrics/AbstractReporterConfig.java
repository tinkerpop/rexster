package com.tinkerpop.rexster.server.metrics;

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

    public long getPeriod()
    {
        return period;
    }

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
}
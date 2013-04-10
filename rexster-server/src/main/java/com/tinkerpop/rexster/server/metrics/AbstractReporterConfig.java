package com.tinkerpop.rexster.server.metrics;

import java.util.concurrent.TimeUnit;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public abstract class AbstractReporterConfig
{
    private long period;

    private String timeunit;

    private String convertRateTo;

    private String convertDurationTo;

    public long getPeriod()
    {
        return period;
    }

    public void setPeriod(long period)
    {
        this.period = period;
    }

    public String getTimeunit()
    {
        return timeunit;
    }

    public void setTimeunit(String timeunit)
    {
        this.timeunit = timeunit;
    }

    public TimeUnit getRealTimeunit()
    {
        return TimeUnit.valueOf(timeunit);
    }

    public void setConvertRateTo(String convertRateTo) {
        this.convertRateTo = convertRateTo;
    }

    public TimeUnit getRealConvertRateTo()
    {
        return TimeUnit.valueOf(convertRateTo);
    }

    public void setConvertDurationTo(String convertDurationTo) {
        this.convertDurationTo = convertDurationTo;
    }

    public TimeUnit getRealConvertDurationTo()
    {
        return TimeUnit.valueOf(convertDurationTo);
    }

    protected boolean isClassAvailable(String className)
    {
        try
        {
            Class.forName(className);
            return true;
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
    }

    public abstract boolean enable();
}
package com.tinkerpop.rexster.server.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

import java.util.regex.Pattern;

/**
 * A filter that includes or excludes metrics using regex.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RegexMetricFilter implements MetricFilter {

    private final Pattern includes;
    private final Pattern excludes;

    public RegexMetricFilter(final String includes, final String excludes) {
        this.includes = includes == null ? null : Pattern.compile(includes);
        this.excludes = excludes == null ? null : Pattern.compile(excludes);
    }

    @Override
    public boolean matches(final String s, final Metric metric) {
        // the key must include and not exclude to match
        if (includes != null && excludes != null) {
            return includes.matcher(s).matches() && !excludes.matcher(s).matches();
        }

        // the key must be in the inclusion list
        if (includes != null) {
            return includes.matcher(s).matches();
        }

        // the key must not be in the exclusion
        if (excludes != null) {
            return !excludes.matcher(s).matches();
        }

        // there were no inclusions/exclusions
        return true;
    }
}

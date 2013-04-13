package com.tinkerpop.rexster.server.metrics;

import junit.framework.Assert;
import org.junit.Test;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RegexMetricFilterTest {
    @Test
    public void matchesNoExclusions() {
        final RegexMetricFilter filter = new RegexMetricFilter(null, null);
        Assert.assertTrue(filter.matches("rexster.server.http", null));
        Assert.assertTrue(filter.matches("rexster.server.http.vertices", null));
        Assert.assertTrue(filter.matches("rexster.server.http.edges", null));
        Assert.assertTrue(filter.matches("rexster.server.rexpro.edges", null));
    }

    @Test
    public void matchesHasInclusions() {
        final RegexMetricFilter filter = new RegexMetricFilter("rexster.server.http.*", null);
        Assert.assertTrue(filter.matches("rexster.server.http", null));
        Assert.assertTrue(filter.matches("rexster.server.http.vertices", null));
        Assert.assertTrue(filter.matches("rexster.server.http.edges", null));
        Assert.assertFalse(filter.matches("rexster.server.rexpro.edges", null));
    }

    @Test
    public void matchesHasMultipleInclusions() {
        final RegexMetricFilter filter = new RegexMetricFilter("rexster.server.http.vertices|rexster.server.rexpro.*", null);
        Assert.assertFalse(filter.matches("rexster.server.http", null));
        Assert.assertTrue(filter.matches("rexster.server.http.vertices", null));
        Assert.assertFalse(filter.matches("rexster.server.http.edges", null));
        Assert.assertTrue(filter.matches("rexster.server.rexpro.edges", null));
    }

    @Test
    public void matchesHasExclusion() {
        final RegexMetricFilter filter = new RegexMetricFilter(null, "rexster.server.rexpro.*");
        Assert.assertTrue(filter.matches("rexster.server.http", null));
        Assert.assertTrue(filter.matches("rexster.server.http.vertices", null));
        Assert.assertTrue(filter.matches("rexster.server.http.edges", null));
        Assert.assertFalse(filter.matches("rexster.server.rexpro.edges", null));
    }

    @Test
    public void matchesHasMultipleExclusions() {
        final RegexMetricFilter filter = new RegexMetricFilter(null, "rexster.server.rexpro.edges|rexster.server.http.edges");
        Assert.assertTrue(filter.matches("rexster.server.http", null));
        Assert.assertTrue(filter.matches("rexster.server.http.vertices", null));
        Assert.assertFalse(filter.matches("rexster.server.http.edges", null));
        Assert.assertFalse(filter.matches("rexster.server.rexpro.edges", null));
    }

    @Test
    public void matchesHasInclusionAndExclusion() {
        final RegexMetricFilter filter = new RegexMetricFilter("rexster.server.http.vertices", "rexster.server.rexpro.*");
        Assert.assertFalse(filter.matches("rexster.server.http", null));
        Assert.assertTrue(filter.matches("rexster.server.http.vertices", null));
        Assert.assertFalse(filter.matches("rexster.server.http.edges", null));
        Assert.assertFalse(filter.matches("rexster.server.rexpro.edges", null));
    }

    @Test
    public void matchesHasConflictingInclusionAndExclusion() {
        final RegexMetricFilter filter = new RegexMetricFilter("rexster.server.http.*", "rexster.server.http.*");
        Assert.assertFalse(filter.matches("rexster.server.http", null));
        Assert.assertFalse(filter.matches("rexster.server.http.vertices", null));
        Assert.assertFalse(filter.matches("rexster.server.http.edges", null));
        Assert.assertFalse(filter.matches("rexster.server.rexpro.edges", null));
    }

}

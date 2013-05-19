package com.tinkerpop.rexster.server.metrics;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.Logger;

/**
 * Configures the HTTP servlet that can be polled for metrics.
 *
 * Rexster HTTP server must be enabled for this to work.  This config is unlike others because it merely acts as
 * a holder for configuration elements.  It does not actually start a reporter on its own.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class HttpReporterConfig extends AbstractReporterConfig {
    private static final Logger logger = Logger.getLogger(HttpReporterConfig.class);

    public HttpReporterConfig(final HierarchicalConfiguration config) {
        super(config, null);
        logger.info("Configured HTTP Metric Reporter.");
    }

    /**
     * Always returns true.  No reporters are started by calling this method.
     */
    @Override
    public boolean enable()
    {
        return true;
    }

    @Override
    public void disable() { }
}

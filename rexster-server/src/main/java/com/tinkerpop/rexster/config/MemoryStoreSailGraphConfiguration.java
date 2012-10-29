package com.tinkerpop.rexster.config;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class MemoryStoreSailGraphConfiguration extends AbstractSailGraphConfiguration {

    public MemoryStoreSailGraphConfiguration() {
        this.sailType = AbstractSailGraphConfiguration.SAIL_TYPE_MEMORY;
    }
}

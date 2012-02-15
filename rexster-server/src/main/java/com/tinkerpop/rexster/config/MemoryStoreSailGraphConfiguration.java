package com.tinkerpop.rexster.config;

public class MemoryStoreSailGraphConfiguration extends AbstractSailGraphConfiguration {

    public MemoryStoreSailGraphConfiguration() {
        this.sailType = AbstractSailGraphConfiguration.SAIL_TYPE_MEMORY;
    }
}

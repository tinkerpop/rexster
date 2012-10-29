package com.tinkerpop.rexster.config;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class NativeStoreSailGraphConfiguration extends AbstractSailGraphConfiguration {
    public NativeStoreSailGraphConfiguration() {
        this.sailType = AbstractSailGraphConfiguration.SAIL_TYPE_NATIVE;
    }
}

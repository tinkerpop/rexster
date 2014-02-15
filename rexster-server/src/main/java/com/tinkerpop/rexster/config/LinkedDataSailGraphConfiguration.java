package com.tinkerpop.rexster.config;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class LinkedDataSailGraphConfiguration extends AbstractSailGraphConfiguration {
    public LinkedDataSailGraphConfiguration() {
        this.sailType = AbstractSailGraphConfiguration.SAIL_TYPE_LINKED_DATA;
    }
}

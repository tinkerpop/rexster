package com.tinkerpop.rexster.config;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class LinkedDataSailGraphConfiguration extends AbstractSailGraphConfiguration {
    public LinkedDataSailGraphConfiguration() {
        this.sailType = AbstractSailGraphConfiguration.SAIL_TYPE_LINKED_DATA;
    }
}

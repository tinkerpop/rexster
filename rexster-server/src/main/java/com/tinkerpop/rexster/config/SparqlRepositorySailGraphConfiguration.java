package com.tinkerpop.rexster.config;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class SparqlRepositorySailGraphConfiguration extends AbstractSailGraphConfiguration {
    public SparqlRepositorySailGraphConfiguration() {
        this.sailType = AbstractSailGraphConfiguration.SAIL_TYPE_SPARQL;
    }
}

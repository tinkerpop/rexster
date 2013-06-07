package com.tinkerpop.rexster.server.metrics;

import com.codahale.metrics.MetricRegistry;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * A base configuration that accepts host and port combinations for configuration.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public abstract class AbstractHostPortReporterConfig extends AbstractReporterConfig {
    private static final Logger logger = Logger.getLogger(AbstractHostPortReporterConfig.class);

    private List<HostPort> hosts;
    protected String hostsString;

    public AbstractHostPortReporterConfig(final HierarchicalConfiguration config, final MetricRegistry metricRegistry) {
        super(config, metricRegistry);
    }

    public List<HostPort> getHosts(){
        return hosts;
    }

    public List<HostPort> parseHostString(){
        final List<HostPort> hosts = new ArrayList<HostPort>();
        final String[] hostPairs = this.hostsString.split(",");
        for (int i = 0; i < hostPairs.length; i++)
        {
            final String[] pair = hostPairs[i].split(":");
            hosts.add(new HostPort(pair[0], Integer.valueOf(pair[1])));
        }
        return hosts;
    }

    public List<HostPort> getHostListAndStringList(){
        // some simple log valadatin' sinc we can't || the @NotNulls
        // make mini protected functions sans logging for Ganglia
        if (getHosts() == null && this.hostsString == null)
        {
            logger.warn("No hosts specified as a list or delimited string");
            return null;
        }

        if (getHosts() != null && this.hostsString != null)
        {
            logger.warn("There are reporter hosts configured as a list and delimited string?");
        }

        final ArrayList<HostPort> combinedHosts = new ArrayList<HostPort>();
        if (getHosts() != null)
        {
            combinedHosts.addAll(getHosts());
        }
        if (this.hostsString != null)
        {
            combinedHosts.addAll(parseHostString());
        }
        return combinedHosts;

    }

    public abstract List<HostPort> getFullHostList();
}

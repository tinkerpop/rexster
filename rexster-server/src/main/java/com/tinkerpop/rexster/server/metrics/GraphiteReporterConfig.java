package com.tinkerpop.rexster.server.metrics;

import com.tinkerpop.rexster.Tokens;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.graphite.Graphite;
import com.yammer.metrics.graphite.GraphiteReporter;
import info.ganglia.gmetric4j.gmetric.GMetric;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class GraphiteReporterConfig extends AbstractHostPortReporterConfig {
    private static final Logger logger = Logger.getLogger(GraphiteReporterConfig.class);

    private String prefix = "";

    private final MetricRegistry metricRegistry;

    public GraphiteReporterConfig(final HierarchicalConfiguration config, final MetricRegistry metricRegistry) {
        final SubnodeConfiguration c = config.configurationAt(Tokens.REXSTER_GRAPH_PROPERTIES);

        this.metricRegistry = metricRegistry;

        this.setTimeunit(c.getString("report-time-unit", TimeUnit.SECONDS.toString()));
        this.setPeriod(c.getInt("report-period", 60));
        this.setConvertRateTo(c.getString("rates-time-unit", TimeUnit.SECONDS.toString()));
        this.setConvertDurationTo(c.getString("duration-time-unit", TimeUnit.SECONDS.toString()));
        this.setHostsString(c.getString("hosts", "localhost:2003"));
        this.prefix = c.getString("prefix", "");
    }

    public String getPrefix()
    {
        return prefix;
    }

    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    @Override
    public List<HostPort> getFullHostList()
    {
        return getHostListAndStringList();
    }

    @Override
    public boolean enable()
    {
        final List<HostPort> hosts = getFullHostList();
        if (hosts == null || hosts.isEmpty())
        {
            logger.error("No hosts specified, cannot enable GraphiteReporter");
            return false;
        }
        for (HostPort hostPort : hosts)
        {
            try
            {
                logger.info(String.format("Enabling GraphiteReporter to %s:%s", hostPort.getHost(), hostPort.getPort()));

                final Graphite graphite = new Graphite(new InetSocketAddress(hostPort.getHost(), hostPort.getPort()));
                GraphiteReporter.forRegistry(this.metricRegistry)
                        .convertDurationsTo(this.getRealConvertDurationTo())
                        .convertRatesTo(this.getRealConvertRateTo())
                        .prefixedWith(this.prefix)
                        .build(graphite).start(this.getPeriod(), this.getRealTimeunit());

            }
            catch (Exception e)
            {
                logger.error("Failed to enable GraphiteReporter", e);
                return false;
            }
        }
        return true;
    }
}

package com.tinkerpop.rexster.server.metrics;

import com.yammer.metrics.ConsoleReporter;
import com.yammer.metrics.JmxReporter;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.ganglia.GangliaReporter;
import com.yammer.metrics.graphite.GraphiteReporter;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class is responsible for configuring the metric reporting options in Rexster.  This class takes the contents
 * of the <i>metrics</i> section of rexster.xml to enable different reporting outs such as ganglia, jmx, graphite, etc.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ReporterConfig {
    private static final Logger logger = Logger.getLogger(ReporterConfig.class);

    private JmxReporter jmxReporter = null;

    private boolean httpReporterEnabled = false;

    private final MetricRegistry metricRegistry;

    private List<AbstractReporterConfig> reporters = new ArrayList<AbstractReporterConfig>();

    private ReporterConfig(final MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public static ReporterConfig load(final List<HierarchicalConfiguration> configurations, final MetricRegistry metricRegistry) {
        final ReporterConfig rc = new ReporterConfig(metricRegistry);

        final Iterator<HierarchicalConfiguration> it = configurations.iterator();
        while (it.hasNext()) {
            final HierarchicalConfiguration reporterConfig = it.next();
            final String reporterType = reporterConfig.getString("type");

            if (reporterType != null) {
                if (reporterType.equals("jmx") || reporterType.equals(JmxReporter.class.getCanonicalName())) {
                    // only initializes this once
                    if (rc.jmxReporter == null) {
                        rc.jmxReporter = JmxReporter.forRegistry(metricRegistry).build();
                    }
                } else if (reporterType.equals("http")) {
                    rc.httpReporterEnabled = true;
                } else if (reporterType.equals("console") || reporterType.equals(ConsoleReporter.class.getCanonicalName())) {
                    rc.reporters.add(new ConsoleReporterConfig(reporterConfig, metricRegistry));
                } else if (reporterType.equals("ganglia") || reporterType.equals(GangliaReporter.class.getCanonicalName())) {
                    rc.reporters.add(new GangliaReporterConfig(reporterConfig, metricRegistry));
                } else if (reporterType.equals("graphite") || reporterType.equals(GraphiteReporter.class.getCanonicalName())) {
                    rc.reporters.add(new GraphiteReporterConfig(reporterConfig, metricRegistry));
                } else {
                    logger.warn(String.format("The configured reporter [%s] is not valid", reporterType));
                }
            } else {
                logger.warn("A metric reporter was not configured properly.  Check rexster.xml as the 'type' attribute was not set.");
            }
        }

        return rc;
    }

    public boolean isHttpReporterEnabled() {
        return httpReporterEnabled;
    }

    public void enable() {
        if (jmxReporter != null) {
            jmxReporter.start();
        }

        for (AbstractReporterConfig reporter : reporters) {
            reporter.enable();
        }
    }
}

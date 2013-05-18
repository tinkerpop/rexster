package com.tinkerpop.rexster.server.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ganglia.GangliaReporter;
import com.codahale.metrics.graphite.GraphiteReporter;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible for configuring the metric reporting options in Rexster.  This class takes the contents
 * of the <i>metrics</i> section of rexster.xml to enable different reporting outs such as ganglia, jmx, graphite, etc.
 *
 * Typical usage involves calling the load method to construct an instance of the class and then calling enable on
 * that instance to start configured reporters.  The <i>http</i> reporter is a bit different only in the sense that
 * it does not initialize via a configuration class.  It is initialized through a servlet configured into the
 * HTTP server in Rexster.  If Rexster's HTTP is disabled then this reporter will not be accessible.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ReporterConfig {
    private static final Logger logger = Logger.getLogger(ReporterConfig.class);

    private JmxReporter jmxReporter = null;
    private HttpReporterConfig httpReporterConfig = null;

    private final MetricRegistry metricRegistry;

    private final List<AbstractReporterConfig> reporters = new ArrayList<AbstractReporterConfig>();

    private ReporterConfig(final MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    /**
     * Create a configuration class from items at the metrics.reporter element of rexster.xml.  It reads the
     * <i>type</i> element from each <i>reporter</i> element and constructs the appropriate config class for
     * that particular type.
     */
    public static ReporterConfig load(final List<HierarchicalConfiguration> configurations, final MetricRegistry metricRegistry) {
        final ReporterConfig rc = new ReporterConfig(metricRegistry);

        final Iterator<HierarchicalConfiguration> it = configurations.iterator();
        while (it.hasNext()) {
            final HierarchicalConfiguration reporterConfig = it.next();
            final String reporterType = reporterConfig.getString("type");

            if (reporterType != null) {
                if (reporterType.equals("jmx") || reporterType.equals(JmxReporter.class.getCanonicalName())) {
                    // only initializes this once...this shouldn't be generated multiple times.
                    if (rc.jmxReporter == null) {
                        rc.jmxReporter = JmxReporter.forRegistry(metricRegistry).build();
                    }
                } else if (reporterType.equals("http")) {
                    // only initializes this once...this shouldn't be generated multiple times.
                    if (rc.httpReporterConfig == null) {
                        rc.httpReporterConfig = new HttpReporterConfig(reporterConfig);
                    }
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
        return httpReporterConfig != null;
    }

    public String getRateTimeUnitConversion() {
        return isHttpReporterEnabled() ? httpReporterConfig.getRealRateTimeUnitConversion().toString() : TimeUnit.SECONDS.toString();
    }

    public String getDurationTimeUnitConversion() {
        return isHttpReporterEnabled() ? httpReporterConfig.getRealDurationTimeUnitConversion().toString() : TimeUnit.SECONDS.toString();
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

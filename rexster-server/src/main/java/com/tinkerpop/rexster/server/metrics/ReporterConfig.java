package com.tinkerpop.rexster.server.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ganglia.GangliaReporter;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.tinkerpop.rexster.server.RexsterProperties;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible for configuring the metric reporting options in Rexster.  This class takes the contents
 * of the <i>metrics</i> section of rexster.xml to enable different reporting outs such as ganglia, jmx, graphite, etc.
 *
 * The <i>http</i> reporter does not initialize by way of the enable() method.  It is initialized through a servlet
 * configured into the HTTP server in Rexster.  If Rexster's HTTP is disabled then this reporter will not be accessible.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ReporterConfig {
    private static final Logger logger = Logger.getLogger(ReporterConfig.class);

    private RexsterProperties properties;
    private JmxReporter jmxReporter = null;
    private HttpReporterConfig httpReporterConfig = null;

    private final MetricRegistry metricRegistry;

    private final List<AbstractReporterConfig> reporters = new ArrayList<AbstractReporterConfig>();

    /**
     * Create a configuration class from items at the metrics.reporter element of rexster.xml.  It reads the
     * <i>type</i> element from each <i>reporter</i> element and constructs the appropriate config class for
     * that particular type.
     */
    public ReporterConfig(final RexsterProperties properties, final MetricRegistry metricRegistry) {
        this.properties = properties;
        this.metricRegistry = metricRegistry;

        this.updateSettings();
        this.enable();

        properties.addListener(new RexsterProperties.RexsterPropertiesListener() {
            @Override
            public void propertiesChanged(final XMLConfiguration configuration) {
                updateSettings();
                enable();
            }
        });
    }

    public void updateSettings() {
        reset();

        final Iterator<HierarchicalConfiguration> it = properties.getReporterConfigurations().iterator();
        while (it.hasNext()) {
            final HierarchicalConfiguration reporterConfig = it.next();
            final String reporterType = reporterConfig.getString("type");

            if (reporterType != null) {
                if (reporterType.equals("jmx") || reporterType.equals(JmxReporter.class.getCanonicalName())) {
                    // only initializes this once...this shouldn't be generated multiple times.
                    if (jmxReporter == null) {
                        jmxReporter = JmxReporter.forRegistry(metricRegistry).build();
                    }
                } else if (reporterType.equals("http")) {
                    // only initializes this once...this shouldn't be generated multiple times.
                    if (httpReporterConfig == null) {
                        httpReporterConfig = new HttpReporterConfig(reporterConfig);
                    }
                } else if (reporterType.equals("console") || reporterType.equals(ConsoleReporter.class.getCanonicalName())) {
                    reporters.add(new ConsoleReporterConfig(reporterConfig, metricRegistry));
                } else if (reporterType.equals("ganglia") || reporterType.equals(GangliaReporter.class.getCanonicalName())) {
                    reporters.add(new GangliaReporterConfig(reporterConfig, metricRegistry));
                } else if (reporterType.equals("graphite") || reporterType.equals(GraphiteReporter.class.getCanonicalName())) {
                    reporters.add(new GraphiteReporterConfig(reporterConfig, metricRegistry));
                } else {
                    logger.warn(String.format("The configured reporter [%s] is not valid", reporterType));
                }
            } else {
                logger.warn("A metric reporter was not configured properly.  Check rexster.xml as the 'type' attribute was not set.");
            }
        }

        // push overrides into the properties so that they are accessible to the http server.  this helps unify
        // the definition of reporters in rexster.xml
        this.properties.addOverride("http-reporter-enabled", isHttpReporterEnabled());
        this.properties.addOverride("http-reporter-duration", getDurationTimeUnitConversion());
        this.properties.addOverride("http-reporter-convert", getRateTimeUnitConversion());
    }

    private void reset() {
        if (jmxReporter != null) {
            jmxReporter.stop();
            jmxReporter = null;
        }

        for (AbstractReporterConfig reporter : reporters) {
            reporter.disable();
        }

        reporters.clear();

        if (httpReporterConfig != null) {
            httpReporterConfig = null;
        }

        this.properties.removeOverride("http-reporter-enabled");
        this.properties.removeOverride("http-reporter-duration");
        this.properties.removeOverride("http-reporter-convert");
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

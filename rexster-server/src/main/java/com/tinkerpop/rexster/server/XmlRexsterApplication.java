package com.tinkerpop.rexster.server;

import com.tinkerpop.rexster.config.GraphConfigurationContainer;
import com.tinkerpop.rexster.config.GraphConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Configure multiple graphs in rexster via XML based Apache Configuration.  This is the standard way Rexster is
 * configured in standalone operations.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class XmlRexsterApplication extends AbstractMapRexsterApplication {
    private static final Logger logger = Logger.getLogger(XmlRexsterApplication.class);

    /**
     * Create new XmlRexsterApplication
     *
     * @param graphConfigs  graph configuration settings.
     */
    public XmlRexsterApplication(final List<HierarchicalConfiguration> graphConfigs) {
        try {
            final GraphConfigurationContainer container = new GraphConfigurationContainer(graphConfigs);
            this.graphs.putAll(container.getApplicationGraphs());
        } catch (GraphConfigurationException gce) {
            logger.error("Graph initialization failed. Check the graph configuration in rexster.xml.");
        }
    }
}

package com.tinkerpop.rexster.server;

import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.config.GraphConfigurationContainer;
import com.tinkerpop.rexster.config.GraphConfigurationException;
import com.tinkerpop.rexster.util.HierarchicalConfigurationComparator;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Configure multiple graphs in rexster via XML based Apache Configuration.  This is the standard way Rexster is
 * configured in standalone operations.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class XmlRexsterApplication extends AbstractMapRexsterApplication {
    private static final Logger logger = Logger.getLogger(XmlRexsterApplication.class);

    private static final HierarchicalConfigurationComparator configComparator = new HierarchicalConfigurationComparator();
    private List<HierarchicalConfiguration> previousConfigurations;

    /**
     * Create new XmlRexsterApplication
     *
     * @param graphConfigs  graph configuration settings.
     */
    public XmlRexsterApplication(final List<HierarchicalConfiguration> graphConfigs) {
        this.reconfigure(graphConfigs);
    }

    /**
     * Create new XmlRexsterApplication
     */
    public XmlRexsterApplication(final RexsterProperties properties) {
        properties.addListener(new RexsterProperties.RexsterPropertiesListener() {
            @Override
            public void propertiesChanged(XMLConfiguration configuration) {
                reconfigure(properties.getGraphConfigurations());
            }
        });
        this.reconfigure(properties.getGraphConfigurations());
    }

    synchronized void reconfigure(final List<HierarchicalConfiguration> graphConfigs) {
        try {
            final List<RexsterApplicationGraph> graphsToKill = new ArrayList<RexsterApplicationGraph>();
            final List<HierarchicalConfiguration> differentConfigs = new ArrayList<HierarchicalConfiguration>();

            // look for new or different configurations
            for (HierarchicalConfiguration gc : graphConfigs) {
                final HierarchicalConfiguration foundGc = find(gc);
                if (foundGc == null || !configComparator.compare(foundGc, gc)) {
                    differentConfigs.add(gc);

                    // this could be null if the graph was not found and is new to the config
                    final RexsterApplicationGraph ragToKill = this.graphs.get(gc.getString(Tokens.REXSTER_GRAPH_NAME));
                    if (ragToKill != null) {
                        graphsToKill.add(ragToKill);
                    }
                }
            }

            // remove any graphs that were killed out of the config
            for (Map.Entry<String, RexsterApplicationGraph> rag : this.graphs.entrySet()) {
                boolean found = false;
                for (HierarchicalConfiguration gc : graphConfigs) {
                    if (gc.getString(Tokens.REXSTER_GRAPH_NAME).equals(rag.getKey())) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    graphsToKill.add(rag.getValue());
                }
            }

            // shutdown the graphs that need to be killed and remove them
            for (RexsterApplicationGraph graphToKill : graphsToKill) {
                try {
                    // call shutdown on the unwrapped graph as some wrappers don't allow shutdown() to be called.
                    graphToKill.getUnwrappedGraph().shutdown();
                } catch (Exception ex) {
                    logger.error(String.format("Error while shutting down graph [%s] after finding it no longer configured.", graphToKill), ex);
                } finally {
                    graphs.remove(graphToKill.getGraphName());
                    graphToKill = null;
                    logger.info(String.format("Shutdown graph [%s].  It is no longer configured.", graphToKill));
                }
            }

            // build configurations for the new/different graph configurations only
            final GraphConfigurationContainer container = new GraphConfigurationContainer(differentConfigs);
            final Map<String, RexsterApplicationGraph> configuredGraphs = container.getApplicationGraphs();

            graphs.putAll(configuredGraphs);

            // the current configuration becomes the new "previous" configuration for future evaluations on
            // what things have changed in the config
            previousConfigurations = graphConfigs;

        } catch (GraphConfigurationException gce) {
            logger.error("Graph initialization failed. Check the graph configuration in rexster.xml.");
        }
    }

    private HierarchicalConfiguration find(final HierarchicalConfiguration hcToFind) {
        if (this.previousConfigurations == null) {
            return null;
        }

        for (HierarchicalConfiguration hc : this.previousConfigurations) {
            final String nameToFind = hcToFind.getString(Tokens.REXSTER_GRAPH_NAME);
            if (nameToFind.equals(hc.getString(Tokens.REXSTER_GRAPH_NAME))) {
                return hc;
            }
        }

        return null;
    }
}

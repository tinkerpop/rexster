package com.tinkerpop.rexster;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import java.util.Set;

/**
 * Providers the RexsterApplication through the instantiated WebServer and
 * its static methods.
 */
public class WebServerRexsterApplicationProvider implements RexsterApplicationProvider {

    protected static final Logger logger = Logger.getLogger(WebServerRexsterApplicationProvider.class);

    private static RexsterApplication rexster;

    public WebServerRexsterApplicationProvider(ServletContext servletContext) throws Exception {
        if (rexster == null) {
            String configurationPath = servletContext.getInitParameter("com.tinkerpop.rexster.config");

            if (configurationPath == null || configurationPath.trim().length() == 0) {
                configurationPath = "rexster.xml";
            }

            XMLConfiguration properties = new XMLConfiguration();
            properties.load(RexsterApplication.class.getResourceAsStream(configurationPath));
            rexster = new RexsterApplication(properties);
        }
    }

    public static RexsterApplication start(final XMLConfiguration properties) {
        rexster = new RexsterApplication(properties);
        return rexster;
    }

    public static void stop() {
        try {
            rexster.stop();
        } catch (Exception ex) {
            logger.warn("Rexster graph may not have been shutdown properly", ex);
        }
    }

    public RexsterApplication getRexsterApplication() {
        return rexster;
    }

    public RexsterApplicationGraph getApplicationGraph(String graphName) {
        return this.getRexsterApplication().getApplicationGraph(graphName);
    }

    public Set<String> getGraphsNames() {
        return this.getRexsterApplication().getGraphNames();
    }

    public long getStartTime() {
        return this.getRexsterApplication().getStartTime();
    }
}

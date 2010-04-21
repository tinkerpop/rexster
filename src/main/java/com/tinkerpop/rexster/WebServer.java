package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.restlet.Component;
import org.restlet.data.Protocol;

import java.io.IOException;
import java.util.Properties;

/**
 * @author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public class WebServer {

    private static final String DEFAULT_HOST = "";
    private static Properties rexsterProperties = new Properties();

    protected static Logger logger = Logger.getLogger(WebServer.class);

    static {
        PropertyConfigurator.configure(RexsterApplication.class.getResource("log4j.properties"));
        try {
        rexsterProperties.load(RexsterApplication.class.getResourceAsStream(RexsterTokens.REXSTER_PROPERTIES_FILE));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public WebServer(final Graph graph) throws Exception {
        logger.info(".:Welcome to Rexster:.");
        GraphHolder.putGraph(graph);
        logger.info("Graph " + graph + " loaded");
        this.runWebServer();
    }

    public WebServer() throws Exception {
        logger.info(".:Welcome to the Rexster:.");
        this.runWebServer();
    }

    protected void runWebServer() throws Exception {
        Component component = new Component();
        component.getServers().add(Protocol.HTTP, new Integer(rexsterProperties.getProperty("rexster.webserver.port")));
        component.getDefaultHost().attach(DEFAULT_HOST, new RexsterApplication());
        component.start();

        // user interaction to shutdown thread
        logger.info("Hit <enter> to shutdown Rexster");
        System.in.read();
        GraphHolder.getGraph().shutdown();
        logger.info("Shutting down Rexster");
        component.stop();
        System.exit(0);
    }

    public static void main(final String[] args) throws Exception {
        new WebServer();
    }
}
package com.tinkerpop.rexster;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class WebServer {

    protected static Logger logger = Logger.getLogger(WebServer.class);
    private static RexsterApplication rexster;

    static {
        PropertyConfigurator.configure(RexsterApplication.class.getResource("log4j.properties"));
    }

    protected SelectorThread threadSelector;

    public WebServer(final XMLConfiguration properties, boolean user) throws Exception {
        logger.info(".:Welcome to Rexster:.");
        if (user)
            this.startUser(properties);
        else
            this.start(properties);
    }

    protected void startUser(final XMLConfiguration properties) throws Exception {
        this.start(properties);
        // user interaction to shutdown server thread
        logger.info("Hit <enter> to shutdown Rexster");
        System.in.read();
        logger.info("Shutting down Rexster");
        this.stop();
        System.exit(0);
    }

    protected void start(final XMLConfiguration properties) throws Exception {
        rexster = new RexsterApplication(properties);
        Integer port = properties.getInteger("rexster.webserver-port", new Integer(8182));
        String baseUri = properties.getString("rexster.base-uri", "http://localhost");

        String baseUriWithPort = baseUri + ":" + port.toString() + "/";

        HierarchicalConfiguration webServerConfig = properties.configurationAt("web-server-configuration");

        final Map<String, String> initParams = new HashMap<String, String>();
        Iterator keys = webServerConfig.getKeys();
        while (keys.hasNext()) {
            String key = keys.next().toString();

            // commons config double dots keys with just one period in it.
            // as that represents a path statement for that lib.  need to remove
            // the double dot so that it can pass directly to grizzly.  hopefully
            // there are no cases where this will cause a problem and a double dot
            // is expected
            String grizzlyKey = key.replace("..", ".");
            String configValue = webServerConfig.getString(key);
            initParams.put(grizzlyKey, configValue);

            logger.info("Web Server configured with " + key + ": " + configValue);
        }

        this.threadSelector = GrizzlyWebContainerFactory.create(baseUriWithPort, initParams);

        logger.info("Server running on " + baseUri + ":" + port);

    }

    protected void stop() throws Exception {
        this.threadSelector.stopEndpoint();
        rexster.stop();
    }

    public static RexsterApplication GetRexsterApplication() {
        return rexster;
    }

    public static void main(final String[] args) throws Exception {
        XMLConfiguration properties = new XMLConfiguration();
        if (args.length == 1) {
            try {
                properties.load(new FileReader(args[0]));
            } catch (IOException e) {
                throw new Exception("Could not locate " + args[0] + " properties file.");
            }
        } else {
            properties.load(RexsterApplication.class.getResourceAsStream("rexster.xml"));
        }

        new WebServer(properties, true);
    }
}
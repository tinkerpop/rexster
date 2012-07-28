package com.tinkerpop.rexster.server;

import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.tinkerpop.rexster.filter.DefaultSecurityFilter;
import com.tinkerpop.rexster.filter.HeaderResponseFilter;
import com.tinkerpop.rexster.servlet.DogHouseServlet;
import com.tinkerpop.rexster.servlet.EvaluatorServlet;
import com.tinkerpop.rexster.servlet.RexsterStaticHttpHandler;
import com.tinkerpop.rexster.servlet.VisualizationServlet;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.servlet.ServletHandler;

import java.io.File;

/**
 * Initializes the HTTP server for Rexster serving REST and Dog House.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class HttpRexsterServer implements RexsterServer {
    private static final Logger logger = Logger.getLogger(HttpRexsterServer.class);

    private final XMLConfiguration properties;
    private final Integer rexsterServerPort;
    private final String rexsterServerHost;
    private final String webRootPath;
    private final String baseUri;
    private final HttpServer httpServer;

    public HttpRexsterServer(final XMLConfiguration properties) {
        this.properties = properties;
        rexsterServerPort = properties.getInteger("rexster-server-port", new Integer(RexsterSettings.DEFAULT_HTTP_PORT));
        rexsterServerHost = properties.getString("rexster-server-host", "0.0.0.0");
        webRootPath = properties.getString("web-root", RexsterSettings.DEFAULT_WEB_ROOT_PATH);
        baseUri = properties.getString("base-uri", RexsterSettings.DEFAULT_BASE_URI);
        this.httpServer = new HttpServer();
    }

    @Override
    public void stop() throws Exception {
        this.httpServer.stop();
    }

    @Override
    public void start() throws Exception {
        final ServletHandler jerseyHandler = new ServletHandler();
        jerseyHandler.addInitParameter("com.sun.jersey.config.property.packages", "com.tinkerpop.rexster");
        jerseyHandler.addInitParameter(ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS, HeaderResponseFilter.class.getName());
        jerseyHandler.addInitParameter("com.tinkerpop.rexster.config", properties.getString("self-xml"));

        final HierarchicalConfiguration securityConfiguration = properties.configurationAt("security.authentication");
        final String securityFilterType = securityConfiguration.getString("type");
        if (!securityFilterType.equals("none")) {
            if (securityFilterType.equals("default")) {
                jerseyHandler.addInitParameter(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, DefaultSecurityFilter.class.getName());
            } else {
                jerseyHandler.addInitParameter(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, securityFilterType);
            }
        }

        jerseyHandler.setContextPath("/");
        jerseyHandler.setServletInstance(new ServletContainer());

        final WebServerRexsterApplicationProvider provider = new WebServerRexsterApplicationProvider();
        final RexsterApplication application = provider.getValue();

        // servlet that services all url from "main" by simply sending
        // main.html back to the calling client.  main.html handles its own
        // state given the uri
        final ServletHandler dogHouseHandler = new ServletHandler();
        dogHouseHandler.setContextPath("/doghouse/main");
        dogHouseHandler.setServletInstance(new DogHouseServlet());

        // servlet for gremlin console
        final ServletHandler visualizationHandler = new ServletHandler();
        visualizationHandler.addInitParameter("com.tinkerpop.rexster.config", properties.getString("self-xml"));

        visualizationHandler.setContextPath("/doghouse/visualize");
        visualizationHandler.setServletInstance(new VisualizationServlet(application));

        // servlet for gremlin console
        final ServletHandler evaluatorHandler = new ServletHandler();
        evaluatorHandler.addInitParameter("com.tinkerpop.rexster.config", properties.getString("self-xml"));

        evaluatorHandler.setContextPath("/doghouse/exec");
        evaluatorHandler.setServletInstance(new EvaluatorServlet(application));

        final String absoluteWebRootPath = (new File(webRootPath)).getAbsolutePath();
        dogHouseHandler.addInitParameter("com.tinkerpop.rexster.config.rexsterApiBaseUri", baseUri + ":" + rexsterServerPort.toString());

        final ServerConfiguration config = this.httpServer.getServerConfiguration();
        config.addHttpHandler(jerseyHandler, "/");
        config.addHttpHandler(new RexsterStaticHttpHandler(absoluteWebRootPath), "/doghouse");
        config.addHttpHandler(dogHouseHandler, "/doghouse/main/*");
        config.addHttpHandler(visualizationHandler, "/doghouse/visualize");
        config.addHttpHandler(evaluatorHandler, "/doghouse/exec");

        final NetworkListener listener = new NetworkListener("grizzly", rexsterServerHost, rexsterServerPort);
        this.httpServer.addListener(listener);

        this.httpServer.start();

        logger.info("Rexster Server running on: [" + baseUri + ":" + rexsterServerPort + "]");
    }
}

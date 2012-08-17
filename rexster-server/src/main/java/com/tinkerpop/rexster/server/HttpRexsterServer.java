package com.tinkerpop.rexster.server;

import com.sun.jersey.api.core.ClassNamesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;
import com.tinkerpop.rexster.EdgeResource;
import com.tinkerpop.rexster.GraphResource;
import com.tinkerpop.rexster.IndexResource;
import com.tinkerpop.rexster.KeyIndexResource;
import com.tinkerpop.rexster.PrefixResource;
import com.tinkerpop.rexster.RexsterResource;
import com.tinkerpop.rexster.RootResource;
import com.tinkerpop.rexster.VertexResource;
import com.tinkerpop.rexster.filter.AbstractSecurityFilter;
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

import javax.ws.rs.core.Context;
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
    public void start(final RexsterApplication application) throws Exception {
        final ServletHandler jerseyHandler = new ServletHandler();
        jerseyHandler.addInitParameter("com.tinkerpop.rexster.config", properties.getString("self-xml"));

        // explicitly load resources so that the "RexsterApplicationProvider" class is not loaded
        final ResourceConfig rc = new ClassNamesResourceConfig(
                EdgeResource.class,
                GraphResource.class,
                IndexResource.class,
                KeyIndexResource.class,
                PrefixResource.class,
                RexsterResource.class,
                RootResource.class,
                VertexResource.class);

        // constructs an injectable for the RexsterApplication instance.  This get constructed externally
        // and is passed into the HttpRexsterServer.  The SingletonTypeInjectableProvider is responsible for
        // pushing that instance into the context.
        rc.getSingletons().add(new SingletonTypeInjectableProvider<Context, RexsterApplication>(
                RexsterApplication.class, application){});

        final String defaultCharacterEncoding = properties.getString("character-set", "ISO-8859-1");
        rc.getContainerResponseFilters().add(new HeaderResponseFilter(defaultCharacterEncoding));

        final HierarchicalConfiguration securityConfiguration = properties.configurationAt("security.authentication");
        final String securityFilterType = securityConfiguration.getString("type");
        if (!securityFilterType.equals("none")) {
            if (securityFilterType.equals("default")) {
                jerseyHandler.addInitParameter(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, DefaultSecurityFilter.class.getName());
                rc.getContainerRequestFilters().add(new DefaultSecurityFilter());
            } else {
                jerseyHandler.addInitParameter(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, securityFilterType);
                final Class clazz = Class.forName(securityFilterType, true, Thread.currentThread().getContextClassLoader());
                final AbstractSecurityFilter securityFilter = (AbstractSecurityFilter) clazz.newInstance();
                rc.getContainerRequestFilters().add(securityFilter);
            }
        }

        jerseyHandler.setContextPath("/");
        jerseyHandler.setServletInstance(new ServletContainer(rc));

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

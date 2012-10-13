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
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.IOStrategy;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

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
    private final int maxWorkerThreadPoolSize;
    private final int coreWorkerThreadPoolSize;
    private final int maxKernalThreadPoolSize;
    private final int coreKernalThreadPoolSize;
    private final boolean enableJmx;
    private final String ioStrategy;
    private final HttpServer httpServer;

    public HttpRexsterServer(final XMLConfiguration properties) {
        this.properties = properties;
        rexsterServerPort = properties.getInteger("http.server-port", new Integer(RexsterSettings.DEFAULT_HTTP_PORT));
        rexsterServerHost = properties.getString("http.server-host", "0.0.0.0");
        webRootPath = properties.getString("http.web-root", RexsterSettings.DEFAULT_WEB_ROOT_PATH);
        baseUri = properties.getString("http.base-uri", RexsterSettings.DEFAULT_BASE_URI);
        coreWorkerThreadPoolSize = properties.getInt("http.thread-pool.worker.core-size", 8);
        maxWorkerThreadPoolSize = properties.getInt("http.thread-pool.worker.max-size", 8);
        coreKernalThreadPoolSize = properties.getInt("http.thread-pool.kernal.core-size", 4);
        maxKernalThreadPoolSize = properties.getInt("http.thread-pool.kernal.max-size", 4);
        enableJmx = properties.getBoolean("http.enable-jmx", false);
        this.ioStrategy = properties.getString("http.io-strategy", "worker");

        this.httpServer = new HttpServer();
    }

    @Override
    public void stop() throws Exception {
        this.httpServer.stop();
    }

    @Override
    public void start(final RexsterApplication application) throws Exception {

        deployRestApi(application);
        deployDogHouse(application);

        final NetworkListener listener = configureNetworkListener();
        final IOStrategy strategy = GrizzlyIoStrategyFactory.createIoStrategy(this.ioStrategy);

        logger.info(String.format("Using %s IOStrategy for HTTP/REST.", strategy.getClass().getName()));

        configureNetworkListener().getTransport().setIOStrategy(strategy);
        this.httpServer.addListener(listener);
        this.httpServer.getServerConfiguration().setJmxEnabled(enableJmx);
        this.httpServer.start();

        logger.info("Rexster Server running on: [" + baseUri + ":" + rexsterServerPort + "]");
    }

    private void deployRestApi(final RexsterApplication application) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        final String absoluteWebRootPath = (new File(webRootPath)).getAbsolutePath();
        final ServerConfiguration config = this.httpServer.getServerConfiguration();
        config.addHttpHandler(new RexsterStaticHttpHandler(absoluteWebRootPath), "/static");

        final WebappContext wacJersey = new WebappContext("jersey", "");

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

        final String defaultCharacterEncoding = properties.getString("http.character-set", "ISO-8859-1");
        rc.getContainerResponseFilters().add(new HeaderResponseFilter(defaultCharacterEncoding));

        final HierarchicalConfiguration securityConfiguration = properties.configurationAt("security.authentication");
        final String securityFilterType = securityConfiguration.getString("type");
        if (!securityFilterType.equals("none")) {
            if (securityFilterType.equals("default")) {
                wacJersey.addContextInitParameter(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, DefaultSecurityFilter.class.getName());
                rc.getContainerRequestFilters().add(new DefaultSecurityFilter());
            } else {
                wacJersey.addContextInitParameter(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, securityFilterType);
                final Class clazz = Class.forName(securityFilterType, true, Thread.currentThread().getContextClassLoader());
                final AbstractSecurityFilter securityFilter = (AbstractSecurityFilter) clazz.newInstance();
                rc.getContainerRequestFilters().add(securityFilter);
            }
        }

        final ServletRegistration sg = wacJersey.addServlet("jersey", new ServletContainer(rc));
        sg.addMapping("/*");
        wacJersey.deploy(this.httpServer);
    }

    private void deployDogHouse(RexsterApplication application) {
        // servlet that services all url from "main" by simply sending
        // main.html back to the calling client.  main.html handles its own
        // state given the uri
        final WebappContext wacDogHouse = new WebappContext("doghouse", "");
        final ServletRegistration sgDogHouse = wacDogHouse.addServlet("doghouse", new DogHouseServlet());
        sgDogHouse.addMapping("/doghouse/*");
        sgDogHouse.setInitParameter("com.tinkerpop.rexster.config.rexsterApiBaseUri", baseUri + ":" + rexsterServerPort.toString());

        final ServletRegistration sgDogHouseEval = wacDogHouse.addServlet("doghouse-evaluator", new EvaluatorServlet(application));
        sgDogHouseEval.addMapping("/doghouse/exec");

        wacDogHouse.deploy(this.httpServer);
    }

    private NetworkListener configureNetworkListener() {
        final NetworkListener listener = new NetworkListener("grizzly", rexsterServerHost, rexsterServerPort);
        final ThreadPoolConfig workerThreadPoolConfig = ThreadPoolConfig.defaultConfig()
                .setCorePoolSize(coreWorkerThreadPoolSize)
                .setMaxPoolSize(maxWorkerThreadPoolSize);
        listener.getTransport().setWorkerThreadPoolConfig(workerThreadPoolConfig);
        final ThreadPoolConfig kernalThreadPoolConfig = ThreadPoolConfig.defaultConfig()
                .setCorePoolSize(coreKernalThreadPoolSize)
                .setMaxPoolSize(maxKernalThreadPoolSize);
        listener.getTransport().setKernelThreadPoolConfig(kernalThreadPoolConfig);
        return listener;
    }
}

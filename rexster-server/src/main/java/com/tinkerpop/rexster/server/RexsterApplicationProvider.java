package com.tinkerpop.rexster.server;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Type;
import java.util.List;

/**
 * A Jersey InjectableProvider and Injectable that supplies Servlets that have a @Context
 * annotated RexsterApplication field with a DefaultRexsterApplication.
 * <p/>
 * Those interested in embedding Rexster into their custom application should write a Provider
 * class following this pattern that supplies their custom implementation of RexsterApplication.
 * <p/>
 * This class allows deployment of Rexster inside of a servlet container like Tomcat.  If
 * it is deployed in this fashion, rexster.xml need only consist of the graph definitions as
 * follows:
 *
 * <pre>
 * {@code
 * <rexster>
 *     <graphs>
 *         <graph>
 *             ...
 *         </graph>
 *         ...
 *     </graphs>
 * </rexster>
 * }
 * </pre>
 *
 * The other settings will be ignore and controlled by the servlet container.  While TinkerPop does not
 * officially support this kind of deployment, it can be done by building Rexster to a WAR with
 * the appropriate web.xml file which should look something like:
 *
 * <pre>
 * {@code
 * <?xml version="1.0" encoding="utf-8"?>
 * <!DOCTYPE web-app PUBLIC "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN" "http://java.sun.com/dtd/web-app_2_3.dtd">
 * <web-app xmlns="http://java.sun.com/xml/ns/javaee" version="2.5">
 *   <display-name>Rexster: A RESTful Graph Shell</display-name>
 *   <servlet>
 *     <servlet-name>Jersey Web Application</servlet-name>
 *     <servlet-class>com.sun.jersey.spi.container.servlet.ServletContainer</servlet-class>
 *   <init-param>
 *     <param-name>com.sun.jersey.config.property.resourceConfigClass</param-name>
 *     <param-value>com.sun.jersey.api.core.PackagesResourceConfig</param-value>
 *   </init-param>
 *   <init-param>
 *     <param-name>com.sun.jersey.config.property.packages</param-name>
 *     <param-value>com.tinkerpop.rexster;com.tinkerpop.rexster.server</param-value>
 *   </init-param>
 *   <init-param>
 *     <param-name>com.tinkerpop.rexster.config</param-name>
 *     <param-value>rexster.xml</param-value>
 *   </init-param>
 *   </servlet>
 * </web-app>
 * }
 * </pre>
 *
 * @author Jordan A. Lewis (http://jordanlewis.org)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@Provider
public class RexsterApplicationProvider extends AbstractHttpContextInjectable<RexsterApplication>
        implements InjectableProvider<Context, Type> {

    private static RexsterApplication rexster;

    private static XMLConfiguration configurationProperties;

    public RexsterApplicationProvider(@Context ServletContext servletContext, @Context ServletConfig servletConfig) {

        if (rexster == null) {
            if (configurationProperties == null) {
                configurationProperties = new XMLConfiguration();
            }

            final String rexsterXmlFile = servletConfig.getInitParameter("com.tinkerpop.rexster.config");

            try {
                configurationProperties.load(servletContext.getResourceAsStream(rexsterXmlFile));
            } catch (ConfigurationException e) {
                throw new RuntimeException(String.format(
                        "Could not load %s properties file. Message: %s", rexsterXmlFile, e.getMessage()), e);
            }

            final List<HierarchicalConfiguration> graphConfigs = configurationProperties.configurationsAt(Tokens.REXSTER_GRAPH_PATH);
            rexster = new XmlRexsterApplication(graphConfigs);

        }
    }

    @Override
    public RexsterApplication getValue(HttpContext c) {
        return rexster;
    }

    @Override
    public RexsterApplication getValue() {
        return rexster;
    }

    @Override
    public ComponentScope getScope() {
        return ComponentScope.Singleton;
    }

    @Override
    public Injectable getInjectable(ComponentContext ic, Context context, Type type) {
        if (type.equals(RexsterApplication.class)) {
            return this;
        }
        return null;
    }
}

package com.tinkerpop.rexster;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;
import org.apache.commons.configuration.XMLConfiguration;

import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Type;

/**
 * A Jersey InjectableProvider and Injectable that supplies Servlets which have a @Context
 * annotated RexsterApplication field with a RexsterApplicationImpl.
 * <p/>
 * Users interested in embedding Rexster into their custom application should write a Provider
 * class following this pattern that supplies their custom implementation of RexsterApplication.
 *
 * @author Jordan A. Lewis (http://jordanlewis.org)
 */
@Provider
public class WebServerRexsterApplicationProvider
        extends AbstractHttpContextInjectable<RexsterApplication>
        implements InjectableProvider<Context, Type> {

    private static RexsterApplication rexster;

    private static XMLConfiguration configurationProperties;

    public static void start(XMLConfiguration properties) {
        configurationProperties = properties;
    }

    public static void stop() {
        rexster.stop();
    }

    public WebServerRexsterApplicationProvider() {
        if (rexster == null) {
            if (configurationProperties == null) {
                configurationProperties = new XMLConfiguration();
            }
            rexster = new RexsterApplicationImpl(configurationProperties);
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

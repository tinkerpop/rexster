/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tinkerpop.rexster.filter;

import com.sun.jersey.core.util.Base64;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.tinkerpop.rexster.RexsterApplication;
import com.tinkerpop.rexster.protocol.message.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.message.MessageType;
import com.tinkerpop.rexster.protocol.message.RexProMessage;
import com.tinkerpop.rexster.protocol.message.SessionRequestMessage;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import java.io.FileReader;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

/**
 * Provides authentication for Rexster for all services: RexPro, REST, and Dog House.
 *
 * This is a bit of sketchy implementation of two semi-related bits of Grizzly/Jersey.  Trying to unify the
 * implementation of security within the system for RexPro/REST/Dog House.  Could be a better way to do this,
 * but it's not clear just yet.
 */
public abstract class AbstractSecurityFilter extends BaseFilter implements ContainerRequestFilter {

    private static Logger logger = Logger.getLogger(AbstractSecurityFilter.class);

    @Context
    protected UriInfo uriInfo;

    @Context
    protected ServletConfig servletConfig;

    @Context
    protected HttpServletRequest httpServletRequest;

    private boolean isConfigured = false;

    public AbstractSecurityFilter() {
    }


    public AbstractSecurityFilter(XMLConfiguration configuration) {
        configure(configuration);
        isConfigured = true;
    }

    /**
     * Authenticate the user in whatever way the implementation requires.
     */
    public abstract boolean authenticate(final String user, final String password);

    /**
     * Configure the filter.
     *
     * This method will be called multiple times so look to cache the configuration once after the
     * first call.
     */
    public abstract void configure(XMLConfiguration configuration);

    /**
     * The name of the security filter.
     */
    public abstract String getName();

    /**
     * RexPro authentication
     */
    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        final RexProMessage message = ctx.getMessage();

        if (message.getType() == MessageType.SESSION_REQUEST && !message.hasSession()) {
            SessionRequestMessage specificMessage = new SessionRequestMessage(message);

            if (specificMessage.getFlag() == SessionRequestMessage.FLAG_NEW_SESSION) {
                final String[] usernamePassword = specificMessage.getUsernamePassword();
                final String username = usernamePassword[0];
                final String password = usernamePassword[1];
                if (!authenticate(username, password)) {
                    // there is no session to this message...that's a problem
                    ctx.write(new ErrorResponseMessage(RexProMessage.EMPTY_SESSION, message.getRequestAsUUID(),
                            ErrorResponseMessage.FLAG_ERROR_AUTHENTICATION_FAILURE,
                            "Invalid username or password."));

                    return ctx.getStopAction();
                }
            }
        }

        return ctx.getInvokeAction();
    }

    /**
     * REST/Dog House based authentication.
     */
    public ContainerRequest filter(ContainerRequest request) {
        User user = authenticateServletRequest(request);
        request.setSecurityContext(new Authorizer(user));
        return request;
    }


    private void initFromServletConfiguration() {
        // have to do this here because the @Context is not initialized in the constructor
        if (isConfigured) {
            final String rexsterXmlFile = servletConfig.getInitParameter("com.tinkerpop.rexster.config");
            final XMLConfiguration properties = new XMLConfiguration();

            try {
                properties.load(new FileReader(rexsterXmlFile));
            } catch (Exception e) {
                throw new RuntimeException("Could not locate " + rexsterXmlFile + " properties file.", e);
            }

            configure(properties);
            isConfigured = true;
        }
    }

    private User authenticateServletRequest(ContainerRequest request) {

        // not sure that this will ever get called, but it's worth a check
        this.initFromServletConfiguration();

        // get the authorization header value
        String authentication = request.getHeaderValue(ContainerRequest.AUTHORIZATION);
        if (authentication == null) {
            throw new WebApplicationException(generateErrorResponse("Authentication credentials are required."));
        }

        if (!authentication.startsWith("Basic ")) {
            logger.info("Authentication failed: request for unsupported authentication type [" + authentication + "]");
            throw new WebApplicationException(generateErrorResponse("Invalid authentication credentials."));
        }

        authentication = authentication.substring("Basic ".length());
        final String[] values = new String(Base64.base64Decode(authentication)).split(":");
        if (values.length < 2) {
            logger.info("Authentication failed: invalid authentication string format [" + authentication + "]");
            throw new WebApplicationException(generateErrorResponse("Invalid authentication credentials."));
        }

        final String username = values[0];
        final String password = values[1];
        if ((username == null) || (password == null)) {
            logger.info("Authentication failed: missing username or password [" + authentication + "]");
            throw new WebApplicationException(generateErrorResponse("Invalid authentication credentials."));
        }

        final User user;
        if (authenticate(username, password)) {
            user = new User(username, "user");
            logger.debug("Authentication succeeded for [" + username + "]");
        } else {
            logger.info("Authentication failed: invalid username or password [" + authentication + "]");
            throw new WebApplicationException(generateErrorResponse("Invalid username or password."));
        }

        return user;
    }

    private Response generateErrorResponse(final String message) {
        final Map<String, String> errorEntity = new HashMap<String, String>() {{
            put("message", message);
            put("version", RexsterApplication.getVersion());
        }};

        return Response.status(Response.Status.UNAUTHORIZED)
                                       .header("WWW-Authenticate", "Basic realm=\"rexster\"")
                                       .type("application/json")
                                       .entity(new JSONObject(errorEntity)).build();
    }

    public class Authorizer implements SecurityContext {

        private User user;
        private Principal principal;

        public Authorizer(final User user) {
            this.user = user;
            this.principal = new Principal() {

                public String getName() {
                    return user.username;
                }
            };
        }

        public Principal getUserPrincipal() {
            return this.principal;
        }

        public boolean isUserInRole(String role) {
            return (role.equals(user.role));
        }

        public boolean isSecure() {
            return "https".equals(uriInfo.getRequestUri().getScheme());
        }

        public String getAuthenticationScheme() {
            return SecurityContext.BASIC_AUTH;
        }
    }

    public class User {

        public String username;
        public String role;

        public User(String username, String role) {
            this.username = username;
            this.role = role;
        }
    }
}

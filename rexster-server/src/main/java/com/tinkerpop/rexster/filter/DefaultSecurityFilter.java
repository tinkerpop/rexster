package com.tinkerpop.rexster.filter;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides authentication for Rexster for all services: RexPro, REST, and Dog House.
 * <p/>
 * Utilizes a simple list of usernames and passwords in rexster.xml.  Example:
 * <p/>
 * <security>
 * <authentication>
 * <type>default</type>
 * <configuration>
 * <users>
 * <user>
 * <username>rexster</username>
 * <password>rexster</password>
 * </user>
 * </users>
 * </configuration>
 * </authentication>
 * </security>
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class DefaultSecurityFilter extends AbstractSecurityFilter {

    /**
     * List of usernames and passwords that have access to Rexster.
     */
    private static Map<String, String> users = null;

    /**
     * Checks the users map to determine if the username and password supplied is one of the ones available..
     */
    public boolean authenticate(final String username, final String password) {
        return users.containsKey(username) && users.get(username).equals(password);
    }

    /**
     * Reads the configuration from rexster.xml and converts it to a map of usernames and passwords.
     */
    public void configure(final XMLConfiguration configuration) {
        if (users == null) {
            users = new HashMap<String, String>();

            try {
                final HierarchicalConfiguration authenticationConfiguration = configuration.configurationAt("security.authentication.configuration.users");
                final List<HierarchicalConfiguration> userListFromConfiguration = authenticationConfiguration.configurationsAt("user");

                for (HierarchicalConfiguration userFromConfiguration : userListFromConfiguration) {
                    users.put(userFromConfiguration.getString("username"), userFromConfiguration.getString("password"));
                }
            } catch (Exception e) {
                users = null;
                throw new RuntimeException("Invalid configuration of users in configuration file.", e);
            }
        }
    }

    public String getName() {
        return "DefaultSecurity";
    }
}

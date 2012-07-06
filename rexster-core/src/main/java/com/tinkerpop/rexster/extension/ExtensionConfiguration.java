package com.tinkerpop.rexster.extension;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Holds information that details the configuration of an extension.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ExtensionConfiguration {
    private static final Logger logger = Logger.getLogger(ExtensionConfiguration.class);

    private final String namespace;

    private final String extensionName;

    private final HierarchicalConfiguration configuration;

    /**
     * Initializes a new ExtensionConfiguration object as taken from rexster.xml.
     * This is the specific configuration for a particular extension in a specific graph.
     */
    public ExtensionConfiguration(final String namespace, final String extensionName, final HierarchicalConfiguration extensionConfiguration) {

        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("Namespace cannot be null or empty.");
        }

        if (extensionName == null || extensionName.isEmpty()) {
            throw new IllegalArgumentException("Extension Name cannot be null or empty.");
        }

        if (extensionConfiguration == null) {
            throw new IllegalArgumentException("Extension Configuration cannot be null.");
        }

        this.namespace = namespace;
        this.extensionName = extensionName;
        this.configuration = extensionConfiguration;
    }

    public String getNamespace() {
        return this.namespace;
    }


    public String getExtensionName() {
        return this.extensionName;
    }

    public HierarchicalConfiguration getConfiguration() {
        return this.configuration;
    }

    /**
     * Helper method that tries to read the configuration for the extension into a map.  The configuration
     * section must appear as follows for this method to work:
     * <p/>
     * <configuration>
     * <key1>value</key1>
     * <key2>value</key2>
     * </configuration>
     * <p/>
     * Key values must be unique within the configuration.
     *
     * @return A map or null if the parse does not work.
     */
    public Map<String, String> tryGetMapFromConfiguration() {

        Map<String, String> map = new HashMap<String, String>();
        try {
            final Iterator keys = this.configuration.getKeys();
            while (keys.hasNext()) {
                final String key = keys.next().toString();
                map.put(key, this.configuration.getString(key));
            }
        } catch (Exception ex) {
            logger.error("There is an error in the configuration of this extension [" + this.namespace + ":" + this.extensionName + "].  All values must be of a String data type.");

            // ignore and return null
            map = null;
        }

        return map;
    }
}
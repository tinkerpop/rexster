package com.tinkerpop.rexster.extension;

/**
 * Marks a class as one that can be used as an extension to Rexster.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public interface RexsterExtension {

    /**
     * Determines if the configuration for the extension is valid.
     *
     * @return True if the configuration is valid and false otherwise.
     */
    public boolean isConfigurationValid(final ExtensionConfiguration extensionConfiguration);
}

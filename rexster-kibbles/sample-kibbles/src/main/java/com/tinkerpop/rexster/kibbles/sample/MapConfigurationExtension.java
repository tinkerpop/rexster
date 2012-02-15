package com.tinkerpop.rexster.kibbles.sample;

import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.extension.ExtensionConfiguration;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.RexsterContext;

import java.util.Map;

/**
 * Demonstrates how to do a simple Map based configuration of an extension.
 * <p/>
 * This extension expects a configuration that contains a simple set of name
 * value pairs:
 * <p/>
 * <configuration>
 * <some-key>some-value</some-key>
 * <other-key>other-value</other-key>
 * </configuration>
 */
@ExtensionNaming(name = MapConfigurationExtension.EXTENSION_NAME, namespace = AbstractSampleExtension.EXTENSION_NAMESPACE)
public class MapConfigurationExtension extends AbstractSampleExtension {
    public static final String EXTENSION_NAME = "map-config";

    private static final String CONFIG_SOME_KEY = "some-key";
    private static final String CONFIG_OTHER_KEY = "other-key";

    /**
     * Reads the values from the configuration and returns them as JSON.
     */
    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH)
    @ExtensionDescriptor(description = "returns the configuration as JSON.")
    public ExtensionResponse doConfiguredWork(@RexsterContext RexsterApplicationGraph rag) {

        // finds the configuration settings from the configured graph
        ExtensionConfiguration configuration = rag.findExtensionConfiguration(EXTENSION_NAMESPACE, EXTENSION_NAME);
        Map<String, String> map = configuration.tryGetMapFromConfiguration();

        return ExtensionResponse.ok(map);
    }

    /**
     * Ensures that the configuration is valid.
     * <p/>
     * A valid configuration is one that has a map like structure and contains two keys.
     */
    @Override
    public boolean isConfigurationValid(ExtensionConfiguration extensionConfiguration) {
        boolean valid = false;

        if (extensionConfiguration != null) {
            Map<String, String> map = extensionConfiguration.tryGetMapFromConfiguration();
            valid = map != null && !map.isEmpty()
                    && map.containsKey(CONFIG_SOME_KEY) && map.containsKey(CONFIG_OTHER_KEY);
        }

        return valid;
    }
}

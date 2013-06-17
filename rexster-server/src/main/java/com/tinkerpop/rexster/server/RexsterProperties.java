package com.tinkerpop.rexster.server;

import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.protocol.EngineController;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RexsterProperties are settings that come from the rexster.xml configuration file appended with other in server
 * settings that come from the command line.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexsterProperties extends FileAlterationListenerAdaptor {
    private static final Logger logger = Logger.getLogger(RexsterProperties.class);

    private XMLConfiguration configuration;
    private final List<RexsterPropertiesListener> listeners = new ArrayList<RexsterPropertiesListener>();
    private final Map<String, RexsterPropertyOverride> overrides = new HashMap<String,RexsterPropertyOverride>();

    public RexsterProperties(final XMLConfiguration configuration) {
        this.configuration = configuration;
    }

    public RexsterProperties(final String rexsterXmlFileLocation) {
        readConfigurationFromFile(rexsterXmlFileLocation);
    }

    private void readConfigurationFromFile(String rexsterXmlFileLocation) {
        this.configuration = new XMLConfiguration();
        final File rexsterXmlFile = new File(rexsterXmlFileLocation);

        try {
            // load either the rexster.xml from the command line or the default rexster.xml in the root of the
            // working directory
            configuration.load(new FileReader(rexsterXmlFileLocation));
            logger.info(String.format("Using [%s] as configuration source.", rexsterXmlFile.getAbsolutePath()));
        } catch (Exception e) {
            final String msg = String.format("Could not load configuration from [%s]", rexsterXmlFile.getAbsolutePath());
            logger.warn(msg);
            throw new RuntimeException(msg);
        }
    }

    public XMLConfiguration getConfiguration() {
        return this.configuration;
    }

    public List<HierarchicalConfiguration> getGraphConfigurations() {
        return configuration.configurationsAt(Tokens.REXSTER_GRAPH_PATH);
    }

    public List<HierarchicalConfiguration> getReporterConfigurations() {
        return configuration.configurationsAt(Tokens.REXSTER_REPORTER_PATH);
    }

    public Long getRexProSessionMaxIdle() {
        return configuration.getLong("rexpro.session-max-idle",
                new Long(RexsterSettings.DEFAULT_REXPRO_SESSION_MAX_IDLE));
    }

    public Long getRexProSessionCheckInterval() {
        return configuration.getLong("rexpro.session-check-interval",
                new Long(RexsterSettings.DEFAULT_REXPRO_SESSION_CHECK_INTERVAL));
    }

    public Integer getRexsterShutdownPort() {
        return configuration.getInteger("shutdown-port", new Integer(RexsterSettings.DEFAULT_SHUTDOWN_PORT));
    }

    public String getRexsterShutdownHost() {
        return configuration.getString("shutdown-host", RexsterSettings.DEFAULT_HOST);
    }

    public List<HierarchicalConfiguration> getScriptEngines() {
        return this.configuration.configurationsAt(Tokens.REXSTER_SCRIPT_ENGINE_PATH);
    }

    public HierarchicalConfiguration getSecuritySettings() {
        try {
            return configuration.configurationAt(Tokens.REXSTER_SECURITY_AUTH);
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    public void addListener(final RexsterPropertiesListener listener) {
        this.listeners.add(listener);
    }

    public void addOverride(final String overrideKey, final Object overrideValue) {
        this.configuration.setProperty(overrideKey, overrideValue);
        this.overrides.put(overrideKey, new RexsterPropertyOverride(overrideKey, overrideValue));
    }

    public void removeOverride(final String overrideKey) {
        this.overrides.remove(overrideKey);
    }

    @Override
    public void onFileChange(final File file) {
        logger.info(String.format("File settings have changed.  Rexster is reloading [%s]", file.getAbsolutePath()));
        readConfigurationFromFile(file.getAbsolutePath());

        for (RexsterPropertyOverride override : overrides.values()) {
            configuration.setProperty(override.getKeyToOverride(), override.getOverrideValue());
        }

        for (RexsterPropertiesListener listener : listeners) {
            listener.propertiesChanged(configuration);
        }
    }

    public interface RexsterPropertiesListener {
        void propertiesChanged(final XMLConfiguration configuration);
    }

    class RexsterPropertyOverride {
        private final String keyToOverride;
        private final Object overrideValue;

        public RexsterPropertyOverride(final String keyToOverride, final Object overrideValue) {
            this.keyToOverride = keyToOverride;
            this.overrideValue = overrideValue;
        }

        public String getKeyToOverride() {
            return keyToOverride;
        }

        public Object getOverrideValue() {
            return overrideValue;
        }
    }
}

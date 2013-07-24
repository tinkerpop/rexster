package com.tinkerpop.rexster.protocol;

import org.apache.commons.configuration.HierarchicalConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration class for a ScriptEngine implementation.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class EngineConfiguration {

    private final int resetCount;
    private final String initScriptFile;
    private final String scriptEngineName;
    private final Set<String> imports;
    private final Set<String> staticImports;

    public EngineConfiguration(final HierarchicalConfiguration configuration) {
        this.scriptEngineName = configuration.getString("name", "gremlin-groovy");
        this.initScriptFile = configuration.getString("init-script", null);
        this.resetCount = configuration.getInt("reset-threshold", EngineController.RESET_NEVER);
        this.imports = new HashSet<String>(configuration.getList("imports", new ArrayList()));
        this.staticImports = new HashSet<String>(configuration.getList("static-imports", new ArrayList()));
    }

    public EngineConfiguration(final String scriptEngineName, final int resetCount, final String initScriptFile,
                               final Set<String> imports, final Set<String> staticImports) {
        this.resetCount = resetCount;
        this.initScriptFile = initScriptFile;
        this.scriptEngineName = scriptEngineName;
        this.imports = imports;
        this.staticImports = staticImports;
    }

    public int getResetCount() {
        return resetCount;
    }

    public String getInitScriptFile() {
        return initScriptFile;
    }

    public String getScriptEngineName() {
        return scriptEngineName;
    }

    public Set<String> getImports() {
        return imports;
    }

    public Set<String> getStaticImports() {
        return staticImports;
    }
}

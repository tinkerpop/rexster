package com.tinkerpop.rexster.server;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexsterSettingsTest {
    @Test
    public void getPrimeCommands() {
        // don't specifically test -h as it autoprints help and
        final Map<String, String> commands = new HashMap<String, String>() {{
            put("-s", RexsterSettings.COMMAND_START);
            put("-u", RexsterSettings.COMMAND_STATUS);
            put("-x", RexsterSettings.COMMAND_STOP);
            put("-v", RexsterSettings.COMMAND_VERSION);
            put("junk", RexsterSettings.COMMAND_HELP);
        }};

        for (Map.Entry<String,String> entry : commands.entrySet()) {
            final RexsterSettings settings = new RexsterSettings(new String [] { entry.getKey() });

            Assert.assertEquals(entry.getValue(), settings.getPrimeCommand());
        }
    }
}

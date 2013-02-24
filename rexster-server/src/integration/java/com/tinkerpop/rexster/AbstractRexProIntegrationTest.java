package com.tinkerpop.rexster;

import com.tinkerpop.rexster.protocol.EngineController;
import com.tinkerpop.rexster.server.RexProRexsterServer;
import com.tinkerpop.rexster.server.RexsterApplication;
import com.tinkerpop.rexster.server.RexsterServer;
import com.tinkerpop.rexster.server.XmlRexsterApplication;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.List;

public abstract class AbstractRexProIntegrationTest {

    protected RexsterServer rexsterServer;

    static {
        EngineController.configure(-1, null);
    }

    @Before
    public void setUp() throws Exception {
        clean();

        final XMLConfiguration properties = new XMLConfiguration();
        properties.load(Application.class.getResourceAsStream("rexster-integration-test.xml"));
        rexsterServer = new RexProRexsterServer(properties);

        final List<HierarchicalConfiguration> graphConfigs = properties.configurationsAt(Tokens.REXSTER_GRAPH_PATH);
        final RexsterApplication application = new XmlRexsterApplication(graphConfigs);
        EngineController.configure(-1, null);
        rexsterServer.start(application);
    }

    @After
    public void tearDown() throws Exception {
        rexsterServer.stop();
    }

    private static void clean() {
        removeDirectory(new File("/tmp/rexster-integration-tests"));
    }

    private static boolean removeDirectory(final File directory) {
        if (directory == null)
            return false;
        if (!directory.exists())
            return true;
        if (!directory.isDirectory())
            return false;

        final String[] list = directory.list();

        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                final File entry = new File(directory, list[i]);
                if (entry.isDirectory())
                {
                    if (!removeDirectory(entry))
                        return false;
                }
                else
                {
                    if (!entry.delete())
                        return false;
                }
            }
        }

        return directory.delete();
    }

}

package com.tinkerpop.rexster.rexpro.msgpack;

import com.tinkerpop.rexster.client.RexsterClient;
import com.tinkerpop.rexster.client.RexsterClientFactory;
import com.tinkerpop.rexster.rexpro.AbstractScriptRequestIntegrationTest;

public class ScriptRequestIntegrationTest extends AbstractScriptRequestIntegrationTest {

    @Override
    public RexsterClient getClient() throws Exception {
        return RexsterClientFactory.open();
    }
}

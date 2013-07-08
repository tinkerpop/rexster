package com.tinkerpop.rexster.rexpro.json;

import com.tinkerpop.rexster.client.RexsterClient;
import com.tinkerpop.rexster.client.RexsterClientFactory;
import com.tinkerpop.rexster.rexpro.AbstractScriptRequestIntegrationTest;

public class ScriptRequestIntegrationTest extends AbstractScriptRequestIntegrationTest {

    @Override
    public RexsterClient getClient() throws Exception {
        RexsterClient client = RexsterClientFactory.open();
        client.setSerializer((byte) 1);
        return client;
    }
}

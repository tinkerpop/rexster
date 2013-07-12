package com.tinkerpop.rexster.rexpro.msgpack;

import com.tinkerpop.rexster.client.RexsterClient;
import com.tinkerpop.rexster.client.RexsterClientFactory;
import com.tinkerpop.rexster.rexpro.AbstractRexsterClientIntegrationTest;

public class RexsterClientIntegrationTest extends AbstractRexsterClientIntegrationTest {

    @Override
    public boolean supportsPrimitiveKeys() {
        return true;
    }

    @Override
    public RexsterClient getClient() throws Exception {
        return RexsterClientFactory.open();
    }
}

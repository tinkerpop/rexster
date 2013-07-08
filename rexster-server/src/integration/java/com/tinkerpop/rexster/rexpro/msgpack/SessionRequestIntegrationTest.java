package com.tinkerpop.rexster.rexpro.msgpack;

import com.tinkerpop.rexster.client.RexsterClient;
import com.tinkerpop.rexster.client.RexsterClientFactory;
import com.tinkerpop.rexster.rexpro.AbstractSessionRequestMessageTest;

public class SessionRequestIntegrationTest extends AbstractSessionRequestMessageTest {

    @Override
    public RexsterClient getClient() throws Exception {
        return RexsterClientFactory.open();
    }
}

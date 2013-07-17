package com.tinkerpop.rexster.rexpro.json;

import com.tinkerpop.rexster.client.RexsterClient;
import com.tinkerpop.rexster.client.RexsterClientFactory;
import com.tinkerpop.rexster.protocol.serializer.json.JSONSerializer;
import com.tinkerpop.rexster.rexpro.AbstractSessionRequestMessageTest;

public class SessionRequestIntegrationTest extends AbstractSessionRequestMessageTest{

    @Override
    public RexsterClient getClient() throws Exception {
        RexsterClient client = RexsterClientFactory.open();
        client.setSerializer(JSONSerializer.SERIALIZER_ID);
        return client;
    }
}

package com.tinkerpop.rexster.client;

import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import java.io.IOException;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexsterClientHandler extends BaseFilter {
    private RexsterClient client;

    public void setClient(RexsterClient client) {
         this.client = client;
    }

    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        try {
            client.putResponse((RexProMessage) ctx.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return ctx.getStopAction();
    }
}

package com.tinkerpop.rexster.client;

import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import java.io.IOException;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class RexsterClientHandler extends BaseFilter {
    private static final Logger logger = Logger.getLogger(RexsterClientHandler.class);

    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        try {
            RexsterClient.putResponse((RexProMessage) ctx.getMessage());
        }
        catch (Exception e) {
            logger.error("RexProMessage could not be cast to to be place on the response map.", e);
        }

        return ctx.getStopAction();
    }
}

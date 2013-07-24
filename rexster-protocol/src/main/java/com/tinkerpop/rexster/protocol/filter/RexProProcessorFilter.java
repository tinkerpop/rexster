package com.tinkerpop.rexster.protocol.filter;

import com.tinkerpop.rexster.protocol.server.RexProRequest;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import java.io.IOException;

/**
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class RexProProcessorFilter extends BaseFilter {
    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        final RexProRequest request = ctx.getMessage();
        request.process();
        ctx.write(request);
        return ctx.getStopAction();
    }
}

package com.tinkerpop.rexster.protocol.filter;

import com.tinkerpop.rexster.protocol.msg.*;
import com.tinkerpop.rexster.server.RexsterApplication;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.msgpack.MessagePack;

import java.io.IOException;

/**
 * Handles all RexPro i/o
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class RexProFilter extends BaseFilter {
    private static final Logger logger = Logger.getLogger(SessionFilter.class);
    private static final MessagePack msgpack = new MessagePack();
    static {
        msgpack.register(RexProMessageMeta.class, RexProMessageMeta.SerializationTemplate.getInstance());
        msgpack.register(RexProBindings.class, RexProBindings.SerializationTemplate.getInstance());
        msgpack.register(RexProScriptResult.class, RexProScriptResult.SerializationTemplate.getInstance());
    }

    private final RexsterApplication rexsterApplication;

    public RexProFilter(RexsterApplication application) {
        rexsterApplication = application;
    }

    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        return super.handleRead(ctx);
    }
}

package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.gremlin.converter.MsgPackResultConverter;
import org.msgpack.annotation.Message;

/**
 * Represents a response to a script request that formats results to MsgPack format.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
@Message
public class ScriptResponseMessage extends RexProMessage {
    private static final MsgPackResultConverter converter = new MsgPackResultConverter();

    public RexProScriptResult Results = new RexProScriptResult();
    public RexProBindings Bindings = new RexProBindings();

    public static byte[] convertResultToBytes(final Object result) throws Exception {
        return converter.convert(result);
    }
}

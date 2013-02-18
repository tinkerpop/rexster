package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.rexster.gremlin.converter.JSONResultConverter;
import org.msgpack.annotation.Message;

/**
 * Represents a response to a script request that formats results to GraphSON format.  This is the same format
 * that is used by the REST API.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@Message
public class GraphSONScriptResponseMessage extends RexProMessage {
    private static final JSONResultConverter converter = new JSONResultConverter(
            GraphSONMode.EXTENDED, 0, Long.MAX_VALUE, null);

    public String Results;
    public RexProBindings Bindings = new RexProBindings();

    public static String convertResultToBytes(final Object result) throws Exception {
        if (result == null) {
            return null;
        } else {
            return converter.convert(result).toString();
        }
    }

    @Override
    public int estimateMessageSize() {
        //TODO: estimate bindings size
        return BASE_MESSAGE_SIZE + (Results == null ? 0 : Results.length());
    }
}

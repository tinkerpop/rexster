package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.rexster.gremlin.converter.JSONResultConverter;

/**
 * Represents a response to a script request that formats results to GraphSON format.  This is the same format
 * that is used by the REST API.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class GraphSONScriptResponseMessage extends RexProMessage {
    private static final JSONResultConverter converter = new JSONResultConverter(
            GraphSONMode.EXTENDED, 0, Long.MAX_VALUE, null);

    public byte[] Results;
    public byte[] Bindings;

    public static byte[] convertResultToBytes(final Object result) throws Exception {
        return converter.convert(result).toString().getBytes();
    }
}

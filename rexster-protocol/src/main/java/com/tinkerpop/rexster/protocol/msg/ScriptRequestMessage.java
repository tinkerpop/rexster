package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.protocol.BitWorks;
import com.tinkerpop.rexster.protocol.RexsterBindings;
import org.msgpack.annotation.Message;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Represents a request to process a script.
 */
@Message
public class ScriptRequestMessage extends RexProMessage {
    public static final byte FLAG_IN_SESSION = 0;
    public static final byte FLAG_NO_SESSION = 1;

    public String LanguageName;
    public String Script;
    public byte[] Bindings;

    public RexsterBindings getBindings() throws IOException, ClassNotFoundException {
        ByteBuffer buffer = ByteBuffer.wrap(this.Bindings);

        byte[] theRest = new byte[buffer.remaining()];
        buffer.get(theRest);
        return BitWorks.convertByteArrayToRexsterBindings(theRest);
    }
}

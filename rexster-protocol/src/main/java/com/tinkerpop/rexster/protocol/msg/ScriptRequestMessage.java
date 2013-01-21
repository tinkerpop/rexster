package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.protocol.BitWorks;
import org.msgpack.annotation.Message;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Represents a request to process a script.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@Message
public class ScriptRequestMessage extends RexProMessage {
    public String LanguageName;
    public String Script;
    public byte[] Bindings;

    @Override
    public int estimateMessageSize() {
        return BASE_MESSAGE_SIZE
                + (LanguageName == null ? 0 : LanguageName.length())
                + (Script == null ? 0 :Script.length())
                + (Bindings == null ? 0 : Bindings.length);
    }

    public javax.script.Bindings getBindings() throws IOException, ClassNotFoundException {
        final ByteBuffer buffer = ByteBuffer.wrap(this.Bindings);

        final byte[] theRest = new byte[buffer.remaining()];
        buffer.get(theRest);
        return BitWorks.convertBytesToBindings(theRest);
    }
}

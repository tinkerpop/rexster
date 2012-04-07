package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.protocol.BitWorks;
import com.tinkerpop.rexster.protocol.RexsterBindings;
import org.msgpack.annotation.Message;

import java.nio.ByteBuffer;

@Message
public class ScriptRequestMessage extends RexProMessage {
    public String LanguageName;
    public String Script;
    public byte[] Bindings;

    public RexsterBindings getBindings() {
        ByteBuffer buffer = ByteBuffer.wrap(this.Bindings);

        RexsterBindings bindings = null;

        try {
            byte[] theRest = new byte[buffer.remaining()];
            buffer.get(theRest);
            bindings = BitWorks.convertByteArrayToRexsterBindings(theRest);
        } catch (Exception e) {
            // TODO: clean up
            e.printStackTrace();
        }

        return bindings;
    }
}

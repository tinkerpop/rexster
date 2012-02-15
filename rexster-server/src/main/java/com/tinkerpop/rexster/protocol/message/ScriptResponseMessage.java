package com.tinkerpop.rexster.protocol.message;

import com.tinkerpop.rexster.protocol.BitWorks;
import com.tinkerpop.rexster.protocol.RexsterBindings;

import javax.script.Bindings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.UUID;

public class ScriptResponseMessage extends RexProMessage {

    public static final byte FLAG_COMPLETE_MESSAGE = 0;

    public ScriptResponseMessage(RexProMessage message) {
        super(message.getVersion(), message.getType(), message.getFlag(),
                message.getSession(), message.getRequest(), message.getBody());

        if (this.getType() != MessageType.SCRIPT_RESPONSE) {
            throw new IllegalArgumentException("The message is not of type SCRIPT_RESPONSE");
        }
    }

    public ScriptResponseMessage(UUID sessionKey, byte flag, Object result, Bindings bindings) throws IOException {
        super(RexProMessage.CURRENT_VERSION, MessageType.SCRIPT_RESPONSE, flag,
                BitWorks.convertUUIDToByteArray(sessionKey),
                BitWorks.convertUUIDToByteArray(UUID.randomUUID()),
                buildBody(BitWorks.convertSerializableBindingsToByteArray(bindings), getBytesBasedOnObject(result)));
    }

    public RexsterBindings getBindings() {
        ByteBuffer buffer = ByteBuffer.wrap(this.body);
        int bindingsLength = buffer.getInt();

        RexsterBindings bindings = null;

        try {
            byte[] bindingsBytes = new byte[bindingsLength];
            buffer.get(bindingsBytes);
            bindings = BitWorks.convertByteArrayToRexsterBindings(bindingsBytes);
        } catch (Exception e) {
            // TODO: clean up
            e.printStackTrace();
        }

        return bindings;
    }

    private static byte[] buildBody(byte[] bindings, byte[] result) {
        ByteBuffer bb = ByteBuffer.allocate(result.length + bindings.length + 4);
        bb.putInt(bindings.length);
        bb.put(bindings);
        bb.put(result);

        return bb.array();
    }

    private static byte[] getBytesBasedOnObject(Object result) throws IOException {
        if (result instanceof Iterable) {
            ByteArrayOutputStream byteOuputStream = new ByteArrayOutputStream();
            for (Object o : (Iterable) result) {
                byte[] bytesToWrite = BitWorks.getFilteredBytesWithLength(o);
                byteOuputStream.write(bytesToWrite, 0, bytesToWrite.length);
            }

            return byteOuputStream.toByteArray();
        } else if (result instanceof Iterator) {
            ByteArrayOutputStream byteOuputStream = new ByteArrayOutputStream();
            Iterator itty = (Iterator) result;
            while (itty.hasNext()) {
                byte[] bytesToWrite = BitWorks.getFilteredBytesWithLength(itty.next());
                byteOuputStream.write(bytesToWrite, 0, bytesToWrite.length);
            }

            return byteOuputStream.toByteArray();
        } else {
            return BitWorks.getFilteredBytesWithLength(result);
        }
    }
}

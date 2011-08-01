package com.tinkerpop.rexster.protocol.message;

import com.tinkerpop.rexster.gremlin.converter.ConsoleResultConverter;
import com.tinkerpop.rexster.protocol.BitWorks;

import javax.script.Bindings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConsoleScriptResponseMessage extends RexProMessage {

    public ConsoleScriptResponseMessage(RexProMessage message) {
        super(message.getVersion(), message.getType(), message.getFlag(),
                message.getSession(), message.getRequest(), message.getBody());

        if (this.getType() != MessageType.SCRIPT_RESPONSE) {
            throw new IllegalArgumentException("The message is not of type SCRIPT_RESPONSE");
        }
    }

    public ConsoleScriptResponseMessage(UUID sessionKey, byte flag, Object result, Bindings bindings) throws IOException {
        super(RexProMessage.CURRENT_VERSION, MessageType.SCRIPT_RESPONSE, flag,
                BitWorks.convertUUIDToByteArray(sessionKey),
                BitWorks.convertUUIDToByteArray(UUID.randomUUID()),
                buildBody(convertBindingsToByteArray(bindings), convertResultToConsoleLineBytes(result)));
    }


    private static byte[] buildBody(byte[] bindings, byte[] result) {
        ByteBuffer bb = ByteBuffer.allocate(result.length + bindings.length + 4);
        bb.putInt(bindings.length);
        bb.put(bindings);
        bb.put(result);

        return bb.array();
    }

    public List<String> getBindings() {
        ByteBuffer buffer = ByteBuffer.wrap(this.body);
        int bindingsLength = buffer.getInt();

        List<String> bindings = new ArrayList<String>();

        try {
            byte[] bindingsBytes = new byte[bindingsLength];
            buffer.get(bindingsBytes);

            ByteBuffer bb = ByteBuffer.wrap(bindingsBytes);

            while (bb.hasRemaining()) {
                int segmentLength = bb.getInt();
                byte[] segmentBytes = new byte[segmentLength];
                bb.get(segmentBytes);

                bindings.add(new String(segmentBytes));
            }

        } catch (Exception e) {
            // TODO: clean up
            e.printStackTrace();
        }

        return bindings;
    }

    private static byte[] convertBindingsToByteArray(Bindings bindings) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            for (String key : bindings.keySet()) {
                baos.write(BitWorks.convertStringsToByteArray(key + "=" + bindings.get(key).toString()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return baos.toByteArray();
    }


    private static byte[] convertResultToConsoleLineBytes(Object result) {
        ConsoleResultConverter converter = new ConsoleResultConverter(new StringWriter());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        List<String> linesAsList = new ArrayList<String>();
        try {
            linesAsList = converter.convert(result);

            for (String lineString : linesAsList) {
                baos.write(BitWorks.convertStringsToByteArray(lineString.toString()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return baos.toByteArray();
    }
}

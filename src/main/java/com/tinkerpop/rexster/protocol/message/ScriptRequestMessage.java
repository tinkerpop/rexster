package com.tinkerpop.rexster.protocol.message;

import com.tinkerpop.rexster.protocol.BitWorks;
import com.tinkerpop.rexster.protocol.RexsterBindings;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class ScriptRequestMessage extends RexProMessage {

    public ScriptRequestMessage(RexProMessage message) {
        super(message.getVersion(), message.getType(), message.getFlag(),
                message.getSession(), message.getRequest(), message.getBody());

        if (this.getType() != MessageType.SCRIPT_REQUEST) {
            throw new IllegalArgumentException("The message is not of type SCRIPT_REQUEST");
        }
    }

    public ScriptRequestMessage(UUID sessionKey, String languageName, RexsterBindings bindings, String script) throws IOException {
        super(RexProMessage.CURRENT_VERSION, MessageType.SCRIPT_REQUEST, (byte) 0,
                BitWorks.convertUUIDToByteArray(sessionKey),
                BitWorks.convertUUIDToByteArray(UUID.randomUUID()),
                buildBody(languageName, script, bindings));
    }

    public String getLanguageName() {
        ByteBuffer buffer = ByteBuffer.wrap(this.body);
        int languageLength = buffer.getInt();

        byte[] languageBytes = new byte[languageLength];
        buffer.get(languageBytes);
        return new String(languageBytes);
    }

    public String getScript() {
        ByteBuffer buffer = ByteBuffer.wrap(this.body);
        int languageLength = buffer.getInt();

        int languageSegmentOffset = languageLength + 4;
        buffer.position(languageSegmentOffset);

        int scriptLength = buffer.getInt();

        byte[] scriptBytes = new byte[scriptLength];
        buffer.get(scriptBytes);
        return new String(scriptBytes);
    }

    public RexsterBindings getBindings() {
        ByteBuffer buffer = ByteBuffer.wrap(this.body);
        int languageLength = buffer.getInt();

        int languageSegmentOffset = languageLength + 4;
        buffer.position(languageSegmentOffset);

        int scriptLength = buffer.getInt();
        buffer.position(scriptLength + 4 + languageSegmentOffset);

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

    private static byte[] buildBody(String languageName, String script, RexsterBindings bindings) throws IOException {
        byte[] languageNameAndScriptBytes = BitWorks.convertStringsToByteArray(languageName, script);
        byte[] bindingsBytes = BitWorks.convertSerializableBindingsToByteArray(bindings);

        ByteBuffer bb = ByteBuffer.allocate(languageNameAndScriptBytes.length + bindingsBytes.length);
        bb.put(languageNameAndScriptBytes);
        bb.put(bindingsBytes);

        return bb.array();
    }
}

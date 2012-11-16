package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.gremlin.converter.ConsoleResultConverter;
import com.tinkerpop.rexster.protocol.BitWorks;
import org.msgpack.annotation.Message;

import javax.script.Bindings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a response to a script request for use in a console where each results is serialized to a
 * string value in an array of strings.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@Message
public class ConsoleScriptResponseMessage extends RexProMessage {

    public String[] ConsoleLines;
    public byte[] Bindings;

    public List<String> consoleLinesAsList() {
        final List<String> list = new ArrayList<String>();
        for (String line : ConsoleLines) {
            list.add(line);
        }

        return list;
    }

    public List<String> bindingsAsList() {
        final List<String> bindings = new ArrayList<String>();

        final ByteBuffer bb = ByteBuffer.wrap(this.Bindings);

        while (bb.hasRemaining()) {
            final int segmentLength = bb.getInt();
            final byte[] segmentBytes = new byte[segmentLength];
            bb.get(segmentBytes);

            bindings.add(new String(segmentBytes));
        }

        return bindings;
    }

    public static byte[] convertBindingsToByteArray(final Bindings bindings) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (String key : bindings.keySet()) {
            final Object val = bindings.get(key);
            baos.write(BitWorks.convertStringsToByteArray(key + "=" + (val == null ? "null" : val.toString())));
        }

        return baos.toByteArray();
    }

    public static List<String> convertResultToConsoleLines(final Object result) throws Exception {
        final ConsoleResultConverter converter = new ConsoleResultConverter(new StringWriter());
        return converter.convert(result);
    }

    @Override
    public int estimateMessageSize() {
        return BASE_MESSAGE_SIZE + (Bindings == null ? 0 : Bindings.length) + estimateConsoleLineSize();
    }

    private int estimateConsoleLineSize() {
        int size = 0;
        if (ConsoleLines != null) {
            for(String cl : ConsoleLines) {
                size = size + cl.length();
            }
        }

        return size;
    }
}

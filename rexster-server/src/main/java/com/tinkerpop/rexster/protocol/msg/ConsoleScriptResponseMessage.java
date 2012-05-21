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

@Message
public class ConsoleScriptResponseMessage extends RexProMessage {
    public static final byte FLAG_COMPLETE_MESSAGE = 0;

    public String[] ConsoleLines;
    public byte[] Bindings;

    public List<String> ConsoleLinesAsList() {
        List<String> list = new ArrayList<String>();
        for (String line : ConsoleLines) {
            list.add(line);
        }

        return list;
    }

    public List<String> BindingsAsList() {
        List<String> bindings = new ArrayList<String>();

        ByteBuffer bb = ByteBuffer.wrap(this.Bindings);

        while (bb.hasRemaining()) {
            int segmentLength = bb.getInt();
            byte[] segmentBytes = new byte[segmentLength];
            bb.get(segmentBytes);

            bindings.add(new String(segmentBytes));
        }

        return bindings;
    }

    public static byte[] convertBindingsToByteArray(Bindings bindings) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (String key : bindings.keySet()) {
            final Object val = bindings.get(key);
            baos.write(BitWorks.convertStringsToByteArray(key + "=" + (val == null ? "null" : val.toString())));
        }

        return baos.toByteArray();
    }

    public static List<String> convertResultToConsoleLines(Object result) throws Exception {
        ConsoleResultConverter converter = new ConsoleResultConverter(new StringWriter());
        return converter.convert(result);
    }
}

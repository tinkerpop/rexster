package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.gremlin.converter.ConsoleResultConverter;
import com.tinkerpop.rexster.protocol.BitWorks;
import org.msgpack.annotation.Message;

import javax.script.Bindings;
import java.io.ByteArrayOutputStream;
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

        try {
            ByteBuffer bb = ByteBuffer.wrap(this.Bindings);

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

    public static byte[] convertBindingsToByteArray(Bindings bindings) {
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

    public static List<String> convertResultToConsoleLines(Object result) {
        ConsoleResultConverter converter = new ConsoleResultConverter(new StringWriter());
        List<String> linesAsList = new ArrayList<String>();
        try {
            linesAsList = converter.convert(result);
        } catch (Exception ex) {
            // TODO: cleanup
            ex.printStackTrace();
        }
        
        return linesAsList;
    }
}

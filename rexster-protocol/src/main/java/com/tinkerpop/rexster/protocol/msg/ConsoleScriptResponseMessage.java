package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.gremlin.converter.ConsoleResultConverter;
import org.msgpack.annotation.Message;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a response to a script request for use in a console where each results is serialized to a
 * string value in an array of strings.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@Message
public class ConsoleScriptResponseMessage extends RexProMessage {

    public String[] ConsoleLines;
    public RexProBindings Bindings = new RexProBindings();

    public ConsoleScriptResponseMessage() {
        super();
    }

    public List<String> consoleLinesAsList() {
        final List<String> list = new ArrayList<String>();
        for (String line : ConsoleLines) {
            list.add(line);
        }

        return list;
    }

    public List<String> bindingsAsList() {
        final List<String> bindings = new ArrayList<String>();

        for(Map.Entry pair: this.Bindings.entrySet()) {
            if (pair.getValue() == null) {
                bindings.add(pair.getKey() + "=null");
            } else {
                bindings.add(pair.getKey() + "=" + pair.getValue().toString());
            }
        }

        return bindings;
    }

    public static List<String> convertResultToConsoleLines(final Object result) throws Exception {
        final ConsoleResultConverter converter = new ConsoleResultConverter(new StringWriter());
        return converter.convert(result);
    }
}

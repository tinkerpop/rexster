package com.tinkerpop.rexster.gremlin.converter;

import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ConsoleResultConverter implements ResultConverter<List<String>> {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final Writer outputWriter;

    public ConsoleResultConverter(final Writer outputWriter) {
        this.outputWriter = outputWriter;
    }

    public List<String> convert(final Object result) throws Exception {
        try {
            List<Object> resultLines = new ArrayList<Object>();
            if (result == null) {
                resultLines = new ArrayList<Object>();
            } else if (result instanceof Iterable) {
                for (Object o : (Iterable) result) {
                    resultLines.add(o);
                }
            } else if (result instanceof Iterator) {
                // Table is handled through here and the toString() to get it formatted.
                Iterator itty = (Iterator) result;
                while (itty.hasNext()) {
                    resultLines.add(itty.next());
                }
            } else if (result.getClass().isArray()) {
                int length = Array.getLength(result);
                for (int ix = 0; ix < length; ix++) {
                    resultLines.add(Array.get(result, ix).toString());
                }
            } else if (result instanceof Map) {
                Map map = (Map) result;
                for (Object key : map.keySet()) {
                    resultLines.add(key + "=" + map.get(key).toString());
                }
            } else if (result instanceof Throwable) {
                resultLines.add(((Throwable) result).getMessage());
            } else {
                resultLines.add(result);
            }

            // Handle output data
            List<String> outputLines = new ArrayList<String>();

            // Handle eval() result
            String[] printLines = this.outputWriter.toString().split(LINE_SEPARATOR);

            if (printLines.length > 0 && printLines[0].length() > 0) {
                for (String printLine : printLines) {
                    outputLines.add(printLine);
                }
            }

            if (resultLines != null && resultLines.size() > 0) {
                // Make sure all lines are strings
                for (Object resultLine : resultLines) {
                    outputLines.add(resultLine != null ? resultLine.toString() : "null");
                }
            }

            return outputLines;
        } catch (Exception ex) {
            ArrayList<String> resultList = new ArrayList<String>();
            resultList.add(ex.getMessage());
            return resultList;
        }
    }
}

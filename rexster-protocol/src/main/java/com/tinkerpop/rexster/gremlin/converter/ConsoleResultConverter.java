package com.tinkerpop.rexster.gremlin.converter;

import com.tinkerpop.pipes.util.iterators.SingleIterator;
import org.apache.commons.collections.iterators.ArrayIterator;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class ConsoleResultConverter implements ResultConverter<List<String>> {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final Writer outputWriter;

    public ConsoleResultConverter(final Writer outputWriter) {
        this.outputWriter = outputWriter;
    }

    public List<String> convert(final Object result) throws Exception {
        try {
            final List<Object> resultLines = new ArrayList<Object>();
            final Iterator itty;
            if (result instanceof Iterable) {
                itty = ((Iterable) result).iterator();
            } else if (result instanceof Iterator) {
                itty = (Iterator) result;
            } else if (result instanceof Object[]) {
                itty = new ArrayIterator((Object[]) result);
            } else if (result instanceof Map) {
                itty = ((Map) result).entrySet().iterator();
            } else if (result instanceof Throwable) {
                itty = new SingleIterator<Object>(((Throwable) result).getMessage());
            } else {
                itty = new SingleIterator<Object>(result);
            }

            while (itty.hasNext()) {
                resultLines.add(itty.next());
            }

            // Handle output data
            final List<String> outputLines = new ArrayList<String>();

            // Handle eval() result
            final String[] printLines = this.outputWriter.toString().split(LINE_SEPARATOR);

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
            final ArrayList<String> resultList = new ArrayList<String>();
            resultList.add(ex.getMessage());
            return resultList;
        }
    }
}

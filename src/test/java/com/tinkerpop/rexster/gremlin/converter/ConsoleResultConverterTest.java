package com.tinkerpop.rexster.gremlin.converter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

public class ConsoleResultConverterTest {

    private ConsoleResultConverter converter = new ConsoleResultConverter();

    @Test
    public void convertNullResultReturnsEmptyList() throws Exception {
        Writer writer = new StringWriter();
        List<String> converted = this.converter.convert(null, writer);

        Assert.assertNotNull(converted);
        Assert.assertEquals(1, converted.size());

        Assert.assertEquals("", converted.get(0));
    }

    @Test
    public void convertIterable() throws Exception {
        Writer writer = new StringWriter();

        ArrayList<FunObject> funList = new ArrayList<FunObject>();
        funList.add(new FunObject("x"));
        funList.add(new FunObject("y"));
        Iterable<FunObject> iterable = funList;

        List<String> converted = this.converter.convert(iterable, writer);

        Assert.assertNotNull(converted);
        Assert.assertEquals(2, converted.size());

        Assert.assertEquals("x", converted.get(0));
        Assert.assertEquals("y", converted.get(1));
    }

    @Test
    public void convertIterator() throws Exception {
        Writer writer = new StringWriter();

        ArrayList<FunObject> funList = new ArrayList<FunObject>();
        funList.add(new FunObject("x"));
        funList.add(new FunObject("y"));
        Iterator<FunObject> iterable = funList.iterator();

        List<String> converted = this.converter.convert(iterable, writer);

        Assert.assertNotNull(converted);
        Assert.assertEquals(2, converted.size());

        Assert.assertEquals("x", converted.get(0));
        Assert.assertEquals("y", converted.get(1));
    }

    @Test
    public void convertMap() throws Exception {
        Writer writer = new StringWriter();

        Map<String, FunObject> map = new HashMap<String, FunObject>();
        map.put("X", new FunObject("x"));
        map.put("Y", new FunObject("y"));

        List<String> converted = this.converter.convert(map, writer);

        Assert.assertNotNull(converted);
        Assert.assertEquals(2, converted.size());

        boolean foundX = false;
        boolean foundY = false;
        for (String line : converted) {
            if (line.equals("X=x")) {
                foundX = true;
            } else if (line.equals("Y=y")) {
                foundY = true;
            }
        }

        Assert.assertTrue(foundX);
        Assert.assertTrue(foundY);
    }

    @Test
    public void convertThrowable() throws Exception {
        Writer writer = new StringWriter();

        Throwable throwable = new Exception("message");
        List<String> converted = this.converter.convert(throwable, writer);

        Assert.assertNotNull(converted);
        Assert.assertEquals(1, converted.size());
        Assert.assertEquals("message", converted.get(0));
    }

    @Test
    public void convertWriter() throws Exception {
        Writer writer = new StringWriter();
        writer.write("x\n");
        writer.write("y");

        List<String> converted = this.converter.convert(null, writer);

        Assert.assertNotNull(converted);
        Assert.assertEquals(2, converted.size());
        Assert.assertEquals("x", converted.get(0));
        Assert.assertEquals("y", converted.get(1));
    }

    private class FunObject {
        private String val;

        public FunObject(String val) {
            this.val = val;
        }

        public String toString() {
            return this.val;
        }
    }
}

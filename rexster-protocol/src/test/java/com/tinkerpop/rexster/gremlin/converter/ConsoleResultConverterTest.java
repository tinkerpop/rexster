package com.tinkerpop.rexster.gremlin.converter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ConsoleResultConverterTest {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private Writer writer;
    private ConsoleResultConverter converter;

    @Before
    public void setUp() {
        this.writer = new StringWriter();
        this.converter = new ConsoleResultConverter(writer);
    }

    @Test
    public void convertNullResultReturnsZeroSizeList() throws Exception {

        List<String> converted = this.converter.convert(null);

        Assert.assertNotNull(converted);
        Assert.assertEquals(1, converted.size());
        Assert.assertEquals("null", converted.get(0));
    }

    @Test
    public void convertIterable() throws Exception {

        ArrayList<FunObject> funList = new ArrayList<FunObject>();
        funList.add(new FunObject("x"));
        funList.add(new FunObject("y"));
        Iterable<FunObject> iterable = funList;

        List<String> converted = this.converter.convert(iterable);

        Assert.assertNotNull(converted);
        Assert.assertEquals(2, converted.size());

        Assert.assertEquals("x", converted.get(0));
        Assert.assertEquals("y", converted.get(1));
    }

    @Test
    public void convertIterableWithNull() throws Exception {

        ArrayList<FunObject> funList = new ArrayList<FunObject>();
        funList.add(new FunObject("x"));
        funList.add(null);
        funList.add(new FunObject("y"));
        Iterable<FunObject> iterable = funList;

        List<String> converted = this.converter.convert(iterable);

        Assert.assertNotNull(converted);
        Assert.assertEquals(3, converted.size());

        Assert.assertEquals("x", converted.get(0));
        Assert.assertEquals("null", converted.get(1));
        Assert.assertEquals("y", converted.get(2));
    }

    @Test
    public void convertIterator() throws Exception {

        ArrayList<FunObject> funList = new ArrayList<FunObject>();
        funList.add(new FunObject("x"));
        funList.add(new FunObject("y"));
        Iterator<FunObject> iterable = funList.iterator();

        List<String> converted = this.converter.convert(iterable);

        Assert.assertNotNull(converted);
        Assert.assertEquals(2, converted.size());

        Assert.assertEquals("x", converted.get(0));
        Assert.assertEquals("y", converted.get(1));
    }

    @Test
    public void convertMap() throws Exception {

        Map<String, FunObject> map = new HashMap<String, FunObject>();
        map.put("X", new FunObject("x"));
        map.put("Y", new FunObject("y"));

        List<String> converted = this.converter.convert(map);

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

        Throwable throwable = new Exception("message");
        List<String> converted = this.converter.convert(throwable);

        Assert.assertNotNull(converted);
        Assert.assertEquals(1, converted.size());
        Assert.assertEquals("message", converted.get(0));
    }

    @Test
    public void convertWriter() throws Exception {
        this.writer.write("x" + LINE_SEPARATOR);
        this.writer.write("y");

        List<String> converted = this.converter.convert(null);

        Assert.assertNotNull(converted);
        Assert.assertEquals(3, converted.size());
        Assert.assertEquals("x", converted.get(0));
        Assert.assertEquals("y", converted.get(1));
        Assert.assertEquals("null", converted.get(2));
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

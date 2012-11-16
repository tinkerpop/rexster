package com.tinkerpop.rexster.protocol.msg;

import junit.framework.Assert;
import org.junit.Test;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ConsoleScriptResponseMessageTest {

    @Test
    public void consoleLinesAsListValid() {
        final ConsoleScriptResponseMessage msg = new ConsoleScriptResponseMessage();
        msg.ConsoleLines = new String[] { "a", "b", "c" };

        final List<String> lines = msg.consoleLinesAsList();
        Assert.assertEquals(3, lines.size());
        Assert.assertEquals("a", lines.get(0));
        Assert.assertEquals("b", lines.get(1));
        Assert.assertEquals("c", lines.get(2));
    }

    @Test
    public void bindingsAsListValid() throws IOException {
        final ConsoleScriptResponseMessage msg = new ConsoleScriptResponseMessage();
        final Bindings b = new SimpleBindings();
        b.put("a", "aaa");
        b.put("b", "bbb");
        b.put("c", 3);

        msg.Bindings = ConsoleScriptResponseMessage.convertBindingsToByteArray(b);

        final List<String> bindingsList = msg.bindingsAsList();
        Assert.assertEquals(3, bindingsList.size());
        Assert.assertEquals("a=aaa", bindingsList.get(2));
        Assert.assertEquals("b=bbb", bindingsList.get(0));
        Assert.assertEquals("c=3", bindingsList.get(1));
    }

    @Test
    public void estimateMessageSize() throws IOException {
        final ConsoleScriptResponseMessage msg = new ConsoleScriptResponseMessage();
        msg.ConsoleLines = new String[] { "a", "b", "c" };

        final Bindings b = new SimpleBindings();
        b.put("a", "aaa");
        b.put("b", "bbb");
        b.put("c", 3);

        msg.Bindings = ConsoleScriptResponseMessage.convertBindingsToByteArray(b);

        Assert.assertEquals(64, msg.estimateMessageSize());
    }
}

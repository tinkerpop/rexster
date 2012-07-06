package com.tinkerpop.rexster.protocol;

import org.junit.Assert;
import org.junit.Test;

import javax.script.Bindings;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class RexsterBindingsTest {
    @Test
    public void putSerializableInstance() {
        Bindings bindings = new RexsterBindings();
        Object oldValue = bindings.put("ok", new WillSerialize());

        Assert.assertEquals("y", bindings.get("ok").toString());
        Assert.assertNull(oldValue);

        WillSerialize newOne = new WillSerialize();
        newOne.someField = "z";
        oldValue = bindings.put("ok", newOne);

        Assert.assertEquals("y", oldValue.toString());
        Assert.assertEquals("z", bindings.get("ok").toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void putEnforceRexsterBindingsInstance() {
        Bindings bindings = new RexsterBindings();
        bindings.put("fail", new WontSerialize());
    }

    @Test
    public void putAllSerializableInstance() {
        Bindings bindings = new RexsterBindings();

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("y", new WillSerialize());
        map.put("z", "some string");

        bindings.putAll(map);

        Assert.assertEquals("y", bindings.get("y").toString());
        Assert.assertEquals("some string", bindings.get("z").toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void putAllEnforceRexsterBindingsInstance() {
        Bindings bindings = new RexsterBindings();

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("y", new WontSerialize());

        bindings.putAll(map);
    }

    private class WontSerialize {
        public String someField = "x";
    }

    private class WillSerialize implements Serializable {
        public String someField = "y";

        public String toString() {
            return this.someField;
        }
    }
}

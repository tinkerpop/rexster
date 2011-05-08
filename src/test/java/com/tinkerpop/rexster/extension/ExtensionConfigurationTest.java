package com.tinkerpop.rexster.extension;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class ExtensionConfigurationTest {

    @Test(expected = IllegalArgumentException.class)
    public void constructEmptyNamespace() {
        new ExtensionConfiguration("", "name", new HierarchicalConfiguration());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNullNamespace() {
        new ExtensionConfiguration(null, "name", new HierarchicalConfiguration());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructEmptyName() {
        new ExtensionConfiguration("ns", "", new HierarchicalConfiguration());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNullName() {
        new ExtensionConfiguration("ns", null, new HierarchicalConfiguration());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructNullConfiguration() {
        new ExtensionConfiguration("ns", "name", null);
    }

    @Test
    public void constructValid() {
        HierarchicalConfiguration hc = new HierarchicalConfiguration();
        ExtensionConfiguration config = new ExtensionConfiguration("ns", "name", hc);

        Assert.assertEquals("ns", config.getNamespace());
        Assert.assertEquals("name", config.getExtensionName());
        Assert.assertEquals(hc, config.getConfiguration());
    }

    @Test
    public void tryGetMapFromConfigurationEmptyConfig() {
        HierarchicalConfiguration hc = new HierarchicalConfiguration();
        ExtensionConfiguration config = new ExtensionConfiguration("ns", "name", hc);

        Map<String, String> map = config.tryGetMapFromConfiguration();
        Assert.assertNotNull(map);
        Assert.assertTrue(map.isEmpty());
    }

    @Test
    public void tryGetMapFromConfigurationBadConfig() {
        HierarchicalConfiguration hc = new HierarchicalConfiguration();
        HierarchicalConfiguration innerHc = new HierarchicalConfiguration();
        innerHc.addProperty("test", "1");
        hc.addProperty("test-bad", innerHc);

        ExtensionConfiguration config = new ExtensionConfiguration("ns", "name", hc);

        Map<String, String> map = config.tryGetMapFromConfiguration();
        Assert.assertNull(map);
    }

    @Test
    public void tryGetMapFromConfigurationNiceConfig() {
        HierarchicalConfiguration hc = new HierarchicalConfiguration();
        hc.addProperty("key1", "value1");
        hc.addProperty("key2", "value2");
        ExtensionConfiguration config = new ExtensionConfiguration("ns", "name", hc);

        Map<String, String> map = config.tryGetMapFromConfiguration();
        Assert.assertNotNull(map);
        Assert.assertTrue(map.containsKey("key1"));
        Assert.assertEquals("value1", map.get("key1"));
        Assert.assertTrue(map.containsKey("key2"));
        Assert.assertEquals("value2", map.get("key2"));
    }
}

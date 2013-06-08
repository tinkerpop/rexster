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

    @Test
    public void shouldEqual() {
        HierarchicalConfiguration hc1 = new HierarchicalConfiguration();
        hc1.addProperty("key1", "value1");
        hc1.addProperty("key2", "value2");
        ExtensionConfiguration config1 = new ExtensionConfiguration("ns", "name", hc1);

        HierarchicalConfiguration hc2 = new HierarchicalConfiguration();
        hc2.addProperty("key1", "value1");
        hc2.addProperty("key2", "value2");
        ExtensionConfiguration config2 = new ExtensionConfiguration("ns", "name", hc2);

        Assert.assertTrue(config1.equals(config2));
    }

    @Test
    public void shouldNotEqualWhenValuesInConfigAreDifferent() {
        HierarchicalConfiguration hc1 = new HierarchicalConfiguration();
        hc1.addProperty("key1", "value1");
        hc1.addProperty("key2", "value2");
        ExtensionConfiguration config1 = new ExtensionConfiguration("ns", "name", hc1);

        HierarchicalConfiguration hc2 = new HierarchicalConfiguration();
        hc2.addProperty("key1", "value1");
        hc2.addProperty("key2", "value-not the same");
        ExtensionConfiguration config2 = new ExtensionConfiguration("ns", "name", hc2);

        Assert.assertFalse(config1.equals(config2));
    }

    @Test
    public void shouldNotEqualWhenMissingKeyInConfig() {
        HierarchicalConfiguration hc1 = new HierarchicalConfiguration();
        hc1.addProperty("key1", "value1");
        hc1.addProperty("key2", "value2");
        ExtensionConfiguration config1 = new ExtensionConfiguration("ns", "name", hc1);

        HierarchicalConfiguration hc2 = new HierarchicalConfiguration();
        hc2.addProperty("key1", "value1");
        ExtensionConfiguration config2 = new ExtensionConfiguration("ns", "name", hc2);

        Assert.assertFalse(config1.equals(config2));
    }

    @Test
    public void shouldNotEqualWhenConfig1HasKeyConfig2DoesNot() {
        HierarchicalConfiguration hc1 = new HierarchicalConfiguration();
        hc1.addProperty("key1", "value1");
        hc1.addProperty("key2", "value2");
        ExtensionConfiguration config1 = new ExtensionConfiguration("ns", "name", hc1);

        HierarchicalConfiguration hc2 = new HierarchicalConfiguration();
        hc2.addProperty("key1", "value1");
        hc2.addProperty("new-key", "value1");
        ExtensionConfiguration config2 = new ExtensionConfiguration("ns", "name", hc2);

        Assert.assertFalse(config1.equals(config2));
    }

    @Test
    public void shouldNotEqualWhenConfig2HasAllKeysFromConfig1ButAlsoANewOne() {
        HierarchicalConfiguration hc1 = new HierarchicalConfiguration();
        hc1.addProperty("key1", "value1");
        ExtensionConfiguration config1 = new ExtensionConfiguration("ns", "name", hc1);

        HierarchicalConfiguration hc2 = new HierarchicalConfiguration();
        hc2.addProperty("key1", "value1");
        hc2.addProperty("new-key", "value1");
        ExtensionConfiguration config2 = new ExtensionConfiguration("ns", "name", hc2);

        Assert.assertFalse(config1.equals(config2));
    }

    @Test
    public void shouldNotEqualWhenValuesInConfigAreDifferentDeeperInHierarchy() {
        HierarchicalConfiguration hc1 = new HierarchicalConfiguration();
        hc1.addProperty("key1", "value1");
        HierarchicalConfiguration hc1i = new HierarchicalConfiguration();
        hc1i.addProperty("key2i", "value2i");
        hc1.addProperty("key2", hc1i);

        ExtensionConfiguration config1 = new ExtensionConfiguration("ns", "name", hc1);

        HierarchicalConfiguration hc2 = new HierarchicalConfiguration();
        hc2.addProperty("key1", "value1");

        HierarchicalConfiguration hc2i = new HierarchicalConfiguration();
        hc2i.addProperty("key2i", "value-not the same");
        hc2.addProperty("key2", hc2i);
        ExtensionConfiguration config2 = new ExtensionConfiguration("ns", "name", hc2);

        Assert.assertFalse(config1.equals(config2));
    }

    @Test
    public void shouldEqualWhenValuesInConfigAreSameDeeperInHierarchy() {
        HierarchicalConfiguration hc1 = new HierarchicalConfiguration();
        hc1.addProperty("key1", "value1");
        HierarchicalConfiguration hc1i = new HierarchicalConfiguration();
        hc1i.addProperty("key2i", "value2i");
        hc1.addProperty("key2", hc1i);

        ExtensionConfiguration config1 = new ExtensionConfiguration("ns", "name", hc1);

        HierarchicalConfiguration hc2 = new HierarchicalConfiguration();
        hc2.addProperty("key1", "value1");

        HierarchicalConfiguration hc2i = new HierarchicalConfiguration();
        hc2i.addProperty("key2i", "value2i");
        hc2.addProperty("key2", hc2i);
        ExtensionConfiguration config2 = new ExtensionConfiguration("ns", "name", hc2);

        Assert.assertTrue(config1.equals(config2));
    }
}

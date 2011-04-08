package com.tinkerpop.rexster.extension;

import org.junit.Assert;
import org.junit.Test;

public class ExtensionConfigurationTest {

    @Test(expected = IllegalArgumentException.class)
    public void isExtensionAllowedEmptyNamespace() {
        ExtensionConfiguration configuration = new ExtensionConfiguration("*:*");
        Assert.assertTrue(configuration.isExtensionAllowed("", "extension"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void isExtensionAllowedNullNamespace() {
        ExtensionConfiguration configuration = new ExtensionConfiguration("*:*");
        Assert.assertTrue(configuration.isExtensionAllowed(null, "extension"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void isExtensionAllowedNullExtension() {
        ExtensionConfiguration configuration = new ExtensionConfiguration("*:*");
        Assert.assertTrue(configuration.isExtensionAllowed("ns", null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void isExtensionAllowedEmptyExtension() {
        ExtensionConfiguration configuration = new ExtensionConfiguration("*:*");
        Assert.assertTrue(configuration.isExtensionAllowed("ns", ""));
    }

    @Test
    public void isExtensionAllowedAllowAll() {
        ExtensionConfiguration configuration = new ExtensionConfiguration("*:*");
        Assert.assertTrue(configuration.isExtensionAllowed("ns", "extension"));
    }

    @Test
    public void isExtensionAllowedAllowAllInNamespace() {
        ExtensionConfiguration configuration = new ExtensionConfiguration("ns:*");
        Assert.assertTrue(configuration.isExtensionAllowed("ns", "extension"));
        Assert.assertFalse(configuration.isExtensionAllowed("bs", "extension"));
    }

    @Test
    public void isExtensionAllowedAllowSpecificExtension() {
        ExtensionConfiguration configuration = new ExtensionConfiguration("ns:allowed");
        Assert.assertTrue(configuration.isExtensionAllowed("ns", "allowed"));
        Assert.assertFalse(configuration.isExtensionAllowed("ns", "not-allowed"));
    }
}

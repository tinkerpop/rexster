package com.tinkerpop.rexster.extension;

import com.tinkerpop.rexster.Tokens;
import org.codehaus.jettison.json.JSONObject;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class ExtensionMethodTest {
    private Mockery mockery = new JUnit4Mockery();

    @Test
    public void getExtensionApiAsJsonNullDescriptor() {
        ExtensionMethod method = new ExtensionMethod(null, null, null, null);
        JSONObject api = method.getExtensionApiAsJson();

        Assert.assertNull(api);
    }

    @Test
    public void getExtensionApiAsJsonDescriptorOnlyNoApiElements() {
        this.mockery = new JUnit4Mockery();

        final ExtensionDescriptor extensionDescriptor = this.mockery.mock(ExtensionDescriptor.class);

        this.mockery.checking(new Expectations() {{
            allowing(extensionDescriptor).description();
            will(returnValue("desc"));
            allowing(extensionDescriptor).api();
            will(returnValue(new ExtensionApi[0]));
            allowing(extensionDescriptor).apiBehavior();
            will(returnValue(ExtensionApiBehavior.EXTENSION_DESCRIPTOR_ONLY));
        }});

        ExtensionMethod extensionMethod = new ExtensionMethod(null, null, extensionDescriptor, null);
        JSONObject api = extensionMethod.getExtensionApiAsJson();

        Assert.assertNotNull(api);
        Assert.assertTrue(api.has(Tokens.DESCRIPTION));
        Assert.assertEquals("desc", api.opt(Tokens.DESCRIPTION));
        Assert.assertFalse(api.has(Tokens.PARAMETERS));
    }

    @Test
    public void getExtensionApiAsJsonParameterOnlyNoApiElements() {
        this.mockery = new JUnit4Mockery();

        final ExtensionDescriptor extensionDescriptor = this.mockery.mock(ExtensionDescriptor.class);

        this.mockery.checking(new Expectations() {{
            allowing(extensionDescriptor).description();
            will(returnValue("desc"));
            allowing(extensionDescriptor).api();
            will(returnValue(new ExtensionApi[0]));
            allowing(extensionDescriptor).apiBehavior();
            will(returnValue(ExtensionApiBehavior.EXTENSION_PARAMETER_ONLY));
        }});

        // just need a method that doesn't have any annotations
        Method[] methods = ExtensionMethodTest.class.getMethods();

        ExtensionMethod extensionMethod = new ExtensionMethod(methods[0], null, extensionDescriptor, null);
        JSONObject api = extensionMethod.getExtensionApiAsJson();

        Assert.assertNotNull(api);
        Assert.assertTrue(api.has(Tokens.DESCRIPTION));
        Assert.assertEquals("desc", api.opt(Tokens.DESCRIPTION));
        Assert.assertFalse(api.has(Tokens.PARAMETERS));
    }

    @Test
    public void getExtensionApiAsJsonDefaultNoApiElements() {
        this.mockery = new JUnit4Mockery();

        final ExtensionDescriptor extensionDescriptor = this.mockery.mock(ExtensionDescriptor.class);

        this.mockery.checking(new Expectations() {{
            allowing(extensionDescriptor).description();
            will(returnValue("desc"));
            allowing(extensionDescriptor).api();
            will(returnValue(new ExtensionApi[0]));
            allowing(extensionDescriptor).apiBehavior();
            will(returnValue(ExtensionApiBehavior.DEFAULT));
        }});

        // just need a method that doesn't have any annotations
        Method[] methods = ExtensionMethodTest.class.getMethods();

        ExtensionMethod extensionMethod = new ExtensionMethod(methods[0], null, extensionDescriptor, null);
        JSONObject api = extensionMethod.getExtensionApiAsJson();

        Assert.assertNotNull(api);
        Assert.assertTrue(api.has(Tokens.DESCRIPTION));
        Assert.assertEquals("desc", api.opt(Tokens.DESCRIPTION));
        Assert.assertFalse(api.has(Tokens.PARAMETERS));
    }

    @Test
    public void getExtensionApiAsJsonDescriptorOnlyHasApiElements() {
        this.mockery = new JUnit4Mockery();

        final ExtensionDescriptor extensionDescriptor = this.mockery.mock(ExtensionDescriptor.class);
        final ExtensionApi extensionApi = this.mockery.mock(ExtensionApi.class);

        this.mockery.checking(new Expectations() {{
            allowing(extensionDescriptor).description();
            will(returnValue("desc"));
            allowing(extensionDescriptor).api();
            will(returnValue(new ExtensionApi[]{extensionApi}));
            allowing(extensionDescriptor).apiBehavior();
            will(returnValue(ExtensionApiBehavior.EXTENSION_DESCRIPTOR_ONLY));
            allowing(extensionApi).parameterName();
            will(returnValue("param1"));
            allowing(extensionApi).description();
            will(returnValue("value1"));
        }});

        ExtensionMethod extensionMethod = new ExtensionMethod(null, null, extensionDescriptor, null);
        JSONObject api = extensionMethod.getExtensionApiAsJson();

        Assert.assertNotNull(api);
        Assert.assertTrue(api.has(Tokens.DESCRIPTION));
        Assert.assertEquals("desc", api.opt(Tokens.DESCRIPTION));
        Assert.assertTrue(api.has(Tokens.PARAMETERS));

        JSONObject params = api.optJSONObject(Tokens.PARAMETERS);
        Assert.assertNotNull(params);
        Assert.assertTrue(params.has("param1"));
        Assert.assertEquals("value1", params.optString("param1"));

    }

    @Test
    public void getExtensionApiAsJsonParameterOnlyHasApiElements() {
        this.mockery = new JUnit4Mockery();

        final ExtensionDescriptor extensionDescriptor = this.mockery.mock(ExtensionDescriptor.class);
        final ExtensionApi extensionApi = this.mockery.mock(ExtensionApi.class);

        this.mockery.checking(new Expectations() {{
            allowing(extensionDescriptor).description();
            will(returnValue("desc"));
            allowing(extensionDescriptor).api();
            will(returnValue(new ExtensionApi[]{extensionApi}));
            allowing(extensionDescriptor).apiBehavior();
            will(returnValue(ExtensionApiBehavior.EXTENSION_PARAMETER_ONLY));
        }});

        Method[] methods = MockMethodHelper.class.getDeclaredMethods();

        ExtensionMethod extensionMethod = new ExtensionMethod(methods[0], null, extensionDescriptor, null);
        JSONObject api = extensionMethod.getExtensionApiAsJson();

        Assert.assertNotNull(api);
        Assert.assertTrue(api.has(Tokens.DESCRIPTION));
        Assert.assertEquals("desc", api.opt(Tokens.DESCRIPTION));
        Assert.assertTrue(api.has(Tokens.PARAMETERS));

        JSONObject params = api.optJSONObject(Tokens.PARAMETERS);
        Assert.assertNotNull(params);
        Assert.assertTrue(params.has("nme"));
        Assert.assertEquals("dsc", params.optString("nme"));

    }

    @Test
    public void getExtensionApiAsJsonDefaultHasApiElements() {
        this.mockery = new JUnit4Mockery();

        final ExtensionDescriptor extensionDescriptor = this.mockery.mock(ExtensionDescriptor.class);
        final ExtensionApi extensionApi = this.mockery.mock(ExtensionApi.class);

        this.mockery.checking(new Expectations() {{
            allowing(extensionDescriptor).description();
            will(returnValue("desc"));
            allowing(extensionDescriptor).api();
            will(returnValue(new ExtensionApi[]{extensionApi}));
            allowing(extensionDescriptor).apiBehavior();
            will(returnValue(ExtensionApiBehavior.DEFAULT));
            allowing(extensionApi).parameterName();
            will(returnValue("param1"));
            allowing(extensionApi).description();
            will(returnValue("value1"));
        }});

        Method[] methods = MockMethodHelper.class.getDeclaredMethods();

        ExtensionMethod extensionMethod = new ExtensionMethod(methods[0], null, extensionDescriptor, null);
        JSONObject api = extensionMethod.getExtensionApiAsJson();

        Assert.assertNotNull(api);
        Assert.assertTrue(api.has(Tokens.DESCRIPTION));
        Assert.assertEquals("desc", api.opt(Tokens.DESCRIPTION));
        Assert.assertTrue(api.has(Tokens.PARAMETERS));

        JSONObject params = api.optJSONObject(Tokens.PARAMETERS);
        Assert.assertNotNull(params);
        Assert.assertTrue(params.has("nme"));
        Assert.assertEquals("dsc", params.optString("nme"));
        Assert.assertNotNull(params);
        Assert.assertTrue(params.has("param1"));
        Assert.assertEquals("value1", params.optString("param1"));

    }

    private class MockMethodHelper {
        public void methodTest(@ExtensionRequestParameter(name = "nme", description = "dsc") String x) {
        }
    }
}

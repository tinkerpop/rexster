package com.tinkerpop.rexster.extension;

import com.tinkerpop.rexster.Tokens;
import org.codehaus.jettison.json.JSONObject;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

public class ExtensionMethodTest {
    private Mockery mockery = new JUnit4Mockery();

    @Test
    public void getExtensionApiAsJsonNullDescriptor() {
        ExtensionMethod method = new ExtensionMethod(null, null, null);
        JSONObject api = method.getExtensionApiAsJson();

        Assert.assertNull(api);
    }

    @Test
    public void getExtensionApiAsJsonNoApiElements() {
        this.mockery = new JUnit4Mockery();

        final ExtensionDescriptor extensionDescriptor = this.mockery.mock(ExtensionDescriptor.class);

        this.mockery.checking(new Expectations() {{
            allowing(extensionDescriptor).description();
            will(returnValue("desc"));
            allowing(extensionDescriptor).api();
            will(returnValue(new ExtensionApi[0]));
        }});

        ExtensionMethod extensionMethod = new ExtensionMethod(null, null, extensionDescriptor);
        JSONObject api = extensionMethod.getExtensionApiAsJson();

        Assert.assertNotNull(api);
        Assert.assertTrue(api.has(Tokens.DESCRIPTION));
        Assert.assertEquals("desc", api.opt(Tokens.DESCRIPTION));
        Assert.assertFalse(api.has(Tokens.PARAMETERS));
    }

    @Test
    public void getExtensionApiAsJsonHasApiElements() {
        this.mockery = new JUnit4Mockery();

        final ExtensionDescriptor extensionDescriptor = this.mockery.mock(ExtensionDescriptor.class);
        final ExtensionApi extensionApi = this.mockery.mock(ExtensionApi.class);

        this.mockery.checking(new Expectations() {{
            allowing(extensionDescriptor).description();
            will(returnValue("desc"));
            allowing(extensionDescriptor).api();
            will(returnValue(new ExtensionApi[] {extensionApi}));
            allowing(extensionApi).parameterName();
            will(returnValue("param1"));
            allowing(extensionApi).description();
            will(returnValue("value1"));
        }});

        ExtensionMethod extensionMethod = new ExtensionMethod(null, null, extensionDescriptor);
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
}

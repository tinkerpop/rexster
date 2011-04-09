package com.tinkerpop.rexster.extension;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import sun.security.provider.certpath.OCSPResponse;

import javax.ws.rs.core.Response;

public class ExtensionResponseTest {
    @Test(expected = IllegalArgumentException.class)
    public void overrideNull() {
        ExtensionResponse.override(null);
    }

    @Test
    public void overrideValid() {
        Response r = Response.noContent().build();
        ExtensionResponse er = ExtensionResponse.override(r);

        Assert.assertNotNull(er);
        Assert.assertFalse(er.isErrorResponse());
        Assert.assertEquals(r, er.getJerseyResponse());
    }

    @Test
    public void errorJustMessage() {
        ExtensionResponse er = ExtensionResponse.error("msg");

        Assert.assertNotNull(er);
        Assert.assertTrue(er.isErrorResponse());

        Response r = er.getJerseyResponse();
        Assert.assertNotNull(r);

        JSONObject entity = (JSONObject) r.getEntity();
        Assert.assertNotNull(entity);
        Assert.assertTrue(entity.has("message"));
        Assert.assertEquals("msg", entity.optString("message"));
    }

    @Test
    public void errorJustException() {
        ExtensionResponse er = ExtensionResponse.error(new Exception("err"));

        Assert.assertNotNull(er);
        Assert.assertTrue(er.isErrorResponse());

        Response r = er.getJerseyResponse();
        Assert.assertNotNull(r);
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());

        JSONObject entity = (JSONObject) r.getEntity();
        Assert.assertNotNull(entity);
        Assert.assertTrue(entity.has("message"));
        Assert.assertEquals("", entity.optString("message"));
        Assert.assertTrue(entity.has("error"));
        Assert.assertEquals("err", entity.optString("error"));
    }

    @Test
    public void errorMessageAndException() {
        ExtensionResponse er = ExtensionResponse.error("msg", new Exception("err"));

        Assert.assertNotNull(er);
        Assert.assertTrue(er.isErrorResponse());

        Response r = er.getJerseyResponse();
        Assert.assertNotNull(r);
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());

        JSONObject entity = (JSONObject) r.getEntity();
        Assert.assertNotNull(entity);
        Assert.assertTrue(entity.has("message"));
        Assert.assertEquals("msg", entity.optString("message"));
        Assert.assertTrue(entity.has("error"));
        Assert.assertEquals("err", entity.optString("error"));
    }

    @Test
    public void errorMessageExceptionAndCode() {
        ExtensionResponse er = ExtensionResponse.error("msg", new Exception("err"),
                Response.Status.BAD_REQUEST.getStatusCode());

        Assert.assertNotNull(er);
        Assert.assertTrue(er.isErrorResponse());

        Response r = er.getJerseyResponse();
        Assert.assertNotNull(r);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), r.getStatus());

        JSONObject entity = (JSONObject) r.getEntity();
        Assert.assertNotNull(entity);
        Assert.assertTrue(entity.has("message"));
        Assert.assertEquals("msg", entity.optString("message"));
        Assert.assertTrue(entity.has("error"));
        Assert.assertEquals("err", entity.optString("error"));
    }
}

package com.tinkerpop.rexster.extension;

import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory;
import com.tinkerpop.rexster.Tokens;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    @Test
    public void errorMessageExceptionAndCodeWithAddedJsonNoKey() {
        HashMap map = new HashMap() {{
            put("this", "that");
        }};

        ExtensionResponse er = ExtensionResponse.error("msg", new Exception("err"),
                Response.Status.BAD_REQUEST.getStatusCode(), null, new JSONObject(map));

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
        Assert.assertTrue(entity.has("this"));
        Assert.assertEquals("that", entity.optString("this"));
    }

    @Test
    public void errorMessageExceptionAndCodeWithAddedJsonWithKey() {

        final JSONObject deep = new JSONObject();
        try {
            deep.put("deepone", "foundme");
        } catch (Exception ex) {
            // never happen
        }

        HashMap map = new HashMap() {{
            put("this", "that");
            put("deep", deep);
        }};

        ExtensionResponse er = ExtensionResponse.error("msg", new Exception("err"),
                Response.Status.BAD_REQUEST.getStatusCode(), "keyitup", new JSONObject(map));

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
        Assert.assertTrue(entity.has("keyitup"));

        JSONObject bonusJson = entity.optJSONObject("keyitup");
        Assert.assertNotNull(bonusJson);
        Assert.assertTrue(bonusJson.has("this"));
        Assert.assertEquals("that", bonusJson.optString("this"));
        Assert.assertTrue(bonusJson.has("deep"));


        JSONObject deepJson = bonusJson.optJSONObject("deep");
        Assert.assertNotNull(bonusJson);
        Assert.assertTrue(deepJson.has("deepone"));
        Assert.assertEquals("foundme", deepJson.optString("deepone"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void okWithNullHashMap() {
        ExtensionResponse.ok((HashMap) null);
    }

    @Test
    public void okWithValidHashMap() {
        HashMap<String, String> map = new HashMap<String, String>() {{
            put("this", "that");
        }};

        ExtensionResponse er = ExtensionResponse.ok(map);

        Assert.assertNotNull(er);
        Assert.assertFalse(er.isErrorResponse());

        Response r = er.getJerseyResponse();
        Assert.assertNotNull(r);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());

        JSONObject entity = (JSONObject) r.getEntity();
        Assert.assertNotNull(entity);
        Assert.assertTrue(entity.has("this"));
        Assert.assertEquals("that", entity.optString("this"));
    }

    @Test
    public void okWithValidJson() {
        HashMap<String, String> map = new HashMap<String, String>() {{
            put("this", "that");
        }};

        JSONObject jsonObject = new JSONObject(map);

        ExtensionResponse er = ExtensionResponse.ok(jsonObject);

        Assert.assertNotNull(er);
        Assert.assertFalse(er.isErrorResponse());

        Response r = er.getJerseyResponse();
        Assert.assertNotNull(r);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());

        JSONObject entity = (JSONObject) r.getEntity();
        Assert.assertNotNull(entity);
        Assert.assertTrue(entity.has("this"));
        Assert.assertEquals("that", entity.optString("this"));
    }

    @Test
    public void okForGraphElement() {
        final Graph g = TinkerGraphFactory.createTinkerGraph();

        final ExtensionResponse er = ExtensionResponse.ok(g.getVertex(1));
        Assert.assertNotNull(er);
        Assert.assertFalse(er.isErrorResponse());

        final Response r = er.getJerseyResponse();
        Assert.assertNotNull(r);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());

        final JSONObject entity = (JSONObject) r.getEntity();
        Assert.assertNotNull(entity);
        Assert.assertEquals("marko", entity.optString("name"));
        Assert.assertEquals(29, entity.optInt("age"));
    }

    @Test
    public void okForGraphElements() {
        final Graph g = TinkerGraphFactory.createTinkerGraph();

        List<Element> elements = new ArrayList<Element>();
        elements.add(g.getVertex(1));
        elements.add(g.getVertex(2));

        final ExtensionResponse er = ExtensionResponse.ok(elements.iterator());
        Assert.assertNotNull(er);
        Assert.assertFalse(er.isErrorResponse());

        final Response r = er.getJerseyResponse();
        Assert.assertNotNull(r);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());

        final JSONObject entity = (JSONObject) r.getEntity();
        Assert.assertNotNull(entity);

        final JSONArray jsonArray = entity.optJSONArray(Tokens.RESULTS);
        Assert.assertEquals(2, jsonArray.length());
        Assert.assertEquals("marko", jsonArray.optJSONObject(0).optString("name"));
        Assert.assertEquals("vadas", jsonArray.optJSONObject(1).optString("name"));
    }

    @Test
    public void noContentValid() {
        ExtensionResponse er = ExtensionResponse.noContent();

        Assert.assertNotNull(er);
        Assert.assertFalse(er.isErrorResponse());

        Response r = er.getJerseyResponse();
        Assert.assertNotNull(r);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), r.getStatus());
    }
}

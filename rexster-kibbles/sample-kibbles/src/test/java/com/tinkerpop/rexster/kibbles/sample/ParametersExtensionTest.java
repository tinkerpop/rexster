package com.tinkerpop.rexster.kibbles.sample;

import com.tinkerpop.rexster.extension.ExtensionResponse;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;

public class ParametersExtensionTest {
    private ParametersExtension parametersExtension = new ParametersExtension();

    @Test
    public void evaluateSomeStringValid() {
        String replyToSend = "somestring";
        ExtensionResponse response = parametersExtension.evaluateSomeString(null, null, replyToSend);

        // the response should never be null
        Assert.assertNotNull(response);

        // the ExtensionResponse really just wraps an underlying jersey response and that
        // should not be null
        Response jerseyResponse = response.getJerseyResponse();
        Assert.assertNotNull(jerseyResponse);

        // the services return an OK status code.
        Assert.assertEquals(Response.Status.OK.getStatusCode(), jerseyResponse.getStatus());

        // JSON is wrapped in the jersey response.
        JSONObject json = (JSONObject) jerseyResponse.getEntity();
        Assert.assertNotNull(json);

        // the JSON has an output property and it contains the data from the toString call on the
        // requested element.
        Assert.assertTrue(json.has("some-string"));
        Assert.assertEquals(replyToSend, json.optString("some-string"));
    }

    @Test
    public void evaluateSomeIntegerValid() {
        Integer replyToSend = 100;
        ExtensionResponse response = parametersExtension.evaluateSomeInteger(null, null, replyToSend);

        // the response should never be null
        Assert.assertNotNull(response);

        // the ExtensionResponse really just wraps an underlying jersey response and that
        // should not be null
        Response jerseyResponse = response.getJerseyResponse();
        Assert.assertNotNull(jerseyResponse);

        // the services return an OK status code.
        Assert.assertEquals(Response.Status.OK.getStatusCode(), jerseyResponse.getStatus());

        // JSON is wrapped in the jersey response.
        JSONObject json = (JSONObject) jerseyResponse.getEntity();
        Assert.assertNotNull(json);

        // the JSON has an output property and it contains the data from the toString call on the
        // requested element.
        Assert.assertTrue(json.has("some-integer"));
        Assert.assertEquals(replyToSend.intValue(), json.optInt("some-integer"));
    }

    @Test
    public void evaluateSomeListValid() {
        JSONArray replyToSend = new JSONArray() {{
            put(1);
            put(2);
            put(3);
        }};

        ExtensionResponse response = parametersExtension.evaluateSomeList(null, null, replyToSend);

        // the response should never be null
        Assert.assertNotNull(response);

        // the ExtensionResponse really just wraps an underlying jersey response and that
        // should not be null
        Response jerseyResponse = response.getJerseyResponse();
        Assert.assertNotNull(jerseyResponse);

        // the services return an OK status code.
        Assert.assertEquals(Response.Status.OK.getStatusCode(), jerseyResponse.getStatus());

        // JSON is wrapped in the jersey response.
        JSONObject json = (JSONObject) jerseyResponse.getEntity();
        Assert.assertNotNull(json);

        // the JSON has an output property and it contains the data from the toString call on the
        // requested element.
        Assert.assertTrue(json.has("some-list"));
        Assert.assertEquals(replyToSend.length(), json.optJSONArray("some-list").length());
    }

    @Test
    public void evaluateSomeListRawValid() {
        String replyToSend = "[1,2,3,4]";

        ExtensionResponse response = parametersExtension.evaluateSomeListRaw(null, null, replyToSend);

        // the response should never be null
        Assert.assertNotNull(response);

        // the ExtensionResponse really just wraps an underlying jersey response and that
        // should not be null
        Response jerseyResponse = response.getJerseyResponse();
        Assert.assertNotNull(jerseyResponse);

        // the services return an OK status code.
        Assert.assertEquals(Response.Status.OK.getStatusCode(), jerseyResponse.getStatus());

        // JSON is wrapped in the jersey response.
        JSONObject json = (JSONObject) jerseyResponse.getEntity();
        Assert.assertNotNull(json);

        // the JSON has an output property and it contains the data from the toString call on the
        // requested element.
        Assert.assertTrue(json.has("some-list"));
        Assert.assertEquals(replyToSend, json.optString("some-list"));
    }

    @Test
    public void evaluateSomeObjectValid() {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        try {
            jsonObject.put("a", "test");
            jsonObject.put("b", 101);

            jsonArray.put(1);
            jsonArray.put(2);
            jsonArray.put(3);

        } catch (Exception ex) {
            Assert.fail();
        }

        ExtensionResponse response = parametersExtension.evaluateSomeObject(null, null, 100, jsonObject, jsonArray);

        // the response should never be null
        Assert.assertNotNull(response);

        // the ExtensionResponse really just wraps an underlying jersey response and that
        // should not be null
        Response jerseyResponse = response.getJerseyResponse();
        Assert.assertNotNull(jerseyResponse);

        // the services return an OK status code.
        Assert.assertEquals(Response.Status.OK.getStatusCode(), jerseyResponse.getStatus());

        // JSON is wrapped in the jersey response.
        JSONObject json = (JSONObject) jerseyResponse.getEntity();
        Assert.assertNotNull(json);

        // the JSON has an output property and it contains the data from the toString call on the
        // requested element.
        Assert.assertTrue(json.has("a"));
        Assert.assertEquals(100, json.optInt("a"));

        JSONObject innerToAssert = json.optJSONObject("b");
        Assert.assertEquals("test", innerToAssert.optString("a"));
        Assert.assertEquals(101, innerToAssert.optInt("b"));

        Assert.assertEquals(3, json.optJSONArray("c").length());
    }
}

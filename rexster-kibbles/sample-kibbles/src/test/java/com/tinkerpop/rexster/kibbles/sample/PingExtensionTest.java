package com.tinkerpop.rexster.kibbles.sample;

import com.tinkerpop.rexster.extension.ExtensionResponse;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;

public class PingExtensionTest {
    private PingExtension pingExtension = new PingExtension();

    @Test
    public void evaluatePingValid() {
        String replyToSend = "pong";
        ExtensionResponse response = pingExtension.evaluatePing(null, null, replyToSend);

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
        Assert.assertTrue(json.has("ping"));
        Assert.assertEquals(replyToSend, json.optString("ping"));
    }
}

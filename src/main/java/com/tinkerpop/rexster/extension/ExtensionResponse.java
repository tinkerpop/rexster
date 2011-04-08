package com.tinkerpop.rexster.extension;

import javax.ws.rs.core.Response;

public class ExtensionResponse {

    private Response jerseyResponse;

    public ExtensionResponse(Response response) {
        this.jerseyResponse = response;
    }

    public static ExtensionResponse override(Response response) {
        if (response == null) {
            throw new IllegalArgumentException("Response cannot be null");
        }

        return new ExtensionResponse(response);
    }

    public Response getJerseyResponse() {
        return this.jerseyResponse;
    }
}

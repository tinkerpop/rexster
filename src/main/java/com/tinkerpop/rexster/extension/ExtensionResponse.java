package com.tinkerpop.rexster.extension;

import com.tinkerpop.rexster.Tokens;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Wraps the Jersey response object with some simple response builder methods.
 */
public class ExtensionResponse {

    private Response jerseyResponse;
    private boolean errorResponse;

    /**
     * Create a non-error ExtensionResponse object.
     */
    public ExtensionResponse(Response response) {
        this(response, false);
    }

    /**
     * Create an ExtensionResponse object.
     */
    public ExtensionResponse(Response response, boolean errorResponse) {
        this.jerseyResponse = response;
        this.errorResponse = errorResponse;
    }

    /**
     * Override the builder and literally construct the Jersey response.
     *
     * Rexster will add its standard headers and override any provided in the response.  It is recommended
     * to use the @see error methods as opposed to override if the intention is to return an error on
     * the response.  The override methods will not throw a WebApplicationException or do any standard
     * Rexster server side logging.
     */
    public static ExtensionResponse override(Response response) {
        if (response == null) {
            throw new IllegalArgumentException("Response cannot be null");
        }

        return new ExtensionResponse(response);
    }

    /**
     * Generates standard Rexster JSON error with an internal server error response code.
     */
    public static ExtensionResponse error(String message) {
        return error(message, null);
    }

    /**
    * Generates standard Rexster JSON error with an internal server error response code.
    */
    public static ExtensionResponse error(Exception source) {
        return error("", source);
    }

    /**
     * Generates standard Rexster JSON error with an internal server error response code.
     */
    public static ExtensionResponse error(String message, Exception source) {
        return error(message, source, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    /**
     * Generates standard Rexster JSON error with a specified server error response code.
     *
     * The status code is not validated, so throw the right code.
     */
    public static ExtensionResponse error(String message, Exception source, int statusCode) {
        Map<String, String> m = new HashMap<String, String>();
        m.put(Tokens.MESSAGE, message);

        if (source != null) {
            m.put("error", source.getMessage());
        }

        // use a hashmap with the constructor so that a JSONException
        // will not be thrown
        return new ExtensionResponse(Response.status(statusCode).entity(new JSONObject(m)).build(), true);
    }

    public Response getJerseyResponse() {
        return this.jerseyResponse;
    }

    public boolean isErrorResponse() {
        return this.errorResponse;
    }
}

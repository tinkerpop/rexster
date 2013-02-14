package com.tinkerpop.rexster.kibbles.sample;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionMethod;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionRequestParameter;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.RexsterContext;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a simple extension that shows how different parameters can be used by an extension.
 */
@ExtensionNaming(namespace = AbstractSampleExtension.EXTENSION_NAMESPACE, name = ParametersExtension.EXTENSION_NAME)
public class ParametersExtension extends AbstractSampleExtension {
    public static final String EXTENSION_NAME = "parameters";

    /**
     * http://localhost/graphs/tinkergraph/tp-sample/parameters/string?some-string=test
     */
    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "string")
    @ExtensionDescriptor(description = "pass a string parameter to be used in the response.")
    public ExtensionResponse evaluateSomeString(@RexsterContext RexsterResourceContext context,
                                                @RexsterContext Graph graph,
                                                @ExtensionRequestParameter(name = "some-string", description = "a string to reply with") String reply) {
        if (reply == null || reply.isEmpty()) {
            ExtensionMethod extMethod = context.getExtensionMethod();
            return ExtensionResponse.error(
                    "the some-string parameter cannot be empty",
                    null,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    null,
                    generateErrorJson(extMethod.getExtensionApiAsJson()));
        }

        Map<String, String> map = new HashMap<String, String>();
        map.put("some-string", reply);
        return ExtensionResponse.ok(map);
    }

    /**
     * http://localhost/graphs/tinkergraph/tp-sample/parameters/integer?some-integer=1
     */
    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "integer")
    @ExtensionDescriptor(description = "pass an integer parameter to be used in the response.")
    public ExtensionResponse evaluateSomeInteger(@RexsterContext RexsterResourceContext context,
                                                 @RexsterContext Graph graph,
                                                 @ExtensionRequestParameter(name = "some-integer", description = "an integer to reply with") Integer reply) {
        if (reply == null) {
            ExtensionMethod extMethod = context.getExtensionMethod();
            return ExtensionResponse.error(
                    "the some-integer parameter cannot be empty",
                    null,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    null,
                    generateErrorJson(extMethod.getExtensionApiAsJson()));
        }

        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("some-integer", reply.intValue());
        return ExtensionResponse.ok(map);
    }

    /**
     * http://localhost/graphs/tinkergraph/tp-sample/parameters/float?some-float=test
     */
    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "float")
    @ExtensionDescriptor(description = "pass a string parameter to be used in the response.")
    public ExtensionResponse evaluateSomeFloat(@RexsterContext RexsterResourceContext context,
                                               @RexsterContext Graph graph,
                                               @ExtensionRequestParameter(name = "some-float", description = "a float to reply with") Float reply) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("some-float", reply.toString());
        return ExtensionResponse.ok(map);
    }

    /**
     * Lists are parsed to JSONArray from the URI when passed as
     * <p/>
     * http://localhost/graphs/tinkergraph/tp-sample/parameters/list?some-list=[1,2,3]
     */
    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "list")
    @ExtensionDescriptor(description = "pass a list parameter to be used in the response.")
    public ExtensionResponse evaluateSomeList(@RexsterContext RexsterResourceContext context,
                                              @RexsterContext Graph graph,
                                              @ExtensionRequestParameter(name = "some-list", description = "a list to reply with") JSONArray reply) {
        if (reply == null) {
            ExtensionMethod extMethod = context.getExtensionMethod();
            return ExtensionResponse.error(
                    "the some-integer parameter cannot be empty",
                    null,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    null,
                    generateErrorJson(extMethod.getExtensionApiAsJson()));
        }

        Map<String, JSONArray> map = new HashMap<String, JSONArray>();
        map.put("some-list", reply);
        return ExtensionResponse.ok(map);
    }

    /**
     * To pass a string value that contains square brackets set parseToJson = false
     * <p/>
     * http://localhost/graphs/tinkergraph/tp-sample/parameters/list-raw?some-list=[1,2,3]
     * <p/>
     * In this case, the data type is not a JSONArray but a String.  The process of mapping a URI to JSON
     * is not performed.
     */
    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "list-raw")
    @ExtensionDescriptor(description = "pass a square bracket enclosed string parameter to be used in the response.")
    public ExtensionResponse evaluateSomeListRaw(@RexsterContext RexsterResourceContext context,
                                                 @RexsterContext Graph graph,
                                                 @ExtensionRequestParameter(name = "some-list", description = "a list to reply with", parseToJson = false) String reply) {
        if (reply == null || reply.isEmpty()) {
            ExtensionMethod extMethod = context.getExtensionMethod();
            return ExtensionResponse.error(
                    "the some-list parameter cannot be empty",
                    null,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    null,
                    generateErrorJson(extMethod.getExtensionApiAsJson()));
        }

        Map<String, String> map = new HashMap<String, String>();
        map.put("some-list", reply);
        return ExtensionResponse.ok(map);
    }

    /**
     * Accessing:
     * <p/>
     * http://localhost:8182/graphs/tinkergraph/tp-sample/parameters/object?a=1&b.a=marko&b.b=true&b.c.a=peter&c=[marko,povel]
     * <p/>
     * would yield three parameters that could be injected to this method: an integer for "a",
     * a JSONObject for "b" and a JSONArray for "c".
     */
    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "object")
    @ExtensionDescriptor(description = "pass an object parameter to be used in the response.")
    public ExtensionResponse evaluateSomeObject(@RexsterContext RexsterResourceContext context,
                                                @RexsterContext Graph graph,
                                                @ExtensionRequestParameter(name = "a", description = "an integer to reply with") Integer reply,
                                                @ExtensionRequestParameter(name = "b", description = "an object to reply with") JSONObject replyObject,
                                                @ExtensionRequestParameter(name = "c", description = "a list to reply with") JSONArray replyList) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("a", reply);
        map.put("b", replyObject);
        map.put("c", replyList);
        return ExtensionResponse.ok(map);
    }

    /**
     * Accessing:
     * <p/>
     * http://localhost:8182/graphs/tinkergraph/tp-sample/parameters/object
     * <p/>
     * would yield three parameters that could be injected to this method: an integer for "a",
     * a JSONObject for "b" and a JSONArray for "c".
     */
    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "object-default")
    @ExtensionDescriptor(description = "pass an object parameter to be used in the response.")
    public ExtensionResponse evaluateSomeObjectWithDefaults(@RexsterContext RexsterResourceContext context,
                                                            @RexsterContext Graph graph,
                                                            @ExtensionRequestParameter(name = "a", defaultValue = "1", description = "an integer to reply with") Integer reply,
                                                            @ExtensionRequestParameter(name = "b", defaultValue = "{\"a\":\"marko\",\"b\":true, \"c\": {\"a\":\"peter\"}}", description = "an object to reply with") JSONObject replyObject,
                                                            @ExtensionRequestParameter(name = "c", defaultValue = "[\"marko\",\"povel\"]", description = "a list to reply with") JSONArray replyList) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("a", reply);
        map.put("b", replyObject);
        map.put("c", replyList);
        return ExtensionResponse.ok(map);
    }
}

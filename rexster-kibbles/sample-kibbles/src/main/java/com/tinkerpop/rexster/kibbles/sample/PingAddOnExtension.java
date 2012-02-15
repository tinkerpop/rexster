package com.tinkerpop.rexster.kibbles.sample;


import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionMethod;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionRequestParameter;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.RexsterContext;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple extension that just echoes back the string parameter passed in, but further demonstrates
 * that an extension class can share a name within the same namespace with another class (in this case
 * the standard PingExtension).  In other words, extensions can span multiple classes within the same
 * namespace and name.
 */
@ExtensionNaming(namespace = AbstractSampleExtension.EXTENSION_NAMESPACE, name = PingExtension.EXTENSION_NAME)
public class PingAddOnExtension extends AbstractSampleExtension {
    public static final String EXTENSION_NAME = "ping";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "pong")
    @ExtensionDescriptor(description = "a simple ping extension.")
    public ExtensionResponse evaluatePing(@RexsterContext RexsterResourceContext context,
                                          @RexsterContext Graph graph,
                                          @ExtensionRequestParameter(name = "reply", description = "a value to reply with") String reply) {
        if (reply == null || reply.isEmpty()) {
            ExtensionMethod extMethod = context.getExtensionMethod();
            return ExtensionResponse.error(
                    "the reply parameter cannot be empty",
                    null,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    null,
                    generateErrorJson(extMethod.getExtensionApiAsJson()));
        }

        Map<String, String> map = new HashMap<String, String>();
        map.put("ping-add-on", reply);
        return ExtensionResponse.ok(map);
    }
}

package com.tinkerpop.rexster.kibbles.sample;

import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.RexsterContext;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * An extension that demostrates how to get a Principal from the SecurityContext.
 */
@ExtensionNaming(name = SecureExtension.EXTENSION_NAME, namespace = AbstractSampleExtension.EXTENSION_NAMESPACE)
public class SecureExtension extends AbstractSampleExtension {
    public static final String EXTENSION_NAME = "secure";

    /**
     * ttp://localhost:8182/graphs/tinkergraph/vertices/1/tp-sample/secure
     */
    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH)
    @ExtensionDescriptor(description = "shows if security is on or off and who the user is if it is on.")
    public ExtensionResponse doSomethingIfSecure(@RexsterContext SecurityContext securityContext) {

        final Map<String, String> map = new HashMap<String, String>();
        final Principal principal = securityContext.getUserPrincipal();

        // if <security><authetication><type> is set to "none" in rexster.xml the principal will
        // return as null
        if (principal == null) {
            map.put("security", "authentication off");
            map.put("user", "<none>");
        } else {
            map.put("security", "authentication on");
            map.put("user", principal.getName());
        }

        return ExtensionResponse.ok(map);

    }
}

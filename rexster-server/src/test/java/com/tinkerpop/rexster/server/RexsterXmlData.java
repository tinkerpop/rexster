package com.tinkerpop.rexster.server;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public final class RexsterXmlData {
    public static final String XML =
            "<rexster>\n" +
            "    <rexster-server-port>8182</rexster-server-port>\n" +
            "    <rexster-server-host>0.0.0.0</rexster-server-host>\n" +
            "    <rexster-shutdown-port>8183</rexster-shutdown-port>\n" +
            "    <rexster-shutdown-host>127.0.0.1</rexster-shutdown-host>\n" +
            "    <rexpro-server-port>8184</rexpro-server-port>\n" +
            "    <rexpro-server-host>0.0.0.0</rexpro-server-host>\n" +
            "    <rexpro-session-max-idle>1790000</rexpro-session-max-idle>\n" +
            "    <rexpro-session-check-interval>3000000</rexpro-session-check-interval>\n" +
            "    <base-uri>http://localhost</base-uri>\n" +
            "    <web-root>public</web-root>\n" +
            "    <character-set>UTF-8</character-set>\n" +
            "    <security>\n" +
            "        <authentication>\n" +
            "            <type>none</type>\n" +
            "            <configuration>\n" +
            "                <users>\n" +
            "                    <user>\n" +
            "                        <username>rexster</username>\n" +
            "                        <password>rexster</password>\n" +
            "                    </user>\n" +
            "                </users>\n" +
            "            </configuration>\n" +
            "        </authentication>\n" +
            "    </security>\n" +
            "    <graphs>\n" +
            "        <graph>\n" +
            "            <graph-name>emptygraph</graph-name>\n" +
            "            <graph-type>tinkergraph</graph-type>\n" +
            "            <extensions>\n" +
            "                <allows>\n" +
            "                    <allow>tp:gremlin</allow>\n" +
            "                </allows>\n" +
            "            </extensions>\n" +
            "        </graph>\n" +
            "        <graph>\n" +
            "            <graph-name>tinkergraph</graph-name>\n" +
            "            <graph-type>tinkergraph</graph-type>\n" +
            "            <extensions>\n" +
            "                <allows>\n" +
            "                    <allow>tp:gremlin</allow>\n" +
            "                </allows>\n" +
            "            </extensions>\n" +
            "        </graph>" +
            "   </graphs>" +
            "</rexster>";

}

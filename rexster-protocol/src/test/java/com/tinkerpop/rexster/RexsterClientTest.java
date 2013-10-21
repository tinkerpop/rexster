package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.rexster.client.DistributedRexsterClient;
import com.tinkerpop.rexster.client.RexsterClientFactory;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexsterClientTest {
    @Test
    @Ignore
    public void justTrying() throws Exception {
        DistributedRexsterClient client = RexsterClientFactory.openDistributed(null);
        while (true) {
            Thread.sleep(1000);

            Map<String, Object> bindings = new HashMap<String, Object>();
            bindings.put("x", 1);
            List l = client.execute("g=rexster.getGraph('tinkergraph');g.v(x)", bindings, Vertex.class, 1l);
            for (Object i : l) {
                System.out.println(i);
            }
            System.out.println("worked..........next");
        }
    }
}

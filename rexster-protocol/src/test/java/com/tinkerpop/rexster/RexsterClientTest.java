package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.rexster.client.HintedRexsterClient;
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
        HintedRexsterClient client = RexsterClientFactory.openHinted(null);
        for (int ix = 0; ix < 10; ix++) {
            Thread.sleep(1000);

            final HintedRexsterClient.Hint hint = new HintedRexsterClient.Hint(Vertex.class, 1l, "tinkergraph");

            Map<String, Object> bindings = new HashMap<String, Object>();
            bindings.put("x", 1);
            List l = client.execute("g=rexster.getGraph('tinkergraph');g.v(x)", bindings, hint);
            for (Object i : l) {
                System.out.println(i);
            }
            System.out.println("worked..........next");
        }

        client.close();
    }
}

package com.tinkerpop.rexster.rexpro;

import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.client.RexsterClient;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public abstract class AbstractRexsterClientIntegrationTest extends AbstractRexProIntegrationTest {

    /**
     * Indicates that the serializer supports primitive
     * map key types, not just strings
     * @return
     */
    public abstract boolean supportsPrimitiveKeys();

    @Test
    public void shouldOpenAndCloseLotsOfClients() throws Exception {
        final int numberOfClientsToOpen = 100;
        for (int ix = 0; ix < numberOfClientsToOpen; ix++) {
            final RexsterClient client = getClient();
            // have to send a script to open a connection
            assertEquals(2l, client.execute("1+1").get(0));
            client.close();
        }
    }

    @Test
    public void shouldOpenAndCloseLotsOfClientsInManyThreads() throws Exception {
        final int numberOfClientsToOpen = 1000;
        final AtomicBoolean fail = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(numberOfClientsToOpen);
        for (int ix = 0; ix < numberOfClientsToOpen; ix++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final RexsterClient client = getClient();
                        // have to send a script to open a connection
                        client.execute("Thread.sleep(50);1+1");
                        client.close();
                    } catch (Exception ex) {
                        fail.set(true);
                    } finally {
                        latch.countDown();
                    }
                }
            }).run();
        }

        latch.await();
        assertFalse(fail.get());
    }

    @Test
    public void shouldCloseWhileOtherClientIsOpen() throws Exception {
        final RexsterClient client1 = getClient();
        final RexsterClient client2 = getClient();
        final RexsterClient client3 = getClient();

        final AtomicBoolean fail2 = new AtomicBoolean(false);
        final AtomicBoolean fail3 = new AtomicBoolean(false);
        final AtomicBoolean result2 = new AtomicBoolean(false);
        final AtomicBoolean result3 = new AtomicBoolean(false);

        // send client 2 into a long run operation
        final Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    result2.set((Boolean) client2.execute("Thread.sleep(5000);true").get(0));
                } catch (Exception ex) {
                    fail2.set(true);
                    ex.printStackTrace();
                }
            }
        });

        // make client 3 do a bunch of stuff that runs at least as long at client 2
        final Thread t3 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int ix = 0; ix < 100; ix++) {
                        result3.set((Boolean) client3.execute("Thread.sleep(50);true").get(0));
                        if (!result3.get())
                            break;
                    }
                } catch (Exception ex) {
                    fail3.set(true);
                    ex.printStackTrace();
                }
            }
        });

        // close client1 while client 2 + 3 are still busy
        t3.run();
        t2.run();
        client1.close();

        // wait for thread doing client 2 work to complete
        t2.join();
        t3.join();

        assertTrue(result2.get());
        assertTrue(result3.get());
        assertFalse(fail2.get());
        assertFalse(fail3.get());

        client3.close();

        // client 2 should still be valid
        final List<Map<String, Object>> vertexResults = client2.execute("g=TinkerGraphFactory.createTinkerGraph();g.v(1)", null);
        assertEquals(1, vertexResults.size());
        client2.close();
    }

    @Test
    public void executeExercise() throws Exception {
        final RexsterClient client = getClient();

        final List<Object> nullResults = client.execute("null");
        assertEquals(1, nullResults.size());
        final Object nullResult = nullResults.get(0);
        assertEquals(null, nullResult);

        final List<Map<String, Object>> mapResults = client.execute("[val:1+1]");
        assertEquals(1, mapResults.size());
        final Map<String, Object> mapResult = mapResults.get(0);
        assertEquals("2", mapResult.get("val").toString());

        final List<Long> intResults = client.execute("1+1", null);
        assertEquals(1, intResults.size());
        final Long intResult = intResults.get(0);
        assertEquals("2", intResult.toString());

        final List<Map<String, Object>> vertexResults = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.v(1)", null);
        assertEquals(1, vertexResults.size());
        final Map<String, Object> vertexResult = vertexResults.get(0);
        assertEquals("vertex", vertexResult.get("_type").toString());
        assertEquals("1", vertexResult.get("_id").toString());

        final Map<String, Object> vertexProperties = (Map<String, Object>) vertexResult.get("_properties");
        assertEquals("marko", vertexProperties.get("name"));
        assertEquals(29L, vertexProperties.get("age"));

        client.close();
    }

    @Test
    public void executeMapValueConversion() throws Exception {
        final RexsterClient client = getClient();

        // all whole numerics convert to long
        // all float go to double
        final List<Map<String, Object>> mapResultsObject = client.execute("[n:1+1,b:true,f:1234.56f,s:'string',a:[1,2,3],m:[one:1]]");
        assertEquals(1, mapResultsObject.size());
        final Map<String, Object> mapResultObject = mapResultsObject.get(0);
        assertEquals(2L, mapResultObject.get("n"));
        assertEquals(true, mapResultObject.get("b"));
        assertEquals(1234.56d, (Double) mapResultObject.get("f"), 0.001d);
        assertEquals("string", mapResultObject.get("s"));
        assertEquals(3L, ((ArrayList) mapResultObject.get("a")).size());
        assertEquals(1L, ((Map) mapResultObject.get("m")).get("one"));

        client.close();
    }

    @Test
    public void executeReturnGraphElementsValueConversion() throws Exception {
        final RexsterClient client = getClient();

        final List<Map<String, Object>> vertexResults = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.v(1)");
        assertEquals(1, vertexResults.size());
        final Map<String, Object> vertexResult = vertexResults.get(0);
        assertEquals("vertex", vertexResult.get("_type"));
        assertEquals("1", vertexResult.get("_id"));
        final Map vertexProperties = (Map) vertexResult.get("_properties");
        assertEquals("marko", vertexProperties.get("name"));
        assertEquals(29L, vertexProperties.get("age"));

        client.close();
    }

    @Test
    public void executeReturnGraphElementsAsMapValueConversion() throws Exception {
        final RexsterClient client = getClient();

        final List<Map<String, Object>> vertexResults = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.v(1).map");
        assertEquals(1, vertexResults.size());
        final Map<String, Object> vertexResult = vertexResults.get(0);
        assertEquals("marko", vertexResult.get("name"));
        assertEquals(29L, vertexResult.get("age"));

        client.close();
    }

    @Test
    public void executeReturnGraphElementsAsSelectValueConversion() throws Exception {
        final RexsterClient client = getClient();

        final List<Map<String,Object>> vertexResults = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.v(1).as('a').out.as('b').select{it.name}{it.age}");
        assertEquals(3, vertexResults.size());
        assertEquals("marko", vertexResults.get(0).get("a"));
        assertEquals(27L, vertexResults.get(0).get("b"));
        assertEquals("marko", vertexResults.get(1).get("a"));
        assertEquals(32L, vertexResults.get(1).get("b"));
        assertEquals("marko", vertexResults.get(2).get("a"));
        Assert.assertNull(vertexResults.get(2).get("b"));

        client.close();
    }

    /*
    @Test
    public void executeTransactionWithAutoCommitInSessionlessMode() throws Exception {
        final Configuration conf = new BaseConfiguration();
        conf.setProperty(RexsterClientTokens.CONFIG_GRAPH_NAME, "neo4jsample");
        final RexsterClient rexsterClientToNeo4j = RexsterClientFactory.open(conf);

        final List<Object> results = rexsterClientToNeo4j.execute("g.addVertex([name:n])",
                new HashMap<String,Object>() {{
                    put("n","vadas");
                }});
        Assert.assertEquals(1, results.size());

        final List<Map<String,Object>> txResults = rexsterClientToNeo4j.execute("g.V");
        Assert.assertEquals(1, txResults.size());
        final Map<String, Object> vertexResult = txResults.get(0);
        Assert.assertEquals("vadas", ((Map<String,Object>)vertexResult.get("_properties")).get("name"));

        rexsterClientToNeo4j.close();

    }
    */

    @Test
    public void executeAndReturnMapWithGraphElementKey() throws Exception {
        // maps of graph element keys get serialized to nested maps like:
        // {elementId : { _element : 1, _contents : { standard vertex/edge serialization } }
        final RexsterClient client = getClient();

        final List<Map<String, Map<String,Object>>> vertexResults = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.V.out.groupCount.cap");
        assertEquals(1, vertexResults.size());

        final Map<String, Map<String,Object>> r = vertexResults.get(0);
        assertEquals(3L, r.get("3").get(Tokens._VALUE));
        assertEquals(1L, r.get("2").get(Tokens._VALUE));
        assertEquals(1L, r.get("5").get(Tokens._VALUE));
        assertEquals(1L, r.get("4").get(Tokens._VALUE));

        client.close();

    }

    @Test
    public void executeAndReturnMapWithPrimitiveKey() throws Exception {
        if (!supportsPrimitiveKeys()) return;
        final RexsterClient client = getClient();

        final List<Map<Integer, String>> vertexResults = client.execute("[1:'test']");
        assertEquals(1, vertexResults.size());

        final Map<Integer, String> r = vertexResults.get(0);
        assertEquals("test", r.get(1L));

        client.close();

    }

    @Test
    public void executeAndReturnTree() throws Exception {
        final RexsterClient client = getClient();

        final List<Object> treeResults = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.V.out.tree.cap");
        assertEquals(1, treeResults.size());

        assertTrue(treeResults.get(0) instanceof Map);
        final HashMap<String, Object> map = (HashMap<String, Object>) treeResults.get(0);

        for(Map.Entry e : map.entrySet()) {
            assertTrue(e.getValue() instanceof Map);
            Map m = (Map) e.getValue();
            assertTrue(m.containsKey(Tokens._KEY));
            assertTrue(m.containsKey(Tokens._VALUE));
        }

        client.close();

    }

    @Test
    public void executeAndReturnSelect() throws Exception {
        final RexsterClient client = getClient();

        final List<Object> selectResults = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.v(1).out.name.as('x').select");
        assertEquals(3, selectResults.size());

        assertTrue(selectResults.get(0) instanceof Map);

        final List<String> names = new ArrayList<String>(){{
            add("vadas");
            add("josh");
            add("lop");
        }};

        for(Object e : selectResults) {
            final Map<String,Object> m = (Map<String, Object>) e;
            assertTrue(names.contains(m.get("x")));
        }

        client.close();

    }

    @Test
    public void executeAndReturnTable() throws Exception {
        final RexsterClient client = getClient();

        final List<Object> tableResults = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.v(1).out.name.as('x').table.cap.next()");
        assertEquals(3, tableResults.size());

        assertTrue(tableResults.get(0) instanceof Map);

        final List<String> names = new ArrayList<String>(){{
            add("vadas");
            add("josh");
            add("lop");
        }};

        for(Object e : tableResults) {
            final Map<String,Object> m = (Map<String, Object>) e;
            assertTrue(names.contains(m.get("x")));
        }

        client.close();

    }

    @Test
    public void executeForProperties() throws Exception {
        final RexsterClient client = getClient();

        final List<Map> stuffs = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.v(1).properties");
        assertEquals(1, stuffs.size());

        Map<String,Object> m = stuffs.get(0);

        assertEquals("class com.tinkerpop.blueprints.impls.tg.TinkerVertex", m.get("class"));
        assertEquals("1", m.get("id"));

        List<String> k = (List<String>) m.get("propertyKeys");
        assertEquals(2, k.size());
        Collections.sort(k);

        assertEquals("age", k.get(0));
        assertEquals("name", k.get(1));

    }

    @Test
    public void executeForTextWithBreaks() throws Exception {
        final RexsterClient client = getClient();

        // note that you have to escape the \r\n for it to be understood as a property value else the script engine
        // assumes it is line breaks in the script itself.
        final List<String> text = client.execute("g=new TinkerGraph();g.addVertex(['text':'''test1\\r\\ntest2\\r\\ntest3''']);g.v(0).text");

        assertEquals(1, text.size());
        assertEquals("test1\r\ntest2\r\ntest3", text.get(0));

    }

    /* this test fails on neo4j given inconsistencies in its blueprints implementation.  a failing test
       was added to blueprints here:
       https://github.com/tinkerpop/blueprints/issues/363
    public void executeTransactionWithNoAutoCommitInSessionlessMode() throws Exception {
        final Configuration conf = new BaseConfiguration();
        conf.setProperty(RexsterClientTokens.CONFIG_GRAPH_NAME, "orientdbsample");
        conf.setProperty(RexsterClientTokens.CONFIG_TRANSACTION, false);
        final RexsterClient rexsterClientToNeo4j = RexsterClientFactory.open(conf);

        final List<Object> results = rexsterClientToNeo4j.execute("g.addVertex([name:n])",
                new HashMap<String,Object>() {{
                    put("n","vadas");
                }});
        Assert.assertEquals(1, results.size());

        final List<Object> txResults = rexsterClientToNeo4j.execute("g.V");
        Assert.assertEquals(1, txResults.size());
        Assert.assertNull(txResults.get(0));

    }
    */
}

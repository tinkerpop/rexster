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
            client.close();
        }
    }

    @Test
    public void executeExercise() throws Exception {
        final RexsterClient client = getClient();

        final List<Map<String, Object>> mapResults = client.execute("[val:1+1]");
        Assert.assertEquals(1, mapResults.size());
        final Map<String, Object> mapResult = mapResults.get(0);
        Assert.assertEquals("2", mapResult.get("val").toString());

        final List<Long> intResults = client.execute("1+1", null);
        Assert.assertEquals(1, intResults.size());
        final Long intResult = intResults.get(0);
        Assert.assertEquals("2", intResult.toString());

        final List<Map<String, Object>> vertexResults = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.v(1)", null);
        Assert.assertEquals(1, vertexResults.size());
        final Map<String, Object> vertexResult = vertexResults.get(0);
        Assert.assertEquals("vertex", vertexResult.get("_type").toString());
        Assert.assertEquals("1", vertexResult.get("_id").toString());

        final Map<String, Object> vertexProperties = (Map<String, Object>) vertexResult.get("_properties");
        Assert.assertEquals("marko", vertexProperties.get("name"));
        Assert.assertEquals(29L, vertexProperties.get("age"));

        client.close();
    }

    @Test
    public void executeMapValueConversion() throws Exception {
        final RexsterClient client = getClient();

        // all whole numerics convert to long
        // all float go to double
        final List<Map<String, Object>> mapResultsObject = client.execute("[n:1+1,b:true,f:1234.56f,s:'string',a:[1,2,3],m:[one:1]]");
        Assert.assertEquals(1, mapResultsObject.size());
        final Map<String, Object> mapResultObject = mapResultsObject.get(0);
        Assert.assertEquals(2L, mapResultObject.get("n"));
        Assert.assertEquals(true, mapResultObject.get("b"));
        Assert.assertEquals(1234.56d, (Double) mapResultObject.get("f"), 0.001d);
        Assert.assertEquals("string", mapResultObject.get("s"));
        Assert.assertEquals(3L, ((ArrayList) mapResultObject.get("a")).size());
        Assert.assertEquals(1L, ((Map) mapResultObject.get("m")).get("one"));

        client.close();
    }

    @Test
    public void executeReturnGraphElementsValueConversion() throws Exception {
        final RexsterClient client = getClient();

        final List<Map<String, Object>> vertexResults = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.v(1)");
        Assert.assertEquals(1, vertexResults.size());
        final Map<String, Object> vertexResult = vertexResults.get(0);
        Assert.assertEquals("vertex", vertexResult.get("_type"));
        Assert.assertEquals("1", vertexResult.get("_id"));
        final Map vertexProperties = (Map) vertexResult.get("_properties");
        Assert.assertEquals("marko", vertexProperties.get("name"));
        Assert.assertEquals(29L, vertexProperties.get("age"));

        client.close();
    }

    @Test
    public void executeReturnGraphElementsAsMapValueConversion() throws Exception {
        final RexsterClient client = getClient();

        final List<Map<String, Object>> vertexResults = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.v(1).map");
        Assert.assertEquals(1, vertexResults.size());
        final Map<String, Object> vertexResult = vertexResults.get(0);
        Assert.assertEquals("marko", vertexResult.get("name"));
        Assert.assertEquals(29L, vertexResult.get("age"));

        client.close();
    }

    @Test
    public void executeReturnGraphElementsAsSelectValueConversion() throws Exception {
        final RexsterClient client = getClient();

        final List<Map<String,Object>> vertexResults = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.v(1).as('a').out.as('b').select{it.name}{it.age}");
        Assert.assertEquals(3, vertexResults.size());
        Assert.assertEquals("marko", vertexResults.get(0).get("a"));
        Assert.assertEquals(27L, vertexResults.get(0).get("b"));
        Assert.assertEquals("marko", vertexResults.get(1).get("a"));
        Assert.assertEquals(32L, vertexResults.get(1).get("b"));
        Assert.assertEquals("marko", vertexResults.get(2).get("a"));
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
        Assert.assertEquals(1, vertexResults.size());

        final Map<String, Map<String,Object>> r = vertexResults.get(0);
        Assert.assertEquals(3L, r.get("3").get(Tokens._VALUE));
        Assert.assertEquals(1L, r.get("2").get(Tokens._VALUE));
        Assert.assertEquals(1L, r.get("5").get(Tokens._VALUE));
        Assert.assertEquals(1L, r.get("4").get(Tokens._VALUE));

        client.close();

    }

    @Test
    public void executeAndReturnMapWithPrimitiveKey() throws Exception {
        if (!supportsPrimitiveKeys()) return;
        final RexsterClient client = getClient();

        final List<Map<Integer, String>> vertexResults = client.execute("[1:'test']");
        Assert.assertEquals(1, vertexResults.size());

        final Map<Integer, String> r = vertexResults.get(0);
        Assert.assertEquals("test", r.get(1L));

        client.close();

    }

    @Test
    public void executeAndReturnTree() throws Exception {
        final RexsterClient client = getClient();

        final List<Object> treeResults = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.V.out.tree.cap");
        Assert.assertEquals(1, treeResults.size());

        Assert.assertTrue(treeResults.get(0) instanceof Map);
        final HashMap<String, Object> map = (HashMap<String, Object>) treeResults.get(0);

        for(Map.Entry e : map.entrySet()) {
            Assert.assertTrue(e.getValue() instanceof Map);
            Map m = (Map) e.getValue();
            Assert.assertTrue(m.containsKey(Tokens._KEY));
            Assert.assertTrue(m.containsKey(Tokens._VALUE));
        }

        client.close();

    }

    @Test
    public void executeAndReturnSelect() throws Exception {
        final RexsterClient client = getClient();

        final List<Object> selectResults = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.v(1).out.name.as('x').select");
        Assert.assertEquals(3, selectResults.size());

        Assert.assertTrue(selectResults.get(0) instanceof Map);

        final List<String> names = new ArrayList<String>(){{
            add("vadas");
            add("josh");
            add("lop");
        }};

        for(Object e : selectResults) {
            final Map<String,Object> m = (Map<String, Object>) e;
            Assert.assertTrue(names.contains(m.get("x")));
        }

        client.close();

    }

    @Test
    public void executeAndReturnTable() throws Exception {
        final RexsterClient client = getClient();

        final List<Object> tableResults = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.v(1).out.name.as('x').table.cap.next()");
        Assert.assertEquals(3, tableResults.size());

        Assert.assertTrue(tableResults.get(0) instanceof Map);

        final List<String> names = new ArrayList<String>(){{
            add("vadas");
            add("josh");
            add("lop");
        }};

        for(Object e : tableResults) {
            final Map<String,Object> m = (Map<String, Object>) e;
            Assert.assertTrue(names.contains(m.get("x")));
        }

        client.close();

    }

    @Test
    public void executeForProperties() throws Exception {
        final RexsterClient client = getClient();

        final List<Map> stuffs = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.v(1).properties");
        Assert.assertEquals(1, stuffs.size());

        Map<String,Object> m = stuffs.get(0);

        Assert.assertEquals("class com.tinkerpop.blueprints.impls.tg.TinkerVertex", m.get("class"));
        Assert.assertEquals("1", m.get("id"));

        List<String> k = (List<String>) m.get("propertyKeys");
        Assert.assertEquals(2, k.size());
        Collections.sort(k);

        Assert.assertEquals("age", k.get(0));
        Assert.assertEquals("name", k.get(1));

    }

    @Test
    public void executeForTextWithBreaks() throws Exception {
        final RexsterClient client = getClient();

        // note that you have to escape the \r\n for it to be understood as a property value else the script engine
        // assumes it is line breaks in the script itself.
        final List<String> text = client.execute("g=new TinkerGraph();g.addVertex(['text':'''test1\\r\\ntest2\\r\\ntest3''']);g.v(0).text");

        Assert.assertEquals(1, text.size());
        Assert.assertEquals("test1\r\ntest2\r\ntest3", text.get(0));

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

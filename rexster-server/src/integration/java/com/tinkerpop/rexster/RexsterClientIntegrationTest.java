package com.tinkerpop.rexster;

import com.tinkerpop.rexster.client.RexsterClient;
import com.tinkerpop.rexster.client.RexsterClientFactory;
import junit.framework.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.msgpack.template.Templates.TInteger;
import static org.msgpack.template.Templates.TString;
import static org.msgpack.template.Templates.TValue;
import static org.msgpack.template.Templates.tMap;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexsterClientIntegrationTest extends AbstractRexProIntegrationTest {

    @Test
    public void executeExercise() throws Exception {
        final RexsterClient client = RexsterClientFactory.open();

        final List<Map<String, Object>> mapResults = client.execute("[val:1+1]");
        Assert.assertEquals(1, mapResults.size());
        final Map<String, Object> mapResult = mapResults.get(0);
        Assert.assertEquals("2", mapResult.get("val").toString());

        final List<Integer> intResults = client.executeList("1+1", null);
        Assert.assertEquals(1, intResults.size());
        final Integer intResult = intResults.get(0);
        Assert.assertEquals("2", intResult.toString());

        final List<Map<String, Object>> vertexResults = client.executeList("g=TinkerGraphFactory.createTinkerGraph();g.v(1)", null);
        Assert.assertEquals(1, vertexResults.size());
        final Map<String, Object> vertexResult = vertexResults.get(0);
        Assert.assertEquals("vertex", vertexResult.get("_type").toString());
        Assert.assertEquals("1", vertexResult.get("_id").toString());

        final Map<String, Object> vertexProperties = (Map<String, Object>) vertexResult.get("_properties");
        Assert.assertEquals("marko", vertexProperties.get("name"));
        Assert.assertEquals(29, vertexProperties.get("age"));
    }

    @Test
    public void executeMapValueConversion() throws Exception {
        final RexsterClient client = RexsterClientFactory.open();

        // all whole numerics convert to long
        // all float go to double
        final List<Map<String, Object>> mapResultsObject = client.execute("[n:1+1,b:true,f:1234.56f,s:'string',a:[1,2,3],m:[one:1]]");
        Assert.assertEquals(1, mapResultsObject.size());
        final Map<String, Object> mapResultObject = mapResultsObject.get(0);
        Assert.assertEquals(2, mapResultObject.get("n"));
        Assert.assertEquals(true, mapResultObject.get("b"));
        Assert.assertEquals(1234.56d, (Double) mapResultObject.get("f"), 0.001d);
        Assert.assertEquals("string", mapResultObject.get("s"));
        Assert.assertEquals(3, ((ArrayList) mapResultObject.get("a")).size());
        Assert.assertEquals(1, ((Map) mapResultObject.get("m")).get("one"));
    }

    @Test
    public void executeReturnGraphElementsValueConversion() throws Exception {
        final RexsterClient client = RexsterClientFactory.open();

        final List<Map<String, Object>> vertexResults = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.v(1)");
        Assert.assertEquals(1, vertexResults.size());
        final Map<String, Object> vertexResult = vertexResults.get(0);
        Assert.assertEquals("vertex", vertexResult.get("_type"));
        Assert.assertEquals("1", vertexResult.get("_id"));
        final Map vertexProperties = (Map) vertexResult.get("_properties");
        Assert.assertEquals("marko", vertexProperties.get("name"));
        Assert.assertEquals(29, vertexProperties.get("age"));
    }

    @Test
    public void executeReturnGraphElementsAsMapValueConversion() throws Exception {
        final RexsterClient client = RexsterClientFactory.open();

        final List<Map<String, Object>> vertexResults = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.v(1).map");
        Assert.assertEquals(1, vertexResults.size());
        final Map<String, Object> vertexResult = vertexResults.get(0);
        Assert.assertEquals("marko", vertexResult.get("name"));
        Assert.assertEquals(29, vertexResult.get("age"));
    }
}

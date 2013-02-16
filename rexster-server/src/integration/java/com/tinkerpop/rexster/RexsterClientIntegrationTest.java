package com.tinkerpop.rexster;

import com.tinkerpop.rexster.client.RexsterClient;
import com.tinkerpop.rexster.client.RexsterClientFactory;
import junit.framework.Assert;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.template.Template;
import org.msgpack.type.MapValue;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Converter;

import java.util.HashMap;
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

        final List<Map<String, Value>> mapResults = client.execute("[val:1+1]", tMap(TString, TValue));
        Assert.assertEquals(1, mapResults.size());
        final Map<String, Value> mapResult = mapResults.get(0);
        Assert.assertEquals("2", mapResult.get("val").toString());

        final List<Integer> intResults = client.execute("1+1", TInteger);
        Assert.assertEquals(1, intResults.size());
        final Integer intResult = intResults.get(0);
        Assert.assertEquals("2", intResult.toString());

        final MessagePack msgpack = new MessagePack();
        final Template<Map<String, Value>> mapTmpl = tMap(TString, TValue);

        final List<Map<String, Value>> vertexResults = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.v(1)", tMap(TString, TValue));
        Assert.assertEquals(1, vertexResults.size());
        final Map<String, Value> vertexResult = vertexResults.get(0);
        Assert.assertEquals("vertex", vertexResult.get("_type").asRawValue().getString());
        Assert.assertEquals("1", vertexResult.get("_id").asRawValue().getString());

        final MapValue vertexPropertiesValue = vertexResult.get("_properties").asMapValue();
        final Map<String, Value> vertexProperties = new HashMap<String, Value>();
        mapTmpl.read(new Converter(msgpack, vertexPropertiesValue), vertexProperties);
        Assert.assertEquals("marko", vertexProperties.get("name").asRawValue().getString());
        Assert.assertEquals(29, vertexProperties.get("age").asIntegerValue().getInt());
    }

    @Test
    public void executeMapValueConversion() throws Exception {
        final RexsterClient client = RexsterClientFactory.open();

        // all whole numerics convert to long
        // all float go to double
        final List<Map<String, Object>> mapResultsObject = client.execute("[n:1+1,b:true,f:1234.56f,s:'string',a:[1,2,3],m:[one:1]]");
        Assert.assertEquals(1, mapResultsObject.size());
        final Map<String, Object> mapResultObject = mapResultsObject.get(0);
        Assert.assertEquals(2l, mapResultObject.get("n"));
        Assert.assertEquals(true, mapResultObject.get("b"));
        Assert.assertEquals(1234.56d, (Double) mapResultObject.get("f"), 0.001d);
        Assert.assertEquals("string", mapResultObject.get("s"));
        Assert.assertEquals(3, ((Object []) mapResultObject.get("a")).length);
        Assert.assertEquals(1l, ((Map) mapResultObject.get("m")).get("one"));
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
        Assert.assertEquals(29l, vertexProperties.get("age"));
    }

    @Test
    public void executeReturnGraphElementsAsMapValueConversion() throws Exception {
        final RexsterClient client = RexsterClientFactory.open();

        final List<Map<String, Object>> vertexResults = client.execute("g=TinkerGraphFactory.createTinkerGraph();g.v(1).map");
        Assert.assertEquals(1, vertexResults.size());
        final Map<String, Object> vertexResult = vertexResults.get(0);
        Assert.assertEquals("marko", vertexResult.get("name"));
        Assert.assertEquals(29l, vertexResult.get("age"));
    }
}

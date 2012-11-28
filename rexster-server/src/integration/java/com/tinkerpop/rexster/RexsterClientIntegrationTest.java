package com.tinkerpop.rexster;

import com.tinkerpop.rexster.client.RexsterClient;
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
        final RexsterClient client = factory.createClient();

        final List<Map<String, Value>> mapResults = client.execute("[val:1+1]");
        Assert.assertEquals(1, mapResults.size());
        final Map<String, Value> mapResult = mapResults.get(0);
        Assert.assertEquals("2", mapResult.get("val").toString());

        final List<Integer> intResults = client.execute("1+1", TInteger);
        Assert.assertEquals(1, intResults.size());
        final Integer intResult = intResults.get(0);
        Assert.assertEquals("2", intResult.toString());

        final MessagePack msgpack = new MessagePack();
        final Template<Map<String, Value>> mapTmpl = tMap(TString, TValue);

        final List<Map<String, Value>> vertexResults = client.execute("g=rexster.getGraph(\"tinkergraph\");g.v(1)");
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
}

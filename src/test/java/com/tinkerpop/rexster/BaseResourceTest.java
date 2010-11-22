package com.tinkerpop.rexster;

import org.codehaus.jettison.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class BaseResourceTest {

	@Test
    public void testQueryParametersToJson()  throws JSONException {
        BaseResource tt = new MockResource();
        Map<String, String> qp = new HashMap<String, String>();
        qp.put("a", "true");
        qp.put("b", "false");
        qp.put("c.a", "12.0");
        qp.put("c.b", "\"marko\"");
        qp.put("c.c", "peter");
        qp.put("c.d.a.b", "true");
        qp.put("d", "[marko,rodriguez,10]");
        
        tt.buildRequestObject(qp);
        Assert.assertTrue(tt.getRequestObject().optBoolean("a"));
        Assert.assertFalse(tt.getRequestObject().optBoolean("b"));
        Assert.assertEquals(12.0, tt.getRequestObject().optJSONObject("c").optDouble("a"), 0);
        Assert.assertEquals("\"marko\"", tt.getRequestObject().optJSONObject("c").optString("b"));
        Assert.assertEquals("peter", tt.getRequestObject().optJSONObject("c").optString("c"));
        Assert.assertTrue(tt.getRequestObject().optJSONObject("c").optJSONObject("d").optJSONObject("a").optBoolean("b"));
        Assert.assertEquals("marko", tt.getRequestObject().optJSONArray("d").optString(0));
        Assert.assertEquals("rodriguez", tt.getRequestObject().optJSONArray("d").optString(1));
        // TODO: make this not a string but a number?
        Assert.assertEquals("10", tt.getRequestObject().optJSONArray("d").optString(2));
    }
	
	@Test 
	public void getStartOffsetEmptyRequest(){
		BaseResource tt = new MockResource();
        Assert.assertNull(tt.getStartOffset());
	}
	
	@Test 
	public void getStartOffsetNoOffset(){
		BaseResource tt = new MockResource();
		tt.buildRequestObject("{\"rexster\": { \"anyotherproperty\": { \"start\":\"ten\", \"end\":100 }}}");
        Assert.assertNull(tt.getStartOffset());
	}
	
	@Test 
	public void getStartOffsetInvalidOffset(){
		BaseResource tt = new MockResource();
		tt.buildRequestObject("{\"rexster\": { \"offset\": { \"start\":\"ten\", \"end\":100 }}}");
		Assert.assertEquals(0l, (long) tt.getStartOffset());
	}
	
	@Test 
	public void getStartOffsetValid(){
		BaseResource tt = new MockResource();
        tt.buildRequestObject("{\"rexster\": { \"offset\": { \"start\":10, \"end\":100 }}}");
        Assert.assertEquals(10l, (long)tt.getStartOffset());

        tt = new MockResource();
        tt.buildRequestObject("{\"rexster\": { \"offset\": { \"start\":-10, \"end\":10001 }}}");
        Assert.assertEquals(-10l, (long)tt.getStartOffset());
	}
	
	@Test 
	public void getEndOffsetEmptyRequest(){
		BaseResource tt = new MockResource();
        Assert.assertNull(tt.getEndOffset());
	}
	
	@Test 
	public void getEndOffsetNoOffset(){
		BaseResource tt = new MockResource();
		tt.buildRequestObject("{\"rexster\": { \"anyotherproperty\": { \"start\":10, \"end\":100 }}}");
        Assert.assertNull(tt.getEndOffset());
	}
	
	@Test 
	public void getEndOffsetInvalidOffset(){
		BaseResource tt = new MockResource();
		tt.buildRequestObject("{\"rexster\": { \"offset\": { \"start\":10, \"end\":\"onehundred\" }}}");
        Assert.assertEquals(0l, (long) tt.getEndOffset());
	}
	
	@Test 
	public void getEndOffsetValid(){
		BaseResource tt = new MockResource();
        tt.buildRequestObject("{\"rexster\": { \"offset\": { \"start\":10, \"end\":100 }}}");
        Assert.assertEquals(100l, (long)tt.getEndOffset());

        tt = new MockResource();
        tt.buildRequestObject("{\"rexster\": { \"offset\": { \"start\":-10, \"end\":10001 }}}");
        Assert.assertEquals(10001l, (long)tt.getEndOffset());
	}
	
	@Test 
	public void getReturnKeysEmptyRequest(){
		BaseResource tt = new MockResource();
        Assert.assertNull(tt.getEndOffset());
	}
	
	@Test 
	public void getReturnKeysNoKeys(){
		BaseResource tt = new MockResource();
		tt.buildRequestObject("{\"rexster\": { \"someproperty\": [ \"key\" ]}}");
        Assert.assertNull(tt.getReturnKeys());
	}
	
	@Test 
	public void getReturnKeysValid(){
		BaseResource tt = new MockResource();
		tt.buildRequestObject("{\"rexster\": { \"return_keys\": [ \"key1\" ]}}");
		Assert.assertNotNull(tt.getReturnKeys());
        Assert.assertEquals(1, tt.getReturnKeys().size());
        Assert.assertEquals("key1", tt.getReturnKeys().get(0));

        tt = new MockResource();
		tt.buildRequestObject("{\"rexster\": { \"return_keys\": [ \"key1\", \"key2\", \"key3\" ]}}");
		Assert.assertNotNull(tt.getReturnKeys());
        Assert.assertEquals(3, tt.getReturnKeys().size());
        Assert.assertEquals("key1", tt.getReturnKeys().get(0));
        Assert.assertEquals("key2", tt.getReturnKeys().get(1));
        Assert.assertEquals("key3", tt.getReturnKeys().get(2));
	}

    protected class MockResource extends BaseResource {
    }
    
}


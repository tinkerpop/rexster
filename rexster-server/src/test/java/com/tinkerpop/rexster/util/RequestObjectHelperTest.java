package com.tinkerpop.rexster.util;

import com.tinkerpop.rexster.Tokens;
import junit.framework.Assert;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;
import org.junit.Test;

import java.util.List;

public class RequestObjectHelperTest {

    @Test
    public void getShowTypesNoKey() {
        JSONObject jsonWithNoShowTypesKey = new JSONObject();
        Assert.assertFalse(RequestObjectHelper.getShowTypes(jsonWithNoShowTypesKey));
    }

    @Test
    public void getShowTypesBadValue() {
        JSONObject jsonWithNonBooleanValueForKey = buildJSONObjectFromString("{\"rexster\": { \"showTypes\": \"not-true-or-false\"}}");
        Assert.assertFalse(RequestObjectHelper.getShowTypes(jsonWithNonBooleanValueForKey));
    }

    @Test
    public void getShowTypesValid() {
        JSONObject jsonWithBooleanValueForKey = buildJSONObjectFromString("{\"rexster\": { \"showTypes\": true}}");
        Assert.assertTrue(RequestObjectHelper.getShowTypes(jsonWithBooleanValueForKey));
    }

    @Test
    public void getReturnKeysNullRequestObject() {
        Assert.assertNull(RequestObjectHelper.getReturnKeys(null));
    }

    @Test
    public void getReturnKeysWildcarded() {
        JSONObject jsonWithWildcard = buildJSONObjectFromString("{\"rexster\": { \"returnKeys\": [\"" + RequestObjectHelper.DEFAULT_WILDCARD + "\"]}}");
        Assert.assertNull(RequestObjectHelper.getReturnKeys(jsonWithWildcard));
    }

    @Test
    public void getReturnKeysNoneSpecified() {
        JSONObject jsonWithNoReturnKeysKey = new JSONObject();
        Assert.assertNull(RequestObjectHelper.getReturnKeys(jsonWithNoReturnKeysKey));
    }

    @Test
    public void getReturnKeysNonArrayBased() {
        JSONObject jsonWithNonArrayReturnKeyValue = buildJSONObjectFromString("{\"rexster\": { \"" + Tokens.RETURN_KEYS + "\": \"" + RequestObjectHelper.DEFAULT_WILDCARD + "\"}}");
        Assert.assertNull(RequestObjectHelper.getReturnKeys(jsonWithNonArrayReturnKeyValue));
    }

    @Test
    public void getReturnKeysValid() {
        JSONObject jsonWithTwoKeys = buildJSONObjectFromString("{\"rexster\": { \"" + Tokens.RETURN_KEYS + "\": [\"k1\",\"k2\"]}}");
        List<String> keys = RequestObjectHelper.getReturnKeys(jsonWithTwoKeys);
        Assert.assertNotNull(keys);
        Assert.assertEquals(2, keys.size());

        Assert.assertEquals("k1", keys.get(0));
        Assert.assertEquals("k2", keys.get(1));
    }

    @Test
    public void getReturnKeysNoKeys() throws Exception {
        JSONObject json = buildJSONObjectFromString("{\"rexster\": { \"someproperty\": [ \"key\" ]}}");
        Assert.assertNull(RequestObjectHelper.getReturnKeys(json));
    }

    @Test
    public void getStartOffsetEmptyRequest() {
        Assert.assertEquals(new Long(0), RequestObjectHelper.getStartOffset(null));
    }

    private JSONObject buildJSONObjectFromString(String json) {
        try {
            JSONTokener tokener = new JSONTokener(json);
            return new JSONObject(tokener);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void getEndOffsetNullRequest() {
        Assert.assertEquals(new Long(0), RequestObjectHelper.getStartOffset(null));
    }

    @Test
    public void getStartOffsetNoOffset() {
        JSONObject requestObject = buildJSONObjectFromString("{\"rexster\": { \"anyotherproperty\": { \"start\":\"ten\", \"end\":100 }}}");
        Assert.assertEquals(new Long(0), RequestObjectHelper.getStartOffset(requestObject));
    }

    @Test
    public void getStartOffsetInvalidOffset() {
        JSONObject requestObject = buildJSONObjectFromString("{\"rexster\": { \"offset\": { \"start\":\"ten\", \"end\":100 }}}");
        Assert.assertEquals(0l, (long) RequestObjectHelper.getStartOffset(requestObject));
    }

    @Test
    public void getStartOffsetWithNoStart() {
        JSONObject requestObject = buildJSONObjectFromString("{\"rexster\": { \"offset\": { \"end\":100 }}}");
        Assert.assertEquals(0l, (long) RequestObjectHelper.getStartOffset(requestObject));
    }

    @Test
    public void getStartOffsetValid() {
        JSONObject requestObject = buildJSONObjectFromString("{\"rexster\": { \"offset\": { \"start\":10, \"end\":100 }}}");
        Assert.assertEquals(10l, (long) RequestObjectHelper.getStartOffset(requestObject));

        requestObject = buildJSONObjectFromString("{\"rexster\": { \"offset\": { \"start\":-10, \"end\":10001 }}}");
        Assert.assertEquals(-10l, (long) RequestObjectHelper.getStartOffset(requestObject));
    }

    @Test
    public void getEndOffsetEmptyRequest() {
        JSONObject requestObject = buildJSONObjectFromString("{\"rexster\": \"nothing\"}");
        Assert.assertEquals(new Long(Long.MAX_VALUE), RequestObjectHelper.getEndOffset(requestObject));
    }

    @Test
    public void getEndOffsetEndNull() {
        Assert.assertEquals(new Long(Long.MAX_VALUE), RequestObjectHelper.getEndOffset(null));
    }

    @Test
    public void getEndOffsetStartWithNoEnd() {
        JSONObject requestObject = buildJSONObjectFromString("{\"rexster\": { \"offset\": { \"start\":10 }}}");
        Assert.assertEquals(new Long(Long.MAX_VALUE), RequestObjectHelper.getEndOffset(requestObject));
    }

    @Test
    public void getEndOffsetNoOffset() {
        JSONObject requestObject = buildJSONObjectFromString("{\"rexster\": { \"anyotherproperty\": { \"start\":10, \"end\":100 }}}");
        Assert.assertEquals(new Long(Long.MAX_VALUE), RequestObjectHelper.getEndOffset(requestObject));
    }

    @Test
    public void getEndOffsetInvalidOffset() {
        JSONObject requestObject = buildJSONObjectFromString("{\"rexster\": { \"offset\": { \"start\":10, \"end\":\"onehundred\" }}}");
        Assert.assertEquals(0l, (long) RequestObjectHelper.getEndOffset(requestObject));
    }

    @Test
    public void getEndOffsetValid() {
        JSONObject requestObject = buildJSONObjectFromString("{\"rexster\": { \"offset\": { \"start\":10, \"end\":100 }}}");
        Assert.assertEquals(100l, (long) RequestObjectHelper.getEndOffset(requestObject));

        requestObject = buildJSONObjectFromString("{\"rexster\": { \"offset\": { \"start\":-10, \"end\":10001 }}}");
        Assert.assertEquals(10001l, (long) RequestObjectHelper.getEndOffset(requestObject));
    }

    @Test
    public void hasElementPropertiesTrue() {
        JSONObject requestObject = buildJSONObjectFromString("{\"_id\": 1, \"name\":\"wally\" }");
        Assert.assertTrue(RequestObjectHelper.hasElementProperties(requestObject));
    }

    @Test
    public void hasElementPropertiesFalse() {
        JSONObject requestObject = buildJSONObjectFromString("{\"_id\": 1 }");
        Assert.assertFalse(RequestObjectHelper.hasElementProperties(requestObject));
    }

    @Test
    public void hasElementPropertiesNullRequestFalse() {
        Assert.assertFalse(RequestObjectHelper.hasElementProperties(null));
    }
}

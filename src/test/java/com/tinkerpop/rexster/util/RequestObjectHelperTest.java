package com.tinkerpop.rexster.util;

import com.tinkerpop.rexster.Tokens;
import junit.framework.Assert;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

public class RequestObjectHelperTest {

    @Test
    public void getShowTypesNoKey() {
        JSONObject jsonWithNoShowTypesKey = new JSONObject();
        Assert.assertFalse(RequestObjectHelper.getShowTypes(jsonWithNoShowTypesKey));
    }

    @Test
    public void getShowTypesBadValue() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Tokens.SHOW_TYPES, "not-true-or-false");
        JSONObject jsonWithNonBooleanValueForKey = new JSONObject(map);
        Assert.assertFalse(RequestObjectHelper.getShowTypes(jsonWithNonBooleanValueForKey));
    }

    @Test
    public void getShowTypesValid() {
        HashMap<String, Boolean> map = new HashMap<String, Boolean>();
        map.put(Tokens.SHOW_TYPES, true);
        JSONObject jsonWithBooleanValueForKey = new JSONObject(map);
        Assert.assertTrue(RequestObjectHelper.getShowTypes(jsonWithBooleanValueForKey));
    }

    @Test
    public void getReturnKeysWildcarded() {
        HashMap<String, JSONArray> map = new HashMap<String, JSONArray>();

        JSONArray returnKeysAsJson =  new JSONArray();
        returnKeysAsJson.put(RequestObjectHelper.DEFAULT_WILDCARD);
        map.put(Tokens.RETURN_KEYS, returnKeysAsJson);

        JSONObject jsonWithWildcard = new JSONObject(map);
        Assert.assertNull(RequestObjectHelper.getReturnKeys(jsonWithWildcard));
    }

    @Test
    public void getReturnKeysNoneSpecified() {
        JSONObject jsonWithNoReturnKeysKey = new JSONObject();
        Assert.assertNull(RequestObjectHelper.getReturnKeys(jsonWithNoReturnKeysKey));
    }

    @Test
    public void getReturnKeysNonArrayBased() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Tokens.RETURN_KEYS, RequestObjectHelper.DEFAULT_WILDCARD);

        JSONObject jsonWithNonArrayReturnKeyValue = new JSONObject(map);
        Assert.assertNull(RequestObjectHelper.getReturnKeys(jsonWithNonArrayReturnKeyValue));
    }

    @Test
    public void getReturnKeysValid() {
        HashMap<String, JSONArray> map = new HashMap<String, JSONArray>();

        JSONArray returnKeysAsJson =  new JSONArray();
        returnKeysAsJson.put("k1");
        returnKeysAsJson.put("k2");
        map.put(Tokens.RETURN_KEYS, returnKeysAsJson);

        JSONObject jsonWithWildcard = new JSONObject(map);
        List<String> keys = RequestObjectHelper.getReturnKeys(jsonWithWildcard);
        Assert.assertNotNull(keys);
        Assert.assertEquals(2, keys.size());

        Assert.assertEquals("k1", keys.get(0));
        Assert.assertEquals("k2", keys.get(1));
    }
}

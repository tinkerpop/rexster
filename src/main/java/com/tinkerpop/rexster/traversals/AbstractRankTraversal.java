package com.tinkerpop.rexster.traversals;

import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.rexster.*;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class AbstractRankTraversal extends AbstractTraversal {

    protected Map<Object, com.tinkerpop.rexster.ElementJSONObject> idToElement = new HashMap<Object, com.tinkerpop.rexster.ElementJSONObject>();
    protected List<com.tinkerpop.rexster.ElementJSONObject> ranks = null;
    protected Sort sort = Sort.NONE;
    protected String sortKey = null;
    protected List<String> returnKeys = null;
    protected int startOffset = -1;
    protected int endOffset = -1;
    protected double totalRank = Double.NaN;


    protected enum Sort {
        NONE, REGULAR, REVERSE
    }

    protected void sortRanks(final String key) {
        java.util.Collections.sort(this.ranks, new Comparator<com.tinkerpop.rexster.ElementJSONObject>() {
            public int compare(com.tinkerpop.rexster.ElementJSONObject e1, com.tinkerpop.rexster.ElementJSONObject e2) {
                try {
                    return -1 * ((Comparable) e1.get(key)).compareTo(e2.get(key));
                } catch (JSONException ex) {
                    // TODO: bah!
                    return -1;
                }
            }
        });
        if (this.sort == Sort.REGULAR)
            Collections.reverse(this.ranks);
    }

    protected void offsetRanks() {
        int s;
        int e;
        if (this.startOffset == -1)
            s = 0;
        else
            s = this.startOffset;
        if (this.endOffset == -1)
            e = this.ranks.size();
        else
            e = this.endOffset;

        if (this.ranks.size() < e)
            e = this.ranks.size();
        if (this.ranks.size() < s)
            s = this.ranks.size();

        this.ranks = this.ranks.subList(s, e);
    }

    protected void generateRankList() {
        this.ranks = new ArrayList<com.tinkerpop.rexster.ElementJSONObject>(this.idToElement.values());
    }


    protected void incrRank(final Element element, final Double incr) throws JSONException {
        Object elementId = element.getId();
        final String traversalName = getTraversalName();
        com.tinkerpop.rexster.ElementJSONObject elementObject = this.idToElement.get(elementId);
        if (null == elementObject) {
            if (null == this.returnKeys)
                elementObject = new com.tinkerpop.rexster.ElementJSONObject(element, this.showTypes);
            else
                elementObject = new com.tinkerpop.rexster.ElementJSONObject(element, this.returnKeys, this.showTypes);
            this.idToElement.put(elementId, elementObject);
        }

        Double value = elementObject.optDouble(traversalName);
        if (!value.equals(Double.NaN)) {
            elementObject.put(traversalName, incr + value);
        } else {
            elementObject.put(traversalName, incr);
        }
    }

    protected double incrRank(final Iterator<? extends Element> iterator, final Double incr) throws JSONException {
        double totalRank = 0.0f;
        while (iterator.hasNext()) {
            incrRank(iterator.next(), incr);
            totalRank = totalRank + incr;
        }
        return totalRank;
    }

    protected double setRank(final Map<? extends Element, Number> rank) throws JSONException {
        double totalRank = 0.0f;
        for (final Map.Entry<? extends Element, Number> entry : rank.entrySet()) {
            final double value = entry.getValue().doubleValue();
            incrRank(entry.getKey(), value);
            totalRank = totalRank + value;
        }
        return totalRank;
    }

    protected void preQuery() {
        super.preQuery();
        String sort = (String) this.requestObject.opt(Tokens.SORT);
        if (null != sort) {
            if (sort.equals(Tokens.REGULAR))
                this.sort = Sort.REGULAR;
            else if (sort.equals(Tokens.REVERSE))
                this.sort = Sort.REVERSE;
        }
        if (this.requestObject.has(Tokens.SORT_KEY)) {
            this.sortKey = (String) this.requestObject.opt(Tokens.SORT_KEY);
        } else {
            sortKey = getTraversalName();
        }

        JSONObject offset = (JSONObject) this.requestObject.opt(Tokens.OFFSET);
        if (null != offset) {
            Long start = offset.optLong(Tokens.START);
            Long end = offset.optLong(Tokens.END);
            if (null != start)
                this.startOffset = start.intValue();
            if (null != end)
                this.endOffset = end.intValue();
        }

        if (this.requestObject.has(Tokens.RETURN_KEYS)) {

            this.returnKeys = new ArrayList<String>();

            // return keys may come in as a string value if there is only one
            String returnKeyString = this.requestObject.optString(Tokens.RETURN_KEYS);
            if (returnKeyString == null || returnKeyString.length() == 0) {
                JSONArray returnKeyArray = this.requestObject.optJSONArray(Tokens.RETURN_KEYS);

                if (returnKeyArray != null && returnKeyArray.length() > 0) {
                    for (int ix = 0; ix < returnKeyArray.length(); ix++) {
                        this.returnKeys.add(returnKeyArray.optString(ix));
                    }
                }
            } else {
                this.returnKeys.add(returnKeyString);
            }

            if (this.returnKeys.size() == 1 && this.returnKeys.get(0).equals(Tokens.WILDCARD)) {
                this.returnKeys = null;
            }
        }
    }

    protected boolean isResultInCache() {
        boolean inCache = false;
        JSONObject tempResultObject = this.resultObjectCache.getCachedResult(this.cacheRequestURI);
        if (tempResultObject != null) {
            this.ranks = (List<com.tinkerpop.rexster.ElementJSONObject>) tempResultObject.opt(Tokens.RANKS);
            this.totalRank = (Double) tempResultObject.opt(Tokens.TOTAL_RANK);
            this.success = true;
            inCache = true;
        }

        return inCache;
    }

    protected void postQuery(boolean resultInCache) throws JSONException {
        if (this.success) {
            if (null == this.ranks)
                this.generateRankList();

            if (this.sort != Sort.NONE && !resultInCache) {
                this.sortRanks(this.sortKey);
            }
            if (this.totalRank != Double.NaN) {
                this.resultObject.put(Tokens.TOTAL_RANK, this.totalRank);
            }
            this.resultObject.put(Tokens.RANKS, this.ranks);
            this.resultObject.put(Tokens.SIZE, this.ranks.size());

            this.cacheCurrentResultObjectState();
            if (this.startOffset != -1 || this.endOffset != -1) {
                this.offsetRanks();
                this.resultObject.put(Tokens.RANKS, this.ranks);
                this.resultObject.put(Tokens.SIZE, this.ranks.size());
            }
        }
        super.postQuery(resultInCache);
    }

    protected Map<String, Object> getParameters() {
        Map<String, Object> parameters = super.getParameters();
        parameters.put(Tokens.OFFSET_START, "the start integer of a page of results (default is 0)");
        parameters.put(Tokens.OFFSET_END, "the end integer of a page of results (default is infinity)");
        parameters.put(Tokens.SORT, "regular, reverse, or none sort the ranked results (default is none)");
        parameters.put(Tokens.SORT_KEY, "the name of the element key to use in the sorting (default is the rank value)");
        parameters.put(Tokens.RETURN_KEYS, "the element property keys to return (default is to return all element properties)");
        return parameters;
    }

}

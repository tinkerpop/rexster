package com.tinkerpop.rexster.traversals;

import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.rexster.Tokens;
import org.json.simple.JSONObject;

import java.util.*;

/**
 * @author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class AbstractRankTraversal extends AbstractTraversal {

    protected Map<Object, ElementJSONObject> idToElement = new HashMap<Object, ElementJSONObject>();
    protected List<ElementJSONObject> ranks = null;
    protected Sort sort = Sort.NONE;
    protected String sortKey = null;
    protected List<String> returnKeys = null;
    protected int startOffset = -1;
    protected int endOffset = -1;
    protected float totalRank = Float.NaN;


    protected enum Sort {
        NONE, REGULAR, REVERSE
    }

    protected void sortRanks(final String key) {
        java.util.Collections.sort(this.ranks, new Comparator<ElementJSONObject>() {
            public int compare(ElementJSONObject e1, ElementJSONObject e2) {
                return -1 * ((Comparable) e1.get(key)).compareTo(e2.get(key));
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
        this.ranks = new ArrayList<ElementJSONObject>(this.idToElement.values());
    }


    protected void incrRank(final Element element, final Float incr) {
        Object elementId = element.getId();
        final String traversalName = getTraversalName();
        ElementJSONObject elementObject = this.idToElement.get(elementId);
        if (null == elementObject) {
            if (null == this.returnKeys)
                elementObject = new ElementJSONObject(element);
            else
                elementObject = new ElementJSONObject(element, this.returnKeys);
            this.idToElement.put(elementId, elementObject);
        }

        Float value = (Float) elementObject.get(traversalName);
        if (null != value) {
            elementObject.put(traversalName, incr + value);
        } else {
            elementObject.put(traversalName, incr);
        }
    }

    protected float incrRank(final Iterator<? extends Element> iterator, final Float incr) {
        float totalRank = 0.0f;
        while (iterator.hasNext()) {
            incrRank(iterator.next(), incr);
            totalRank = totalRank + incr;
        }
        return totalRank;
    }

    protected void preQuery() {
        super.preQuery();
        String sort = (String) this.requestObject.get(Tokens.SORT);
        if (null != sort) {
            if (sort.equals(Tokens.REGULAR))
                this.sort = Sort.REGULAR;
            else if (sort.equals(Tokens.REVERSE))
                this.sort = Sort.REVERSE;
        }
        if (this.requestObject.containsKey(Tokens.SORT_KEY)) {
            this.sortKey = (String) this.requestObject.get(Tokens.SORT_KEY);
        } else {
            sortKey = getTraversalName();
        }

        JSONObject offset = (JSONObject) this.requestObject.get(Tokens.OFFSET);
        if (null != offset) {
            Long start = (Long) offset.get(Tokens.START);
            Long end = (Long) offset.get(Tokens.END);
            if (null != start)
                this.startOffset = start.intValue();
            if (null != end)
                this.endOffset = end.intValue();
        }

        if (this.requestObject.containsKey(Tokens.RETURN_KEYS)) {
            this.returnKeys = (List<String>) this.requestObject.get(Tokens.RETURN_KEYS);
            if (this.returnKeys.size() == 1 && this.returnKeys.get(0).equals(Tokens.WILDCARD))
                this.returnKeys = null;
        }

        if (this.allowCached) {
            JSONObject tempResultObject = this.resultObjectCache.getCachedResult(this.cacheRequestURI);
            if (tempResultObject != null) {
                this.ranks = (List<ElementJSONObject>) tempResultObject.get(Tokens.RANKS);
                this.totalRank = (Float) tempResultObject.get(Tokens.TOTAL_RANK);
                this.success = true;
                this.usingCachedResult = true;
            }
        }
    }

    protected void postQuery() {
        if (this.success) {
            if (null == this.ranks)
                this.generateRankList();

            if (this.sort != Sort.NONE && !this.usingCachedResult) {
                this.sortRanks(this.sortKey);
            }
            if (this.totalRank != Float.NaN) {
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
        super.postQuery();
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

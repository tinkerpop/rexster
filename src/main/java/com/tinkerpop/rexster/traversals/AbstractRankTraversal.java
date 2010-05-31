package com.tinkerpop.rexster.traversals;

import com.tinkerpop.blueprints.pgm.Element;
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

    private static final String RANKS = "ranks";
    private static final String SIZE = "size";
    private static final String SORT = "sort";
    private static final String SORT_KEY = "sort_key";
    private static final String OFFSET = "offset";
    private static final String START = "start";
    private static final String END = "end";
    private static final String REVERSE = "reverse";
    private static final String REGULAR = "regular";
    private static final String RETURN_KEYS = "return_keys";
    private static final String WILDCARD = "*";
    protected static final String TOTAL_RANK = "total_rank";


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

        this.ranks = this.ranks.subList(s, e);
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
        String sort = (String) this.requestObject.get(SORT);
        if (null != sort) {
            if (sort.equals(REGULAR))
                this.sort = Sort.REGULAR;
            else if (sort.equals(REVERSE))
                this.sort = Sort.REVERSE;
        }
        if (this.requestObject.containsKey(SORT_KEY)) {
            this.sortKey = (String) this.requestObject.get(SORT_KEY);
        } else {
            sortKey = getTraversalName();
        }

        JSONObject offset = (JSONObject) this.requestObject.get(OFFSET);
        if (null != offset) {
            Long start = (Long) offset.get(START);
            Long end = (Long) offset.get(END);
            if (null != start)
                this.startOffset = start.intValue();
            if (null != end)
                this.endOffset = end.intValue();
        }

        if (this.requestObject.containsKey(RETURN_KEYS)) {
            this.returnKeys = (List<String>) this.requestObject.get(RETURN_KEYS);
            if (this.returnKeys.size() == 1 && this.returnKeys.get(0).equals(WILDCARD))
                this.returnKeys = null;
        }

        if (this.allowCached) {
            JSONObject tempResultObject = this.resultObjectCache.getCachedResult(this.cacheRequestURI);
            if (tempResultObject != null) {
                this.ranks = (List<ElementJSONObject>) tempResultObject.get(RANKS);
                this.totalRank = (Float) tempResultObject.get(TOTAL_RANK);
                this.success = true;
                this.usingCachedResult = true;
            }
        }
    }

    protected void postQuery() {
        if (this.success) {
            if (null == this.ranks)
                this.ranks = new ArrayList<ElementJSONObject>(this.idToElement.values());

            if (this.sort != Sort.NONE && !this.usingCachedResult) {
                sortRanks(this.sortKey);
            }
            if (this.totalRank != Float.NaN) {
                this.resultObject.put(TOTAL_RANK, this.totalRank);
            }
            this.resultObject.put(RANKS, this.ranks);
            this.resultObject.put(SIZE, this.ranks.size());

            this.cacheCurrentResultObjectState();
            if (this.startOffset != -1 || this.endOffset != -1) {
                offsetRanks();
                this.resultObject.put(RANKS, this.ranks);
                this.resultObject.put(SIZE, this.ranks.size());
            }
        }
        super.postQuery();
    }

    protected Map<String, Object> getParameters() {
        Map<String, Object> parameters = super.getParameters();
        parameters.put("offset.start", "the start integer of a page of results (default is 0)");
        parameters.put("offset.end", "the end integer of a page of results (default is infinity)");
        parameters.put(SORT, "regular, reverse, or none sort the ranked results (default is none)");
        parameters.put(SORT_KEY, "the name of the element key to use in the sorting (default is the rank value)");
        parameters.put(RETURN_KEYS, "the element property keys to return (default is to return all element properties)");
        return parameters;
    }

}

package com.tinkerpop.rexster.traversals;

import com.tinkerpop.blueprints.pgm.Element;
import org.json.simple.JSONObject;

import java.util.*;

/**
 * @author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class AbstractRankTraversal extends AbstractTraversal {

    protected Map<Object, ElementJSONObject> idToElement = new HashMap<Object, ElementJSONObject>();
    protected Sort sortType = Sort.NONE;
    protected List returnKeys = null;
    protected long startOffset = -1;
    protected long endOffset = -1;
    protected float totalRank = Float.NaN;

    private static final String RANKS = "ranks";
    private static final String SIZE = "size";
    private static final String SORT = "sort";
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


    protected void sortRanksByValue() {
        final String shortName = getTraversalName();
        List<Map.Entry<Object, ElementJSONObject>> list = new ArrayList<Map.Entry<Object, ElementJSONObject>>(this.idToElement.entrySet());
        java.util.Collections.sort(list, new Comparator<Map.Entry<Object, ElementJSONObject>>() {
            public int compare(Map.Entry<Object, ElementJSONObject> e1, Map.Entry<Object, ElementJSONObject> e2) {
                if (e1.getValue().get(shortName).equals(e2.getValue().get(shortName)))
                    return (e1.toString().compareTo(e2.toString()));
                else if ((Float) e1.getValue().get(shortName) > (Float) e2.getValue().get(shortName))
                    return 1;
                else
                    return -1;
            }
        });
        if (this.sortType == Sort.REVERSE)
            Collections.reverse(list);

        this.idToElement = new LinkedHashMap<Object, ElementJSONObject>();
        for (Map.Entry<Object, ElementJSONObject> entry : list) {
            this.idToElement.put(entry.getKey(), entry.getValue());
        }
    }

    protected void offsetRanks() {
        List<Map.Entry<Object, ElementJSONObject>> list = new ArrayList<Map.Entry<Object, ElementJSONObject>>(this.idToElement.entrySet());
        this.idToElement = new LinkedHashMap<Object, ElementJSONObject>();
        int counter = 0;
        for (Map.Entry<Object, ElementJSONObject> entry : list) {
            if ((startOffset == -1 || counter >= startOffset) && (endOffset == -1 || counter < endOffset)) {
                this.idToElement.put(entry.getKey(), entry.getValue());
            }
            counter++;
        }
    }

    protected void incrRank(final Element element, final Float incr) {
        Object elementId = element.getId();
        final String shortName = getTraversalName();
        ElementJSONObject elementObject = this.idToElement.get(elementId);
        if (null == elementObject) {
            if(null == this.returnKeys)
                elementObject = new ElementJSONObject(element);
            else
                elementObject = new ElementJSONObject(element, this.returnKeys);
            this.idToElement.put(elementId, elementObject);
        }

        Float value = (Float) elementObject.get(shortName);
        if (null != value) {
            elementObject.put(shortName, incr + value);
        } else {
            elementObject.put(shortName, incr);
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
                this.sortType = Sort.REGULAR;
            else if (sort.equals(REVERSE))
                this.sortType = Sort.REVERSE;
        }
        JSONObject offset = (JSONObject) this.requestObject.get(OFFSET);
        if (null != offset) {
            Long start = (Long) offset.get(START);
            Long end = (Long) offset.get(END);
            if (null != start)
                this.startOffset = start;
            if (null != end)
                this.endOffset = end;
        }

        if (this.requestObject.containsKey(RETURN_KEYS)) {
            this.returnKeys = (List) this.requestObject.get(RETURN_KEYS);
            if (this.returnKeys.size() == 1 && this.returnKeys.get(0).equals(WILDCARD))
                this.returnKeys = null;
            //System.out.println(this.returnKeys);
        }

        if (this.allowCached) {
            JSONObject tempResultObject = this.resultObjectCache.getCachedResult(this.cacheRequestURI);
            if (tempResultObject != null) {
                this.idToElement = (Map<Object, ElementJSONObject>) tempResultObject.get(RANKS);
                this.totalRank = (Float) tempResultObject.get(TOTAL_RANK);
                this.success = true;
                this.usingCachedResult = true;
            }
        }
    }

    protected void postQuery() {
        if (this.success) {
            if (this.sortType != Sort.NONE && !this.usingCachedResult) {
                sortRanksByValue();
            }
            if (this.totalRank != Float.NaN) {
                this.resultObject.put(TOTAL_RANK, this.totalRank);
            }
            this.resultObject.put(RANKS, idToElement);
            this.resultObject.put(SIZE, this.idToElement.size());

            this.cacheCurrentResultObjectState();
            if (this.startOffset != -1 || this.endOffset != -1) {
                offsetRanks();
                this.resultObject.put(RANKS, this.idToElement);
                this.resultObject.put(SIZE, this.idToElement.size());
            }
        }
        super.postQuery();
    }

    protected Map<String, Object> getParameters() {
        Map<String, Object> parameters = super.getParameters();
        parameters.put("offset.start", "the start integer of a page of results (default is 0)");
        parameters.put("offset.end", "the end integer of a page of results (default is infinity)");
        parameters.put(SORT, "regular, reverse, or none sort the ranked results (default is none)");
        parameters.put(RETURN_KEYS, "the element property keys to return (default is to return all element properties)");
        return parameters;
    }

}

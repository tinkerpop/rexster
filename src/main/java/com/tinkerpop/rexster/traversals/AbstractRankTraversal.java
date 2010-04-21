package com.tinkerpop.rexster.traversals;

import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.rexster.ResultObjectCache;
import org.json.simple.JSONObject;

import java.util.*;

/**
 * @author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class AbstractRankTraversal extends AbstractTraversal {

    protected Map<Object, Float> ranks = new HashMap<Object, Float>();
    protected Sort sortType = Sort.NONE;
    protected long startOffset = -1;
    protected long endOffset = -1;
    protected String returnKey = null;
    protected float totalRank = Float.NaN;

    private static final String RANKS = "ranks";
    private static final String SIZE = "size";
    private static final String SORT = "sort";
    private static final String OFFSET = "offset";
    private static final String START = "start";
    private static final String END = "end";
    private static final String REVERSE = "reverse";
    private static final String REGULAR = "regular";
    private static final String RETURN_KEY = "return_key";
    protected static final String TOTAL_RANK = "total_rank";


    protected enum Sort {
        NONE, REGULAR, REVERSE
    }

    protected void sortRanksByValue() {
        List<Map.Entry<Object, Float>> list = new ArrayList<Map.Entry<Object, Float>>(ranks.entrySet());
        java.util.Collections.sort(list, new Comparator<Map.Entry<Object, Float>>() {
            public int compare(Map.Entry<Object, Float> e1, Map.Entry<Object, Float> e2) {
                if (e1.getValue().equals(e2.getValue()))
                    return (e1.toString().compareTo(e2.toString()));
                else if (e1.getValue() > e2.getValue())
                    return 1;
                else
                    return -1;
            }
        });
        if (this.sortType == Sort.REVERSE)
            Collections.reverse(list);

        this.ranks = new LinkedHashMap<Object, Float>();
        for (Map.Entry<Object, Float> entry : list) {
            ranks.put(entry.getKey(), entry.getValue());
        }
    }

    protected void offsetRanks() {
        List<Map.Entry<Object, Float>> list = new ArrayList<Map.Entry<Object, Float>>(ranks.entrySet());
        this.ranks = new LinkedHashMap<Object, Float>();
        int counter = 0;
        for (Map.Entry<Object, Float> entry : list) {
            if ((startOffset == -1 || counter >= startOffset) && (endOffset == -1 || counter < endOffset)) {
                this.ranks.put(entry.getKey(), entry.getValue());
            }
            counter++;
        }
    }

    protected void incrRank(final Object key, final Float incr) {
        Float value = ranks.get(key);
        if (null != value) {
            ranks.put(key, incr + value);
        } else {
            ranks.put(key, incr);
        }
    }

    protected float incrRank(Iterator<? extends Element> iterator, final Float incr) {
        float totalRank = 0.0f;
        while (iterator.hasNext()) {
            if (null == this.returnKey)
                incrRank(iterator.next().getId(), incr);
            else
                incrRank(iterator.next().getProperty(this.returnKey), incr);
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

        this.returnKey = (String) this.requestObject.get(RETURN_KEY);

        if (this.allowCached) {
            JSONObject tempResultObject = ResultObjectCache.getCachedResult(this.cacheRequestURI);
            if (tempResultObject != null) {
                this.ranks = (Map<Object, Float>) tempResultObject.get(RANKS);
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
}

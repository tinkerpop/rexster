package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Index;

public class MockIndex implements Index {

    private String indexName;
    private Class clazz;
    private long count;

    public MockIndex(String indexName, Class clazz, long count) {
        this.indexName = indexName;
        this.clazz = clazz;
        this.count = count;
    }

    public long count(String arg0, Object arg1) {
        return count;
    }

    public CloseableIterable get(String arg0, Object arg1) {
        return null;
    }

    @Override
    public CloseableIterable query(String key, Object query) {
        return null;
    }

    public Class getIndexClass() {
        return this.clazz;
    }

    public String getIndexName() {
        return this.indexName;
    }

    public void put(String arg0, Object arg1, Element arg2) {
    }

    public void remove(String arg0, Object arg1, Element arg2) {
    }

}

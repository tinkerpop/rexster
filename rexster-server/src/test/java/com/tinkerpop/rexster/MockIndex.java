package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.CloseableSequence;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Index;

public class MockIndex implements Index {

    private String indexName;
    private Type indexType;
    private Class clazz;
    private long count;

    public MockIndex(String indexName, Type type, Class clazz, long count) {
        this.indexName = indexName;
        this.indexType = type;
        this.clazz = clazz;
        this.count = count;
    }

    public long count(String arg0, Object arg1) {
        return count;
    }

    public CloseableSequence get(String arg0, Object arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    public Class getIndexClass() {
        return this.clazz;
    }

    public String getIndexName() {
        return this.indexName;
    }

    public Type getIndexType() {
        return this.indexType;
    }

    public void put(String arg0, Object arg1, Element arg2) {
        // TODO Auto-generated method stub

    }

    public void remove(String arg0, Object arg1, Element arg2) {
        // TODO Auto-generated method stub

    }

}

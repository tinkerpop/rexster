package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Index;

public class MockIndex implements Index {

    private String indexName;
    private Type indexType;
    private Class clazz;

    public MockIndex(String indexName, Type type, Class clazz) {
        this.indexName = indexName;
        this.indexType = type;
        this.clazz = clazz;
    }

    @Override
    public Iterable get(String arg0, Object arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class getIndexClass() {
        return this.clazz;
    }

    @Override
    public String getIndexName() {
        return this.indexName;
    }

    @Override
    public Type getIndexType() {
        return this.indexType;
    }

    @Override
    public void put(String arg0, Object arg1, Element arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void remove(String arg0, Object arg1, Element arg2) {
        // TODO Auto-generated method stub

    }

}

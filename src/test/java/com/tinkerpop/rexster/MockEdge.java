package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Vertex;

import java.util.Hashtable;
import java.util.Set;

public class MockEdge implements Edge {

    private String id;
    private String label;
    private Hashtable<String, Object> properties = new Hashtable<String, Object>();
    private Vertex inVertex;
    private Vertex outVertex;

    public MockEdge(String id, String label, Hashtable<String, Object> properties, Vertex in, Vertex out) {
        this.id = id;
        this.label = label;
        this.properties = properties;
        this.inVertex = in;
        this.outVertex = out;
    }

    @Override
    public Object getId() {
        return this.id;
    }

    @Override
    public Object getProperty(String key) {
        return this.properties.get(key);
    }

    @Override
    public Set<String> getPropertyKeys() {
        return this.properties.keySet();
    }

    @Override
    public Object removeProperty(String key) {
        return this.properties.remove(key);
    }

    @Override
    public void setProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    @Override
    public Vertex getInVertex() {
        return this.inVertex;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public Vertex getOutVertex() {
        return this.outVertex;
    }

}

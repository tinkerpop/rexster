package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Vertex;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

public class MockVertex implements Vertex {

    private String id;

    private Hashtable<String, Object> properties = new Hashtable<String, Object>();

    private ArrayList<Edge> inEdges = new ArrayList<Edge>();
    private ArrayList<Edge> outEdges = new ArrayList<Edge>();

    public MockVertex(String id) {
        this.id = id;
    }

    public void setInEdges(ArrayList<Edge> inEdges) {
        this.inEdges = inEdges;
    }

    public void setOutEdges(ArrayList<Edge> outEdges) {
        this.outEdges = outEdges;
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
    public Iterable<Edge> getInEdges() {
        return this.inEdges;
    }

    @Override
    public Iterable<Edge> getOutEdges() {
        return this.outEdges;
    }

}

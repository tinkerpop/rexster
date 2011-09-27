package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.gremlin.pipes.filter.LabelFilterPipe;
import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.pipes.filter.FilterPipe;

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

    public Object getId() {
        return this.id;
    }

    public Object getProperty(String key) {
        return this.properties.get(key);
    }

    public Set<String> getPropertyKeys() {
        return this.properties.keySet();
    }

    public Object removeProperty(String key) {
        return this.properties.remove(key);
    }

    public void setProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    public Iterable<Edge> getOutEdges(String... labels) {
        if (labels.length == 0) {
            return this.outEdges;
        } else {
            Pipe pipe = new LabelFilterPipe(labels[0], FilterPipe.Filter.EQUAL);
            pipe.setStarts(this.outEdges);
            return pipe;
        }
    }

    public Iterable<Edge> getInEdges(String... labels) {
        if (labels.length == 0) {
            return this.inEdges;
        } else {
            Pipe pipe = new LabelFilterPipe(labels[0], FilterPipe.Filter.EQUAL);
            pipe.setStarts(this.inEdges);
            return pipe;
        }
    }
}

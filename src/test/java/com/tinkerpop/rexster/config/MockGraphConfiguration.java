package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import org.apache.commons.configuration.Configuration;

public class MockGraphConfiguration implements GraphConfiguration {

    public Graph configureGraphInstance(Configuration properties) throws GraphConfigurationException {
        return new MockGraph();
    }

    public class MockGraph implements Graph {

        public Vertex addVertex(Object o) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Vertex getVertex(Object o) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void removeVertex(Vertex vertex) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public Iterable<Vertex> getVertices() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Edge addEdge(Object o, Vertex vertex, Vertex vertex1, String s) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Edge getEdge(Object o) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void removeEdge(Edge edge) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public Iterable<Edge> getEdges() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void clear() {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void shutdown() {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}


package com.tinkerpop.rexster.traversals;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.rexster.Tokens;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.List;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class ElementJSONObject extends JSONObject {

    private final Object id;

    public ElementJSONObject(final Element element) throws JSONException {
        this(element, null);
    }

    public ElementJSONObject(final Element element, final List<String> propertyKeys) throws JSONException {
        this.id = element.getId();
        if (element instanceof Vertex) {
            this.put(Tokens._TYPE, Tokens.VERTEX);
        } else {
            this.put(Tokens._TYPE, Tokens.EDGE);
        }
        
        if (propertyKeys == null) {
            this.put(Tokens._ID, this.id);
            for (String key : element.getPropertyKeys()) {
                this.put(key, element.getProperty(key));
            }
            
            if (element instanceof Edge) {
                Edge edge = (Edge) element;
                this.put(Tokens._LABEL, edge.getLabel());
                this.put(Tokens._IN_V, edge.getInVertex().getId());
                this.put(Tokens._OUT_V, edge.getOutVertex().getId());
            }
        } else {
            for (String key : propertyKeys) {
                if (key.equals(Tokens._ID)) {
                    this.put(Tokens._ID, this.id);
                } else if (element instanceof Edge && key.equals(Tokens._LABEL)) {
                    Edge edge = (Edge) element;
                    this.put(Tokens._LABEL, edge.getLabel());
                } else if (element instanceof Edge && key.equals(Tokens._IN_V)) {
                    Edge edge = (Edge) element;
                    this.put(Tokens._IN_V, edge.getInVertex().getId());
                } else if (element instanceof Edge && key.equals(Tokens._OUT_V)) {
                    Edge edge = (Edge) element;
                    this.put(Tokens._IN_V, edge.getOutVertex().getId());
                } else {
                    Object temp = element.getProperty(key);
                    if (null != temp) {
                        this.put(key, temp);
                    }
                }
            }
        }
    }

    public int hashCode() {
        return this.id.hashCode();
    }

    public Object getId() {
        return this.id;
    }

    public boolean equals(Object object) {
        if (object instanceof ElementJSONObject)
            return ((ElementJSONObject) object).getId().equals(this.id);
        else
            return false;
    }
}


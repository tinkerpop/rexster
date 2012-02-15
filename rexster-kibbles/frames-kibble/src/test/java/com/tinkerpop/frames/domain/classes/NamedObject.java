package com.tinkerpop.frames.domain.classes;

import com.tinkerpop.frames.Property;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public interface NamedObject {

    @Property("name")
    public String getName();

    @Property("name")
    public void setName(final String name);
}

package com.tinkerpop.frames.domain.relations;

import com.tinkerpop.frames.Domain;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.Range;
import com.tinkerpop.frames.domain.classes.Person;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public interface Knows {

    @Property("weight")
    public Float getWeight();

    @Property("weight")
    public Float setWeight(float weight);

    @Domain
    public Person getDomain();

    @Range
    public Person getRange();
}

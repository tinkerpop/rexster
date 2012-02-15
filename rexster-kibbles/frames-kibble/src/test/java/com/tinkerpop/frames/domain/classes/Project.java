package com.tinkerpop.frames.domain.classes;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Direction;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.Relation;
import com.tinkerpop.frames.domain.relations.CreatedBy;

import java.util.Collection;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public interface Project extends NamedObject {

    @Property("lang")
    public String getLanguage();

    @Relation(label = "created", direction = Direction.INVERSE)
    public Collection<Person> getCreatedByPeople();

    @Adjacency(label = "created", direction = Direction.INVERSE)
    public Collection<CreatedBy> getCreatedBy();

    @Relation(label = "created", direction = Direction.INVERSE)
    public void removeCreatedByPerson(Person person);

    @Adjacency(label = "created", direction = Direction.INVERSE)
    public void removeCreatedBy(CreatedBy createdBy);
}


package com.tinkerpop.frames.domain.classes;


import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Incident;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.domain.incidences.CreatedBy;

import java.util.Collection;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public interface Project extends NamedObject {

    @Property("lang")
    public String getLanguage();

    @Adjacency(label = "created", direction = Direction.IN)
    public Collection<Person> getCreatedByPeople();

    @Incident(label = "created", direction = Direction.IN)
    public Collection<CreatedBy> getCreatedBy();


    @Adjacency(label = "created", direction = Direction.IN)
    public void removeCreatedByPerson(Person person);

    @Incident(label = "created", direction = Direction.IN)
    public void removeCreatedBy(CreatedBy createdBy);
}


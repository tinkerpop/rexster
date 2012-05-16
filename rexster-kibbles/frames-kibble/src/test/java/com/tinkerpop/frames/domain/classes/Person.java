package com.tinkerpop.frames.domain.classes;


import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Incidence;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.domain.incidences.Created;
import com.tinkerpop.frames.domain.incidences.Knows;

import java.util.Collection;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public interface Person extends NamedObject {

    @Property("age")
    public Integer getAge();

    @Property("age")
    public void setAge(int age);

    @Property("age")
    public void removeAge();

    @Adjacency(label = "knows")
    public Collection<Knows> getKnows();

    @Incidence(label = "knows")
    public Collection<Person> getKnowsPeople();

    @Adjacency(label = "created")
    public Collection<Created> getCreated();

    @Incidence(label = "created")
    public Collection<Project> getCreatedProjects();

    @Incidence(label = "knows")
    public void addKnowsPerson(final Person person);

    @Adjacency(label = "knows")
    public Knows addKnows(final Person person);

    @Incidence(label = "created")
    public void addCreatedProject(final Project project);

    @Adjacency(label = "created")
    public Created addCreated(final Project project);

    @Incidence(label = "knows")
    public void removeKnowsPerson(final Person person);

    @Adjacency(label = "knows")
    public void removeKnows(final Knows knows);

    /*@GremlinInference(script = "_{x=it}.outE('created').inV.inE('created').outV{it!=x}")
    public Collection<Person> getCoCreators();*/

}

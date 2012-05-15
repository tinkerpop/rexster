package com.tinkerpop.frames.domain.classes;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.Relation;
import com.tinkerpop.frames.domain.relations.Created;
import com.tinkerpop.frames.domain.relations.Knows;

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

    @Relation(label = "knows")
    public Collection<Knows> getKnows();

    @Adjacency(label = "knows")
    public Collection<Person> getKnowsPeople();

    @Relation(label = "created")
    public Collection<Created> getCreated();

    @Adjacency(label = "created")
    public Collection<Project> getCreatedProjects();

    @Adjacency(label = "knows")
    public void addKnowsPerson(final Person person);

    @Relation(label = "knows")
    public Knows addKnows(final Person person);

    @Adjacency(label = "created")
    public void addCreatedProject(final Project project);

    @Relation(label = "created")
    public Created addCreated(final Project project);

    @Adjacency(label = "knows")
    public void removeKnowsPerson(final Person person);

    @Relation(label = "knows")
    public void removeKnows(final Knows knows);

    /*@GremlinInference(script = "_{x=it}.outE('created').inV.inE('created').outV{it!=x}")
    public Collection<Person> getCoCreators();*/

}

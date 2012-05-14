package com.tinkerpop.frames.domain.classes;

import com.tinkerpop.frames.Adjacent;
import com.tinkerpop.frames.Incident;
import com.tinkerpop.frames.Property;
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

    @Incident(label = "knows")
    public Collection<Knows> getKnows();

    @Adjacent(label = "knows")
    public Collection<Person> getKnowsPeople();

    @Incident(label = "created")
    public Collection<Created> getCreated();

    @Adjacent(label = "created")
    public Collection<Project> getCreatedProjects();

    @Adjacent(label = "knows")
    public void addKnowsPerson(final Person person);

    @Incident(label = "knows")
    public Knows addKnows(final Person person);

    @Adjacent(label = "created")
    public void addCreatedProject(final Project project);

    @Incident(label = "created")
    public Created addCreated(final Project project);

    @Adjacent(label = "knows")
    public void removeKnowsPerson(final Person person);

    @Incident(label = "knows")
    public void removeKnows(final Knows knows);

    /*@GremlinInference(script = "_{x=it}.outE('created').inV.inE('created').outV{it!=x}")
    public Collection<Person> getCoCreators();*/

}

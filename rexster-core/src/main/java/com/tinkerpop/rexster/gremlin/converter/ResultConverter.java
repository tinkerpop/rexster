package com.tinkerpop.rexster.gremlin.converter;

/**
 * A ResultConverter converts output from Gremlin to some other format.
 * <p/>
 * An example is the ConsoleResultConverter which takes Gremlin output and converts it to a list of Strings for
 * display in the Console.
 *
 * @param <T> the type to convert Gremlin output to.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public interface ResultConverter<T> {
    /**
     * Converts the result to a sensible format given the implementation.
     * <p/>
     * Gremlin can send back many different kinds of output as a result.  That output is the value passed to
     * this method to be processed and converted to some other type.  A result may be an Iterator, Vertex,
     * Edge, numeric, string, etc.  Implementation is not trivial.
     */
    T convert(final Object result) throws Exception;
}

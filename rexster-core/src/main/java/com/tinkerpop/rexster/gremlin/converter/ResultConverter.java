package com.tinkerpop.rexster.gremlin.converter;

public interface ResultConverter<T> {
    /**
     * Converts the result to a sensible format given the implementation.
     */
    T convert(final Object result) throws Exception;
}

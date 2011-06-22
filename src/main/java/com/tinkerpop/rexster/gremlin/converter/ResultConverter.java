package com.tinkerpop.rexster.gremlin.converter;

import java.io.Writer;

public interface ResultConverter<T> {
    T convert(final Object result, final Writer outputWriter) throws Exception;
}

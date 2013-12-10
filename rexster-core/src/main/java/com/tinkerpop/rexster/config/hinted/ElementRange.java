package com.tinkerpop.rexster.config.hinted;

import com.tinkerpop.blueprints.Element;

import java.io.Serializable;

public interface ElementRange<U, E extends Element> extends Serializable {

    public Class<E> getElementType();

    public boolean contains(U item);

    public int getPriority();

}

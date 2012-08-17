package com.tinkerpop.rexster;

import javax.ws.rs.core.Request;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ResourceHolder<T> {
    private final T resource;
    private final Request request;

    public ResourceHolder(final T resource, final Request request) {
        this.resource = resource;
        this.request = request;
    }

    public T getResource() {
        return resource;
    }

    public Request getRequest() {
        return request;
    }
}

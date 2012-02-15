package com.tinkerpop.rexster.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a parameter on an extension method as one that should receive attention from Rexster by
 * plugging in values that match the expected types from the request.  This annotation can be
 * applied to these types:  edge/vertex/graph (dependent on the ExtensionPoint),
 * and RexsterResourceContext.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RexsterContext {
}

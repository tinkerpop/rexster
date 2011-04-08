package com.tinkerpop.rexster.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines an extension method to rexster.  Extensions may be applied to one of three
 * extension points: graph, vertex and edge.  This basically means that extensions can
 * hang from any one of these resources in rexster.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExtensionDefinition {
    ExtensionPoint extensionPoint();
    String path() default "";
}

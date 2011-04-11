package com.tinkerpop.rexster.extension;

import javax.ws.rs.core.MediaType;
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

    /**
     * Defines the resource from which this extension is applied.
     */
    ExtensionPoint extensionPoint();

    /**
     * Defines the path to the method extension.
     */
    String path() default "";

    /**
     * Specifies the media type to be returned by the extension.
     */
    String produces() default MediaType.APPLICATION_JSON;
}

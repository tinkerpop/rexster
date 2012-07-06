package com.tinkerpop.rexster.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines an extension method to rexster.  Extensions may be applied to one of three
 * extension points: graph, vertex and edge.  This basically means that extensions can
 * hang from any one of these resources in rexster.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
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
     * If the ExtensionDefinition is configured to produce JSON, setting this value to true
     * will try to insert the Rexster version and query time attributes.
     */
    boolean tryIncludeRexsterAttributes() default true;

    // don't clean up namespace http://stackoverflow.com/questions/1425088/incompatible-types-found-required-default-enums-in-annotations

    /**
     * The HTTP method that the extension method will support.  By default this is set to ANY which means any
     * request will be passed through to the extension.
     */
    HttpMethod method() default com.tinkerpop.rexster.extension.HttpMethod.ANY;

    /**
     * Specifies the media type to be returned by the extension. By default this value
     * is application/json.
     */
    String produces() default javax.ws.rs.core.MediaType.APPLICATION_JSON;


}

package com.tinkerpop.rexster.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The descriptor for the extension used for self-documentation and hypermedia.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ExtensionDescriptor {
    String description();

    ExtensionApi[] api() default {};

    // don't clean up namespace http://stackoverflow.com/questions/1425088/incompatible-types-found-required-default-enums-in-annotations
    ExtensionApiBehavior apiBehavior() default com.tinkerpop.rexster.extension.ExtensionApiBehavior.DEFAULT;
}

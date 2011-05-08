package com.tinkerpop.rexster.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides for a namespace and extension name which is applied at a class level.  Extensions
 * are then exposed on specific graphs by their namespace and name via rexster.xml configuration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExtensionNaming {
    String name() default "";

    String namespace() default "g";
}

package com.tinkerpop.rexster.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tries to extract a value from the request to inject into the extension.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface ExtensionRequestParameter {
    String name();

    String description() default "";

    /**
     * This value can only be set to primitive/string values.  Values are set as a string, but
     * will be coerced to the type connected to this annotation.   Even though this is an array
     * it is treated as a single value so that it can be evaluated to null. Only the first value
     * is used if more than one is specified.
     */
    String[] defaultValue() default {};

    boolean parseToJson() default true;
}

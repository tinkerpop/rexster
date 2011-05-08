package com.tinkerpop.rexster.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ExtensionDescriptor {
    String description();

    ExtensionApi[] api() default {};

    ExtensionApiBehavior apiBehavior() default ExtensionApiBehavior.DEFAULT;
}

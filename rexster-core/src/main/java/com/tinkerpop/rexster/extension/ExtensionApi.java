package com.tinkerpop.rexster.extension;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents one parameter in the API of the extension.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ExtensionApi {
    String parameterName();

    String description() default "";
}

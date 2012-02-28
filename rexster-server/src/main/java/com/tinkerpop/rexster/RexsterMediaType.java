package com.tinkerpop.rexster;

import javax.ws.rs.core.MediaType;

public class RexsterMediaType {
    public final static String APPLICATION_REXSTER_TYPED_JSON = "application/vnd.rexster-typed-v1+json";
    public final static MediaType APPLICATION_REXSTER_TYPED_JSON_TYPE = new
            MediaType("application", "vnd.rexster-typed-v1+json");

    public final static String APPLICATION_REXSTER_JSON = "application/vnd.rexster-v1+json";
    public final static MediaType APPLICATION_REXSTER_JSON_TYPE = new
            MediaType("application", "vnd.rexster-v1+json");
}

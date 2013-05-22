package com.tinkerpop.rexster.protocol.server;

/**
 * Raised when rexpro is asked to process an incomplete request
 */
public class IncompleteRexProRequest extends Exception {
    public IncompleteRexProRequest() {
    }

    public IncompleteRexProRequest(String s) {
        super(s);
    }

    public IncompleteRexProRequest(String s, Throwable throwable) {
        super(s, throwable);
    }

    public IncompleteRexProRequest(Throwable throwable) {
        super(throwable);
    }
}

package com.tinkerpop.rexster.protocol.server;

/**
 * Raised when rexpro is asked to process an incomplete request
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class IncompleteRexProRequestException extends Exception {
    public IncompleteRexProRequestException() {
    }

    public IncompleteRexProRequestException(String s) {
        super(s);
    }

    public IncompleteRexProRequestException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public IncompleteRexProRequestException(Throwable throwable) {
        super(throwable);
    }
}

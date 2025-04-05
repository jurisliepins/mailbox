package com.github.jurisliepins.mailbox;

public final class ActorException extends RuntimeException {
    public ActorException(final String message) {
        super(message);
    }

    public ActorException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ActorException(final Throwable cause) {
        super(cause);
    }
}

package com.github.jurisliepins.mailbox;

import static java.util.Objects.requireNonNull;

public record Letter(Object message, ActorSystem system, ActorRef self, ActorRef sender) {
    public Letter {
        requireNonNull(message, "message must not be null");
        requireNonNull(system, "system must not be null");
        requireNonNull(self, "self must not be null");
        requireNonNull(sender, "sender must not be null");
    }
}

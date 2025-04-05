package com.github.jurisliepins.mailbox;

import static java.util.Objects.requireNonNull;

public sealed interface Mailbox permits Mailbox.Success, Mailbox.Failure {

    record Success(Object message, ActorSystem system, ActorRef self, ActorRef sender) implements Mailbox {
        public Success {
            requireNonNull(message, "message must not be null");
            requireNonNull(system, "system must not be null");
            requireNonNull(self, "self must not be null");
            requireNonNull(sender, "sender must not be null");
        }
    }

    record Failure(Object message, ActorSystem system, ActorRef self, ActorRef sender, Throwable cause) implements Mailbox {
        public Failure {
            requireNonNull(message, "message must not be null");
            requireNonNull(system, "system must not be null");
            requireNonNull(self, "self must not be null");
            requireNonNull(sender, "sender must not be null");
        }
    }

    default <T> void reply(final T replyMessage) {
        switch (this) {
            case Success mailbox -> mailbox.sender().post(replyMessage);
            case Failure mailbox -> mailbox.sender().post(replyMessage);
        }
    }

    default <T> void reply(final T replyMessage, final ActorRef replySender) {
        switch (this) {
            case Success mailbox -> mailbox.sender().post(replyMessage, replySender);
            case Failure mailbox -> mailbox.sender().post(replyMessage, replySender);
        }
    }
}

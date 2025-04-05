package com.github.jurisliepins.mailbox.examples;

import com.github.jurisliepins.mailbox.ActorSystem;
import com.github.jurisliepins.mailbox.Mailbox;
import com.github.jurisliepins.mailbox.NextState;

import java.io.IOException;

public final class SimpleExample {
    public record Ping(String message) {
        @Override
        public String toString() {
            return message;
        }
    }

    public record Pong(String message) {
        @Override
        public String toString() {
            return message;
        }
    }

    public static void main(final String[] args) throws IOException {
        var system = new ActorSystem();

        var ref1 = system.spawn(mailbox -> switch (mailbox) {
            case Mailbox.Success m -> {
                switch (m.message()) {
                    case Ping ping -> {
                        System.out.println("Actor 1 received: " + ping);
                        m.sender().post(new Pong("Pong"));
                        System.out.println("Actor 1 responded with pong");
                    }
                    case Pong pong -> System.out.println("Actor 1 received pong: " + pong);
                    default -> System.err.println("Actor 1 received unexpected message: " + m.message());
                }
                yield NextState.Receive;
            }
            case Mailbox.Failure m -> {
                System.err.println("Actor 1 failed with: " + m.message() + ".");
                yield NextState.Terminate;
            }
        });

        var ref2 = system.spawn(mailbox -> switch (mailbox) {
            case Mailbox.Success m -> {
                switch (m.message()) {
                    case Ping ping -> System.out.println("Actor 2 received: " + ping);
                    case Pong pong -> System.out.println("Actor 2 received: " + pong);
                    default -> System.err.println("Actor 2 received unexpected message: " + m.message());
                }
                yield NextState.Receive;
            }
            case Mailbox.Failure m -> {
                System.err.println("Actor 2 failed with: " + m.message());
                yield NextState.Terminate;
            }
        });

        ref1.post(new Ping("Ping"), ref2);

        var ignored = System.in.read();

        system.shutdown();
    }
}

package com.github.jurisliepins.mailbox;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public interface Actor {
    class DeadLetter implements ActorRef {
        public static final DeadLetter INSTANCE = new DeadLetter();

        @Override
        public <T> ActorRef post(final T message, final ActorRef sender) {
            return this;
        }

        @Override
        public <T> ActorRef post(final T message) {
            return this;
        }

        @Override
        public <T, U> U postWithReply(final T message) {
            throw new ActorException("Cannot post to a blank actor");
        }

        @Override
        public <T, U> U postWithReply(final T message, final long timeout, final TimeUnit unit) {
            throw new ActorException("Cannot post to a blank actor");
        }
    }

    class BlockingQueue implements ActorRef, Runnable {
        private final LinkedBlockingQueue<Letter> letters = new LinkedBlockingQueue<>();
        private final ActorSystem system;
        private final MailboxReceiver receiver;

        public BlockingQueue(final ActorSystem system, final MailboxReceiver receiver) {
            this.system = requireNonNull(system, "system is null");
            this.receiver = requireNonNull(receiver, "receiver is null");
        }

        public <T> ActorRef post(final T message, final ActorRef sender) {
            requireNonNull(message, "message is null");
            letters.add(new Letter(message, system, this, sender));
            return this;
        }

        public <T> ActorRef post(final T message) {
            requireNonNull(message, "message is null");
            letters.add(new Letter(message, system, this, DeadLetter.INSTANCE));
            return this;
        }

        @Override
        public <T, U> U postWithReply(final T message) {
            requireNonNull(message, "message is null");
            var awaiter = new Awaiter<U>();
            var awaiterRef = system.spawn(awaiter);
            post(message, awaiterRef);
            return awaiter.awaitResult();
        }

        @Override
        public <T, U> U postWithReply(final T message, final long timeout, final TimeUnit unit) {
            requireNonNull(message, "message is null");
            var awaiter = new Awaiter<U>();
            var awaiterRef = system.spawn(awaiter);
            post(message, awaiterRef);
            return awaiter.awaitResult(timeout, unit);
        }

        public void run() {
            // This actor relies on the fact that blocking operations ran inside virtual threads will now be
            // unmounted from platform thread when blocking is detected with the stack copied into heap memory.
            // Once the operation unblocks, the stack is mounted back to the platform thread and execution is
            // resumed. All blocking operations have been refactored in Java 21, including the LinkedBlockingQueue.
            //
            // Explanation https://www.youtube.com/watch?v=5E0LU85EnTI.
            //
            // Actor setup inspired by
            // https://www.javaadvent.com/2022/12/actors-and-virtual-threads-a-match-made-in-heaven.html.
            //
            // We continue iterating until we get NextState.Terminate.
            NextState nextState;
            do {
                try {
                    var letter = letters.take();
                    try {
                        var mailbox = new Mailbox.Success(letter.message(), letter.system(), letter.self(), letter.sender());
                        nextState = receiver.receive(mailbox);
                    } catch (Throwable cause) {
                        try {
                            var mailbox = new Mailbox.Failure(letter.message(), letter.system(), letter.self(), letter.sender(), cause);
                            nextState = receiver.receive(mailbox);
                        } catch (Throwable ignored) {
                            // If actor throws while handling an exception then we can fall into an infinite loop so we simply terminate.
                            nextState = NextState.Terminate;
                        }
                    }
                } catch (InterruptedException ignored) {
                    // Interrupted means actor system has been shutdown, so we stop the actor.
                    return;
                }
            } while (nextState == NextState.Receive);
        }
    }
}

package com.github.jurisliepins.mailbox;

import java.util.concurrent.TimeUnit;

public interface ActorRef {
    <T> ActorRef post(T message, ActorRef sender);

    <T> ActorRef post(T message);

    <T, U> U postWithReply(T message);

    <T, U> U postWithReply(T message, long timeout, TimeUnit unit);
}
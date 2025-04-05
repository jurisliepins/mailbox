package com.github.jurisliepins.mailbox;

@FunctionalInterface
public interface MailboxReceiver {
    NextState receive(Mailbox mailbox);
}

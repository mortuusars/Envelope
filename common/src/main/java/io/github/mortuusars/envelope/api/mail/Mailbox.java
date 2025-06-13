package io.github.mortuusars.envelope.api.mail;

import io.github.mortuusars.envelope.EnvelopeServer;

public class Mailbox {
    public static void send(Mail mail) {
        EnvelopeServer.getMailCoordinator().send(mail);
    }
}

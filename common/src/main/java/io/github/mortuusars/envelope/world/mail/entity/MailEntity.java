package io.github.mortuusars.envelope.world.mail.entity;

import io.github.mortuusars.envelope.core.address.Address;
import io.github.mortuusars.envelope.world.mail.MailReceiver;

public abstract class MailEntity implements MailReceiver {
    private final Address.Npc address;

    public MailEntity(Address.Npc address) {
        this.address = address;
    }

    public Address.Npc getAddress() {
        return address;
    }
}

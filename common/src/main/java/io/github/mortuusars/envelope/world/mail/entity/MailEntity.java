package io.github.mortuusars.envelope.world.mail.entity;

import io.github.mortuusars.envelope.core.address.Address;
import io.github.mortuusars.envelope.world.mail.MailReceiver;

public abstract class MailEntity implements MailReceiver {
    private final Address.Entity address;

    public MailEntity(Address.Entity address) {
        this.address = address;
    }

    public Address.Entity getAddress() {
        return address;
    }
}

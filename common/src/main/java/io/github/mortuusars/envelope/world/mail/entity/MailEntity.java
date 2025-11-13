package io.github.mortuusars.envelope.world.mail.entity;

import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.MailReceiver;

public abstract class MailEntity implements MailReceiver {
    private final Address.Entity address;
    private final int distance;

    public MailEntity(Address.Entity address, int distance) {
        this.address = address;
        this.distance = distance;
    }

    public Address.Entity getAddress() {
        return address;
    }

    public int getDistance() {
        return distance;
    }
}

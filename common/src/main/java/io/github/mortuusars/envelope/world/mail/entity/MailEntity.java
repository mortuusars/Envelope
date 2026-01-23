package io.github.mortuusars.envelope.world.mail.entity;

import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressLocation;
import io.github.mortuusars.envelope.world.mail.receiver.MailReceiver;

public abstract class MailEntity implements MailReceiver {
    private final Address.Entity address;
    private final AddressLocation location;

    public MailEntity(Address.Entity address, AddressLocation location) {
        this.address = address;
        this.location = location;
    }

    public Address.Entity getAddress() {
        return address;
    }

    public AddressLocation getLocation() {
        return location;
    }
}

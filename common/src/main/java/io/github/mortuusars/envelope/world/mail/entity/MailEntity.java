package io.github.mortuusars.envelope.world.mail.entity;

import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressLocation;
import io.github.mortuusars.envelope.world.mail.receiver.MailReceiver;

public abstract class MailEntity implements MailReceiver {
    private final MailService service;
    private final Address.Entity address;
    private final AddressLocation location;

    public MailEntity(MailService service, Address.Entity address, AddressLocation location) {
        this.service = service;
        this.address = address;
        this.location = location;
    }

    public MailService getService() {
        return service;
    }

    public Address.Entity getAddress() {
        return address;
    }

    public AddressLocation getLocation() {
        return location;
    }
}

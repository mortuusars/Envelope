package io.github.mortuusars.envelope.world.mail.entity;

import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.AddressLocation;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import io.github.mortuusars.envelope.world.mail.receiver.MailReceiver;

public abstract class MailEntity implements MailReceiver {
    private final MailService service;
    private final EntityAddress address;
    private final AddressLocation location;

    public MailEntity(MailService service, EntityAddress address, AddressLocation location) {
        this.service = service;
        this.address = address;
        this.location = location;
    }

    public MailService getService() {
        return service;
    }

    public EntityAddress getAddress() {
        return address;
    }

    public AddressLocation getLocation() {
        return location;
    }
}

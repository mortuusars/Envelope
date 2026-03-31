package io.github.mortuusars.envelope.world.item.crafting.mail;

import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;

public abstract class CustomMailRecipe implements MailRecipe {
    private final ServiceAddress address;

    public CustomMailRecipe(ServiceAddress address) {
        this.address = address;
    }

    @Override
    public ServiceAddress getAddress() {
        return address;
    }
}

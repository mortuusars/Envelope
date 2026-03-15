package io.github.mortuusars.envelope.world.item.crafting.mail;

import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;

public abstract class CustomMailRecipe implements MailRecipe {
    private final EntityAddress address;

    public CustomMailRecipe(EntityAddress address) {
        this.address = address;
    }

    @Override
    public EntityAddress getAddress() {
        return address;
    }
}

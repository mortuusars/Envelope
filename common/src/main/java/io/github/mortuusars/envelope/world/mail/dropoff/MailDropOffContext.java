package io.github.mortuusars.envelope.world.mail.dropoff;

import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class MailDropOffContext {
    private final MailService service;
    private final Address target;
    private final Delivery delivery;

    public MailDropOffContext(MailService service, Address target, Delivery delivery) {
        this.service = service;
        this.target = target;
        this.delivery = delivery;
    }

    public MailService getService() {
        return service;
    }

    public ServerLevel getLevel() {
        return getService().getLevel();
    }

    public Address getTarget() {
        return target;
    }

    public Delivery getDelivery() {
        return delivery;
    }

    public ItemStack getMail() {
        return getDelivery().getMail();
    }

    public boolean isReturned() {
        return Mail.isReturned(getMail());
    }
}

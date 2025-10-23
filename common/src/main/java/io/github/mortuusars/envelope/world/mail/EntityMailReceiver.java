package io.github.mortuusars.envelope.world.mail;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.core.address.Address;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class EntityMailReceiver implements MailReceiver {
    private final Address.Npc address;

    public EntityMailReceiver(Address.Npc address) {
        this.address = address;
    }

    @Override
    public ItemStack receiveMail(ServerLevel level, ItemStack mail) {
        return Envelope.MAIL_ENTITIES.byAddress(address)
              .map(entity -> entity.receiveMail(level, mail))
              .orElseGet(() -> Mail.returnedRecipientNotFound(mail));
    }
}

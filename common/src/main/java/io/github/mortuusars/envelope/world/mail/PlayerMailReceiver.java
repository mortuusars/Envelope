package io.github.mortuusars.envelope.world.mail;

import io.github.mortuusars.envelope.core.address.Address;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class PlayerMailReceiver implements MailReceiver {
    private final Address.Player address;

    public PlayerMailReceiver(Address.Player address) {
        this.address = address;
    }

    @Override
    public ItemStack receiveMail(ServerLevel level, ItemStack mail) {
        return level.getEnvelopeContext().getKnownPlayers().getUuid(address)
              .flatMap(uuid -> level.getEnvelopeContext().getDefaultAddresses().of(uuid))
              .map(PigeonholeMailReceiver::new)
              .map(receiver -> receiver.receiveMail(level, mail))
              .orElseGet(() -> Mail.returnedRecipientNotFound(mail));
    }
}

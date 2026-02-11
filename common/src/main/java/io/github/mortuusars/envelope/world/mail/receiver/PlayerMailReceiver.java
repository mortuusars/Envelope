package io.github.mortuusars.envelope.world.mail.receiver;

import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class PlayerMailReceiver implements MailReceiver {
    private final PlayerAddress address;

    public PlayerMailReceiver(PlayerAddress address) {
        this.address = address;
    }

    @Override
    public ItemStack receiveMail(ServerLevel level, Address sender, ItemStack mail) {
        return MailService.of(level).getKnownPlayers().getDefaultAddressOf(address)
              .map(MailboxMailReceiver::new)
              .map(receiver -> receiver.receiveMail(level, sender, mail))
              .orElseGet(() -> returned(mail, MailService.of(level).getAddress(), DeliveryRecord.Message.RECIPIENT_NOT_FOUND));
    }
}
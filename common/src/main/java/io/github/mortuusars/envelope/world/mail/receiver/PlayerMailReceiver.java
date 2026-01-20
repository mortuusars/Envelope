package io.github.mortuusars.envelope.world.mail.receiver;

import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class PlayerMailReceiver implements MailReceiver {
    private final Address.Player address;

    public PlayerMailReceiver(Address.Player address) {
        this.address = address;
    }

    @Override
    public ItemStack receiveMail(ServerLevel level, ItemStack mail) {
        return MailService.of(level).getPlayers().getDefaultAddressOf(address)
              .map(BlockMailReceiver::new)
              .map(receiver -> receiver.receiveMail(level, mail))
              .orElseGet(() -> returned(mail, DeliveryRecord.Message.RECIPIENT_NOT_FOUND));
    }
}
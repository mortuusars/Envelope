package io.github.mortuusars.envelope.world.mail.delivery.incoming;

import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class PlayerMailHandler implements IncomingMailHandler {
    private final PlayerAddress address;

    public PlayerMailHandler(PlayerAddress address) {
        this.address = address;
    }

    @Override
    public ItemStack handle(ServerLevel level, Delivery delivery) {
        return MailService.of(level).getKnownPlayers().getDefaultAddressOf(address)
              .map(BlockMailHandler::new)
              .map(receiver -> receiver.handle(level, delivery))
              .orElseGet(() -> Mail.returned(delivery.getMail(), DeliveryRecord.Message.RECIPIENT_NOT_FOUND));
    }
}
package io.github.mortuusars.envelope.world.mail.receiver;

import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class PlayerMailReceiver implements MailReceiver {
    private final Address.Player address;

    public PlayerMailReceiver(Address.Player address) {
        this.address = address;
    }

    @Override
    public ItemStack receiveMail(ServerLevel level, ItemStack mail) {
        return ItemStack.EMPTY;
//        return MailService.of(level).getPlayers().getDefaultAddressOf(address)
//              .map(BlockMailReceiver::new)
//              .map(receiver -> receiver.receiveMail(level, mail))
//              .orElseGet(() -> mail.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
//                    .message(DeliveryRecord.Message.RECIPIENT_NOT_FOUND)));
    }
}
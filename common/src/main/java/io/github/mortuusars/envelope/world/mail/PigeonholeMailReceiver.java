package io.github.mortuusars.envelope.world.mail;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.core.address.Address;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import io.github.mortuusars.envelope.world.item.component.MailDeliveryLog;
import io.github.mortuusars.envelope.world.service.pigeonhole.PigeonholeManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public class PigeonholeMailReceiver implements MailReceiver {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Address.Pigeonhole address;

    public PigeonholeMailReceiver(Address.Pigeonhole address) {
        this.address = address;
    }

    @Override
    public ItemStack receiveMail(ServerLevel level, ItemStack mail) {
        PigeonholeManager pigeonholeManager = level.getEnvelopeContext().getPigeonholeManager();
        if (pigeonholeManager.putMail(address, mail)) {
            MailDeliveryLog.addRecords(mail,
                  MailDeliveryLog.Record.arrivedTo(address).atTime(level.getGameTime()));

            pigeonholeManager.getPositionOf(address).ifPresent(pos -> {
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity) {
                    blockEntity.onMailReceived(level, mail);
                }
            });

            return ItemStack.EMPTY;
        } else {
            LOGGER.info("Cannot deliver mail to pigeonhole '{}': address not found. Returning to sender.", address);
            return Mail.returned(mail, address);
        }
    }
}

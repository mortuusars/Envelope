package io.github.mortuusars.envelope.world.mail;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.item.component.MailDeliveryLog;
import io.github.mortuusars.envelope.world.mail.address.Address;
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

        return pigeonholeManager.getBlockEntityOf(address)
              .map(blockEntity -> {
                  MailDeliveryLog.addRecords(mail, MailDeliveryLog.Record.arrivedTo(address).atTime(level.getGameTime()));
                  blockEntity.insertMail(mail);
                  return ItemStack.EMPTY;
              })
              .orElseGet(() -> pigeonholeManager.getData(address)
                    .map(data -> {
                        MailDeliveryLog.addRecords(mail, MailDeliveryLog.Record.arrivedTo(address).atTime(level.getGameTime()));
                        data.insertMail(mail);
                        return ItemStack.EMPTY;
                    })
                    .orElseGet(() -> {
                        LOGGER.info("Cannot deliver mail to pigeonhole '{}': address not found. Returning to sender.", address);
                        return Mail.returnedRecipientNotFound(mail);
                    }));
    }
}

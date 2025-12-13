package io.github.mortuusars.envelope.world.mail.receiver;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.service.pigeonhole.PigeonholeManager;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

public class PigeonholeMailReceiver implements MailReceiver {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Address.Pigeonhole address;

    public PigeonholeMailReceiver(Address.Pigeonhole address) {
        this.address = address;
    }

    @Override
    public Mail receiveMail(ServerLevel level, Mail mail) {
        PigeonholeManager pigeonholeManager = level.getEnvelopeContext().getPigeonholeManager();

        if (mail.isEmpty()) {
            return Mail.EMPTY;
        }

        return pigeonholeManager.getBlockEntityOf(address)
              .map(blockEntity -> {
                  blockEntity.insertMail(mail.writeToLog(DeliveryRecord.arrivedTo(address).at(level.getGameTime())));
                  return Mail.EMPTY;
              })
              .orElseGet(() -> pigeonholeManager.getData(address)
                    .map(data -> {
                        data.insertMail(mail.writeToLog(DeliveryRecord.arrivedTo(address).at(level.getGameTime())));
                        return Mail.EMPTY;
                    })
                    .orElseGet(() -> {
                        LOGGER.info("Cannot deliver mail to pigeonhole '{}': address not found. Returning to sender.", address);
                        return mail.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
                              .withMessage(DeliveryRecord.Message.RECIPIENT_NOT_FOUND));
                    }));
    }
}

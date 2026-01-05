package io.github.mortuusars.envelope.world.mail.entity.mail_service;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.entity.MailEntity;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

public class MailServiceEntity extends MailEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final MailService mailService;

    public MailServiceEntity(MailService mailService) {
        super(Address.MAIL_SERVICE, 1500);
        this.mailService = mailService;
    }

    public MailService getMailService() {
        return mailService;
    }

    // --

    @Override
    public Mail receiveMail(ServerLevel level, Mail mail) {
        return mail.writeToLog(DeliveryRecord.returnedFrom(getAddress()));
    }
}
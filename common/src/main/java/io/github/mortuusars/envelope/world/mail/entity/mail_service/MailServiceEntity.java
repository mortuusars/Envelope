package io.github.mortuusars.envelope.world.mail.entity.mail_service;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.entity.MailEntity;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class MailServiceEntity extends MailEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final MailService mailService;
    private final MailServicePaybackDepartment paybackDepartment;
    private @Nullable MailServiceData data;

    public MailServiceEntity(MailService mailService) {
        super(Address.MAIL_SERVICE, 1500);
        this.mailService = mailService;
        this.paybackDepartment = new MailServicePaybackDepartment(mailService, this::getData);
    }

    public MailService getMailService() {
        return mailService;
    }

    public MailServicePaybackDepartment getPaybackDepartment() {
        return paybackDepartment;
    }

    public @NotNull MailServiceData getData() {
        if (data == null) {
            data = MailServiceData.get(getMailService().getLevel(), "envelope_mail_service_data");
        }
        return data;
    }

    // --

    public void tick() {
        getPaybackDepartment().tick();
    }

    @Override
    public Mail receiveMail(ServerLevel level, Mail mail) {
        if (getPaybackDepartment().shouldHandle(mail)) {
            return getPaybackDepartment().handle(mail);
        }

        return mail.writeToLog(DeliveryRecord.returnedFrom(getAddress()));
    }
}
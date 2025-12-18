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

    private final MailService context;
    private final MailServicePaybackDepartment paybackDepartment;
    private @Nullable MailServiceData data;

    public MailServiceEntity(MailService context) {
        super(Address.MAIL_SERVICE, 1500);
        this.context = context;
        this.paybackDepartment = new MailServicePaybackDepartment(context, this, this::getData);
    }

    public MailService getContext() {
        return context;
    }

    public MailServicePaybackDepartment getPaybackDepartment() {
        return paybackDepartment;
    }

    public @NotNull MailServiceData getData() {
        if (data == null) {
            data = MailServiceData.get(getContext().getLevel(), "envelope_mail_service_data");
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
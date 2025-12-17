package io.github.mortuusars.envelope.world.mail.entity.mail_service;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.entity.MailEntity;
import io.github.mortuusars.envelope.world.service.EnvelopeContext;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class MailService extends MailEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final EnvelopeContext context;
    private final PaybackHandlingDepartment paybackDepartment;
    private @Nullable MailServiceData data;

    public MailService(EnvelopeContext context) {
        super(Address.MAIL_SERVICE, 1500);
        this.context = context;
        this.paybackDepartment = new PaybackHandlingDepartment(context, this, this::getData);
    }

    public EnvelopeContext getContext() {
        return context;
    }

    public PaybackHandlingDepartment getPaybackDepartment() {
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
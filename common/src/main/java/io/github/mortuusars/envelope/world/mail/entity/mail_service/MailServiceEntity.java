package io.github.mortuusars.envelope.world.mail.entity.mail_service;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressLocation;
import io.github.mortuusars.envelope.world.mail.entity.MailEntity;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public class MailServiceEntity extends MailEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final MailService mailService;

    public MailServiceEntity(MailService mailService) {
        super(Address.MAIL_SERVICE, AddressLocation.VIRTUAL);
        this.mailService = mailService;
    }

    public MailService getMailService() {
        return mailService;
    }

    // --

    @Override
    public ItemStack receiveMail(ServerLevel level, ItemStack mail) {
        return returned(mail, DeliveryRecord.Message.REJECTED);
    }
}
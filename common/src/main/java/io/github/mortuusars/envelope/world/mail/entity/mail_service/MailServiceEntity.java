package io.github.mortuusars.envelope.world.mail.entity.mail_service;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressLocation;
import io.github.mortuusars.envelope.world.mail.entity.MailEntity;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public class MailServiceEntity extends MailEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    public MailServiceEntity(MailService service) {
        super(service, service.getAddress(), AddressLocation.VIRTUAL);
    }

    // --

    @Override
    public ItemStack receiveMail(ServerLevel level, Address sender, ItemStack mail) {
        return returned(mail, DeliveryRecord.Message.REJECTED);
    }
}
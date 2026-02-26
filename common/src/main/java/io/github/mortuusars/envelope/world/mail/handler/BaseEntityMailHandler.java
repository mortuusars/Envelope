package io.github.mortuusars.envelope.world.mail.handler;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public class BaseEntityMailHandler implements EntityMailHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final EntityAddress address;
    private final CraftingMailHandler craftingHandler;

    public BaseEntityMailHandler(EntityAddress address) {
        this.address = address;
        this.craftingHandler = new CraftingMailHandler(address);
    }

    public EntityAddress getAddress() {
        return address;
    }

    public CraftingMailHandler getCraftingHandler() {
        return craftingHandler;
    }

    @Override
    public MailHandlingResult handle(MailService service, Delivery delivery) {
        ItemStack mail = delivery.getMail();

        if (mail.isEmpty()) {
            return MailHandlingResult.CONSUME;
        }

        if (Mail.isReturned(delivery.getMail())) {
            //TODO: Lost mail
            LOGGER.info("Mail Entity received returned mail [{}] from '{}'. Voiding.", mail, delivery.getSender());
            return MailHandlingResult.CONSUME;
        }

        MailHandlingResult craftingResult = getCraftingHandler().handle(service, delivery);
        if (craftingResult.isHandled()) {
            return craftingResult;
        }

        for (EntityMailHandler handler : service.getMailEntities().getHandlers(getAddress())) {
            try {
                MailHandlingResult result = handler.handle(service, delivery);
                if (result.isHandled()) {
                    return result;
                }
            } catch (Exception e) {
                String id = service.getMailEntities().getHandlerId(handler).map(ResourceLocation::toString).orElse("unknown");
                LOGGER.error("Entity Mail Handler '{}' of address '{}' failed to handle delivery '{}':", id, handler.getAddress(), delivery, e);
            }
        }

        return MailHandlingResult.returned(mail, DeliveryRecord.Message.REJECTED);
    }
}
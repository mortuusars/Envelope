package io.github.mortuusars.envelope.world.mail.dropoff;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.api.EntityMailDropOffHandlerRegistry;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public class EntityDropOffHandler implements MailDropOffHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final CraftingDropOffHandler craftingHandler = new CraftingDropOffHandler();

    public CraftingDropOffHandler getCraftingHandler() {
        return craftingHandler;
    }

    @Override
    public MailDropOffResult handle(MailDropOffContext context) {
        if (!(context.getTarget() instanceof EntityAddress address)) {
            return MailDropOffResult.PASS;
        }

        ItemStack mail = context.getMail();

        if (mail.isEmpty()) {
            return MailDropOffResult.CONSUME;
        }

        if (context.isReturned()) {
            //TODO: Lost mail
            LOGGER.info("Mail Entity received returned mail [{}] in '{}'. Voiding.", mail, context.getDelivery());
            return MailDropOffResult.CONSUME;
        }

        MailDropOffResult craftingResult = getCraftingHandler().handle(context);
        if (craftingResult.isHandled()) {
            return craftingResult;
        }

        for (MailDropOffHandler handler : EntityMailDropOffHandlerRegistry.getHandlers(address)) {
            try {
                MailDropOffResult result = handler.handle(context);
                if (result.isHandled()) {
                    return result;
                }
            } catch (Exception e) {
                LOGGER.error("Entity Mail Handler '{}' for address '{}' failed to handle delivery '{}':",
                      handler.toString(), address, context.getDelivery(), e);
            }
        }

        return MailDropOffResult.returned(mail, DeliveryRecord.Message.REJECTED);
    }
}
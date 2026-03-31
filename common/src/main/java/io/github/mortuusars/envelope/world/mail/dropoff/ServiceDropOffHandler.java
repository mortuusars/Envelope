package io.github.mortuusars.envelope.world.mail.dropoff;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.api.ServiceDropOffHandlerRegistry;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public class ServiceDropOffHandler implements MailDropOffHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ServiceCraftingDropOffHandler craftingHandler = new ServiceCraftingDropOffHandler();

    public ServiceCraftingDropOffHandler getCraftingHandler() {
        return craftingHandler;
    }

    @Override
    public MailDropOffResult handle(MailDropOffContext context) {
        if (!(context.getTarget() instanceof ServiceAddress address)) {
            return MailDropOffResult.PASS;
        }

        ItemStack mail = context.getMail();

        if (mail.isEmpty()) {
            return MailDropOffResult.CONSUME;
        }

        if (context.isReturned()) {
            LOGGER.info("'{}' received returned mail. Delivery: '{}'. Voiding.", address, context.getDelivery());
            return MailDropOffResult.CONSUME;
        }

        MailDropOffResult craftingResult = getCraftingHandler().handle(context);
        if (craftingResult.isHandled()) {
            return craftingResult;
        }

        for (MailDropOffHandler handler : ServiceDropOffHandlerRegistry.getHandlers(address)) {
            try {
                MailDropOffResult result = handler.handle(context);
                if (result.isHandled()) {
                    return result;
                }
            } catch (Exception e) {
                LOGGER.error("Service Drop Off Handler '{}' for address '{}' has failed to handle delivery '{}':",
                      handler.toString(), address, context.getDelivery(), e);
            }
        }

        return MailDropOffResult.returned(mail, DeliveryRecord.Message.REJECTED);
    }
}
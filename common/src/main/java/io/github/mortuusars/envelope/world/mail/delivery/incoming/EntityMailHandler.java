package io.github.mortuusars.envelope.world.mail.delivery.incoming;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public class EntityMailHandler implements IncomingMailHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final EntityAddress address;
    private final CraftingMailHandler craftingReceiver;

    public EntityMailHandler(EntityAddress address) {
        this.address = address;
        this.craftingReceiver = new CraftingMailHandler(address);
    }

    public CraftingMailHandler getCraftingReceiver() {
        return craftingReceiver;
    }

    @Override
    public ItemStack handle(ServerLevel level, Delivery delivery) {
        ItemStack mail = delivery.getMail();

        if (Mail.isReturned(mail)) {
            LOGGER.info("Mail Entity received returned mail [{}] from '{}'. Voiding.", mail, delivery.getSender());
            return ItemStack.EMPTY;
        }

        ItemStack craftingResult = getCraftingReceiver().handle(level, delivery);
        if (!craftingResult.isEmpty()) {
            return craftingResult;
        }

        return Mail.returned(mail, DeliveryRecord.Message.REJECTED);
    }
}
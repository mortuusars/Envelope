package io.github.mortuusars.envelope.world.mail.receiver;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.item.crafting.*;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.slf4j.Logger;

public class EntityMailReceiver implements MailReceiver {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final EntityAddress address;
    private final EntityCraftingMailReceiver craftingReceiver;

    public EntityMailReceiver(EntityAddress address) {
        this.address = address;
        this.craftingReceiver = new EntityCraftingMailReceiver(address);
    }

    public EntityCraftingMailReceiver getCraftingReceiver() {
        return craftingReceiver;
    }

    @Override
    public ItemStack receiveMail(ServerLevel level, Address sender, ItemStack mail) {
        if (Mail.isReturned(mail)) {
            LOGGER.info("Mail Entity received returned mail [{}] from '{}'. Voiding.", mail, sender);
            return ItemStack.EMPTY;
        }

        ItemStack craftingResult = getCraftingReceiver().receiveMail(level, sender, mail);
        if (!craftingResult.isEmpty()) {
            return craftingResult;
        }

        return returned(mail, DeliveryRecord.Message.REJECTED);
    }
}
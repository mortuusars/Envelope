package io.github.mortuusars.envelope.world.mail;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public interface MailReceiver {
    ItemStack receiveMail(ServerLevel level, ItemStack mail);
}

package io.github.mortuusars.envelope.world.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface ApplicatorItem {
    boolean shouldRenderTooltipWhileCarrying(Level level, ItemStack carried, ItemStack hovered);
}

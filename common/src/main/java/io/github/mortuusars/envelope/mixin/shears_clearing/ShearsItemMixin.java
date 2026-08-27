package io.github.mortuusars.envelope.mixin.shears_clearing;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.mortaar.world.item.ApplicatorItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ShearsItem.class)
public class ShearsItemMixin implements ApplicatorItem {
    @Override
    public boolean shouldRenderSlotTooltipWhileCarrying(Player player, AbstractContainerMenu menu, Slot slot, ItemStack carried) {
        //TODO: Should probably check if we have something to remove from the mail
        return slot.getItem().is(Envelope.Tags.Items.MAILABLE);
    }
}

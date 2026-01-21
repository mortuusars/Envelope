package io.github.mortuusars.envelope.mixin.shears_clearing;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.ApplicatorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ShearsItem.class)
public class ShearsItemMixin implements ApplicatorItem {
    @SuppressWarnings({"AddedMixinMembersNamePattern"}) // This could cause crash due to name conflict, but probability of it should be pretty low.
    @Override
    public boolean shouldRenderTooltipWhileCarrying(Level level, ItemStack carried, ItemStack hovered) {
        return hovered.is(Envelope.Tags.Items.MAILABLE);
    }
}

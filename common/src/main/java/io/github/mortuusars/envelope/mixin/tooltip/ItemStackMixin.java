package io.github.mortuusars.envelope.mixin.tooltip;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.mortuusars.envelope.EnvelopeClient;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements DataComponentHolder {
    @ModifyReturnValue(method = "getTooltipImage", at = @At("RETURN"))
    private Optional<TooltipComponent> appendHoverText(Optional<TooltipComponent> original) {
        return EnvelopeClient.TooltipComponents.modifyTooltipImage(((ItemStack) (Object) this), original);
    }
}
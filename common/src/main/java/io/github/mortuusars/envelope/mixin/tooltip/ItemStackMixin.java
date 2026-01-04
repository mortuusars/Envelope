package io.github.mortuusars.envelope.mixin.tooltip;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.mortuusars.envelope.EnvelopeClient;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements DataComponentHolder {
    @ModifyReturnValue(method = "getTooltipImage", at = @At("RETURN"))
    private Optional<TooltipComponent> getTooltipImage(Optional<TooltipComponent> original) {
        return EnvelopeClient.TooltipComponents.modifyTooltipImage(((ItemStack) (Object) this), original);
    }

    @Inject(method = "getTooltipLines",
          at = @At(value = "INVOKE",
          ordinal = 0,
          target = "Lnet/minecraft/world/item/ItemStack;addToTooltip(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V"))
    private void appendHoverText(Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipFlag,
                                 CallbackInfoReturnable<List<Component>> cir, @Local Consumer<Component> consumer) {
        EnvelopeClient.TooltipComponents.appendTooltipLines(((ItemStack) (Object) this), consumer, tooltipContext, player, tooltipFlag);
    }
}
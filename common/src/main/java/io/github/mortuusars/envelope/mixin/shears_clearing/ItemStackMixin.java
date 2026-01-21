package io.github.mortuusars.envelope.mixin.shears_clearing;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow
    public abstract boolean is(Item item);

    @Shadow
    public abstract Item getItem();

    @WrapOperation(method = "overrideStackedOnOther",
          at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/item/Item;overrideStackedOnOther(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/inventory/ClickAction;Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean overrideStackedOnOther(Item instance, ItemStack stack, Slot slot, ClickAction action, Player player, Operation<Boolean> original) {
        if (original.call(instance, stack, slot, action, player)) {
            return true;
        }

        if (getItem() instanceof ShearsItem) {
            return Mail.handleShearsUse(stack, slot, action, player);
        }

        return false;
    }
}

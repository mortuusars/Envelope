package io.github.mortuusars.envelope.mixin.dispenser_waste_scooping;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.dispenser.WasteScoopingDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DispenserBlock.class)
public class DispenserBlockMixin {
    @Inject(method = "getDispenseMethod", at = @At("HEAD"), cancellable = true)
    private void onGetDispenseMethod(Level level, ItemStack item, CallbackInfoReturnable<DispenseItemBehavior> cir) {
        if (Config.Server.PIGEONHOLE_DISPENSER_WASTE_SCOOPING.get() && item.is(Envelope.Tags.Items.WASTE_SCOOPABLE)) {
            cir.setReturnValue(WasteScoopingDispenseItemBehavior.INSTANCE);
        }
    }
}

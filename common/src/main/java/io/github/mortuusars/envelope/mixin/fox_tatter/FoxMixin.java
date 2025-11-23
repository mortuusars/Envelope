package io.github.mortuusars.envelope.mixin.fox_tatter;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Fox.class)
public class FoxMixin {
    @ModifyVariable(method = "spitOutItem", at = @At("HEAD"), argsOnly = true)
    private ItemStack onSpitOutItem(ItemStack stack) {
        if (stack.is(Envelope.Items.LETTER.get())) {
            return stack.transmuteCopy(Envelope.Items.TATTERED_LETTER.get());
        }
        return stack;
    }
}

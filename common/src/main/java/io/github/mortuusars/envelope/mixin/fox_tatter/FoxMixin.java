package io.github.mortuusars.envelope.mixin.fox_tatter;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.LetterItem;
import io.github.mortuusars.envelope.world.item.SealedLetterItem;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Fox.class)
public class FoxMixin {
    @ModifyVariable(method = "spitOutItem", at = @At("HEAD"), argsOnly = true)
    private ItemStack onSpitOutItem(ItemStack stack) {
        if (Config.Server.FOX_LETTER_TATTERING.get() && (stack.getItem() instanceof LetterItem || stack.getItem() instanceof SealedLetterItem)) {
            stack.set(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE);
        }
        return stack;
    }
}

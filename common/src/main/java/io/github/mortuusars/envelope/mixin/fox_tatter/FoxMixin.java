package io.github.mortuusars.envelope.mixin.fox_tatter;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.world.item.LetterItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Fox.class)
public abstract class FoxMixin extends Animal {
    protected FoxMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @ModifyVariable(method = "spitOutItem", at = @At("HEAD"), argsOnly = true)
    private ItemStack onSpitOutItem(ItemStack stack) {
        if (Config.Server.FOX_LETTER_TATTERING.get()) {
            return LetterItem.tatterIfApplicable(level(), stack);
        }
        return stack;
    }
}

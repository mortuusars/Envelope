package io.github.mortuusars.envelope.mixin.equine_assurance_bureau;

import io.github.mortuusars.envelope.world.mail.service.EquineAssuranceBureau;
import net.minecraft.advancements.critereon.TameAnimalTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TameAnimalTrigger.class)
public abstract class TameAnimalTriggerMixin {
    @Inject(method = "trigger", at = @At("RETURN"))
    private void onTrigger(ServerPlayer player, Animal entity, CallbackInfo ci) {
        EquineAssuranceBureau.onTameAnimal(player, entity);
    }
}

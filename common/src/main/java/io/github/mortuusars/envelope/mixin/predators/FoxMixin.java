package io.github.mortuusars.envelope.mixin.predators;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Fox.class)
public abstract class FoxMixin extends Animal {
    protected FoxMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "registerGoals", at = @At("RETURN"))
    private void registerGoals(CallbackInfo ci) {
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>((Fox) (Object) this, Pigeon.class, 10,
              false, false, entity -> Config.Server.PIGEON_HUNTED_BY_FOX.get()));
    }
}

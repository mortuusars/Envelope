package io.github.mortuusars.envelope.mixin.pigeon_on_shoulder;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "playShoulderEntityAmbientSound", at = @At("RETURN"))
    private void playShoulderEntityAmbientSound(CompoundTag entityCompound, CallbackInfo ci) {
        if (entityCompound == null
                || entityCompound.getBoolean("Silent")
                || level().random.nextInt(200) != 0) {
            return;
        }

        String entityId = entityCompound.getString("id");
        EntityType.byString(entityId)
                .filter(entityType -> entityType == Envelope.EntityTypes.PIGEON.get())
                .ifPresent(type -> level().playSound(null, getX(), getY(), getZ(),
                        Envelope.SoundEvents.PIGEON_AMBIENT.get(), getSoundSource(), 1.0F, Pigeon.getPitch(this.level().random))
                );
    }
}

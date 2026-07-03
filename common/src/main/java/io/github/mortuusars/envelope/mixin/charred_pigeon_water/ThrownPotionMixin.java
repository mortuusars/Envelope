package io.github.mortuusars.envelope.mixin.charred_pigeon_water;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.mortuusars.envelope.world.entity.CharredPigeon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("LocalMayUseName")
@Mixin(ThrownPotion.class)
public abstract class ThrownPotionMixin extends ThrowableItemProjectile implements ItemSupplier {
    public ThrownPotionMixin(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "applyWater", at = @At("RETURN"))
    private void onApplyWater(CallbackInfo ci, @Local AABB boundingBox) {
        if (level() instanceof ServerLevel level) {
            for (CharredPigeon pigeon : level.getEntitiesOfClass(CharredPigeon.class, boundingBox)) {
                if (pigeon.canConvert()) {
                    pigeon.convert(level);
                }
            }
        }
    }
}

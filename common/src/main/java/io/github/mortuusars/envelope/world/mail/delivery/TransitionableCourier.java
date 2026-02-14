package io.github.mortuusars.envelope.world.mail.delivery;

import io.github.mortuusars.envelope.world.mail.delivery.background.BackgroundCourier;
import io.github.mortuusars.envelope.world.entity.SpawnableEntityData;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface TransitionableCourier extends Courier {
    SpawnableEntityData toSpawnableData();

    void setDelivery(Delivery delivery);

    default BackgroundCourier transitionToBackground(ServerLevel level) {
        Delivery delivery = getCurrentDelivery().orElseThrow(() -> new IllegalStateException("Cannot transition: courier is not delivering."));
        BackgroundCourier backgroundCourier = new BackgroundCourier(toSpawnableData(), getOrigin(), delivery);
        MailService.of(level).getBackgroundDelivery().addCourier(backgroundCourier);
        onVanished(level);
        ((Entity) this).discard();
        return backgroundCourier;
    }

    default void onAppeared(ServerLevel level) {
        Vec3 pos = ((Entity) this).position();
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 16, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1, 1);
    }

    default void onVanished(ServerLevel level) {
        Vec3 pos = ((Entity) this).position();
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 16, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1, 1);
    }
}
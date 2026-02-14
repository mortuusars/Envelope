package io.github.mortuusars.envelope.world.entity.spawner;

import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.mail.delivery.background.FinishedBackgroundCourier;
import io.github.mortuusars.envelope.world.mail.delivery.background.BackgroundDelivery;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FinishedBackgroundCourierSpawner implements Spawner {
    protected static final int SPAWN_ATTEMPT_DELAY = 10;
    protected int nextAttemptDelay;

    @Override
    public void tick(ServerLevel level) {
        nextAttemptDelay--;
        if (nextAttemptDelay > 0) {
            return;
        }

        nextAttemptDelay = SPAWN_ATTEMPT_DELAY;

        BackgroundDelivery backgroundDelivery = MailService.of(level).getBackgroundDelivery();
        List<FinishedBackgroundCourier> couriers = backgroundDelivery.getFinishedCouriers();

        if (couriers.isEmpty()) {
            return;
        }

        FinishedBackgroundCourier courier = Util.getRandom(couriers, level.getRandom());

        @Nullable BlockPos spawnPos = Position.findNearbyHeightmapSpawnPosition(level, courier.spawnPos(), 2);
        if (spawnPos == null) {
            return;
        }

        @Nullable Entity entity = courier.entityData().createEntity(level);
        if (entity == null) {
            return;
        }

        entity.moveTo(spawnPos, entity.getYRot(), entity.getXRot());
        level.addFreshEntityWithPassengers(entity);

        if (!courier.undeliveredMail().isEmpty()) {
            entity.spawnAtLocation(courier.undeliveredMail());
        }

        backgroundDelivery.removeFinishedCourier(courier);
    }
}
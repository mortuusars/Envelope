package io.github.mortuusars.envelope.world.entity.spawner;

import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.delivery.background.FinishedBackgroundCourier;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundDelivery;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CustomSpawner;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FinishedBackgroundCourierSpawner implements CustomSpawner {
    protected static final int SPAWN_ATTEMPT_DELAY = 10;
    protected int nextAttemptDelay;

    @Override
    public int tick(ServerLevel level, boolean spawnEnemies, boolean spawnFriendlies) {
        nextAttemptDelay--;
        if (nextAttemptDelay > 0) {
            return 0;
        }

        nextAttemptDelay = SPAWN_ATTEMPT_DELAY;

        BackgroundDelivery backgroundDelivery = MailService.of(level).getBackgroundDelivery();
        List<FinishedBackgroundCourier> couriers = backgroundDelivery.getFinishedCouriers();

        if (couriers.isEmpty()) {
            return 0;
        }

        FinishedBackgroundCourier courier = Util.getRandom(couriers, level.getRandom());

        @Nullable BlockPos spawnPos = Position.findNearbyHeightmapSpawnPosition(level, courier.spawnPos(), 2);
        if (spawnPos == null) {
            return 0;
        }

        @Nullable Entity entity = courier.entityData().createEntity(level);
        if (entity == null) {
            return 0;
        }

        entity.moveTo(spawnPos, entity.getYRot(), entity.getXRot());
        level.addFreshEntityWithPassengers(entity);

        if (!courier.undeliveredMail().isEmpty()) {
            Containers.dropItemStack(level, entity.getX(), entity.getY(), entity.getZ(), courier.undeliveredMail());
        }

        backgroundDelivery.removeFinishedCourier(courier);
        return 0;
    }
}
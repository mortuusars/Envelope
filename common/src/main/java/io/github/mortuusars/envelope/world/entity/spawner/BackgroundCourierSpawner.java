package io.github.mortuusars.envelope.world.entity.spawner;

import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.TransitionableCourier;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundCourier;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundDelivery;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.CustomSpawner;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BackgroundCourierSpawner implements CustomSpawner {
    @Override
    public int tick(ServerLevel level, boolean spawnEnemies, boolean spawnFriendlies) {
        BackgroundDelivery backgroundDelivery = level.getEnvelopeContext().getBackgroundDelivery();

        List<BackgroundCourier> spawnableCouriers = backgroundDelivery.getCouriers()
              .stream()
              .filter(courier -> !courier.delivery().isFinished() && !courier.delivery().getCurrentPhase().isTraveling())
              .toList();

        if (spawnableCouriers.isEmpty()) {
            return 0;
        }

        BackgroundCourier courier = Util.getRandom(spawnableCouriers, level.getRandom());
        trySpawn(level, courier);

        return 0;
    }

    protected void trySpawn(ServerLevel level, BackgroundCourier backgroundCourier) {
        Delivery delivery = backgroundCourier.delivery();

        @Nullable BlockPos spawnPos = delivery.estimateCurrentPos()
              .filter(level::isLoaded)
              .map(pos -> Position.aboveGround(level, pos, 2))
              .filter(pos -> Position.isInSimulationDistance(level, pos))
              .orElse(null);

        if (spawnPos == null) {
            return;
        }

        @Nullable Entity entity = backgroundCourier.getEntityData().createEntity(level);
        if (entity instanceof TransitionableCourier<?> courier) {
            entity.moveTo(spawnPos, entity.getYRot(), entity.getXRot());

            level.addFreshEntityWithPassengers(entity);

            courier.onAppeared(level);
            courier.continueDelivery(level, delivery);

            level.getEnvelopeContext().getBackgroundDelivery().removeCourier(backgroundCourier);
        }
    }
}

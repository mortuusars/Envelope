package io.github.mortuusars.envelope.world.entity.spawner;

import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.TransitionableCourier;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundCourier;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundDelivery;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.Util;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.CustomSpawner;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BackgroundCourierSpawner implements CustomSpawner {
    @Override
    public int tick(ServerLevel level, boolean spawnEnemies, boolean spawnFriendlies) {
        BackgroundDelivery backgroundDelivery = MailService.of(level).getBackgroundDelivery();

        List<BackgroundCourier> spawnableCouriers = backgroundDelivery.getCouriers()
              .stream()
              .filter(this::canSpawn)
              .toList();

        if (spawnableCouriers.isEmpty()) {
            return 0;
        }

        BackgroundCourier courier = Util.getRandom(spawnableCouriers, level.getRandom());
        trySpawn(level, courier);

        return 0;
    }

    private boolean canSpawn(BackgroundCourier courier) {
        if (courier.getOrigin().isService()
              && courier.getDelivery().getMail().isEmpty()
              && !courier.getDelivery().getPhase().isOnRecipientSide()) {
            return false; // Don't spawn service couriers when it doesn't make sense
        }

        return courier.getDelivery().getPhase().isSpawnable();
    }

    protected void trySpawn(ServerLevel level, BackgroundCourier backgroundCourier) {
        Delivery delivery = backgroundCourier.getDelivery();

        Position.estimateCourierPosition(delivery)
              .filter(level::isLoaded)
              .map(pos -> Position.aboveGround(level, pos, 2))
              .filter(pos -> Position.isInSimulationDistance(level, pos))
              .ifPresent(pos -> {
                  @Nullable Entity entity = backgroundCourier.getEntityData().createEntity(level);
                  if (entity instanceof TransitionableCourier courier) {
                      entity.moveTo(pos, entity.getYRot(), entity.getXRot());

                      level.addFreshEntityWithPassengers(entity);

                      courier.onAppeared(level);
                      courier.setDelivery(delivery);

                      MailService.of(level).getBackgroundDelivery().removeCourier(backgroundCourier);
                  }
              });
    }
}

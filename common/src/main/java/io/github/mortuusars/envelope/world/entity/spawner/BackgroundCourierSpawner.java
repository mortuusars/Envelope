package io.github.mortuusars.envelope.world.entity.spawner;

import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.TransitionableCourier;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundCourier;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundDelivery;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.delivery.route.DeliveryRoute;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class BackgroundCourierSpawner implements CustomSpawner {
    @Override
    public int tick(ServerLevel level, boolean spawnEnemies, boolean spawnFriendlies) {
        BackgroundDelivery backgroundDelivery = MailService.of(level).getBackgroundDelivery();

        List<BackgroundCourier> spawnableCouriers = backgroundDelivery.getCouriers()
              .stream()
              .filter(this::isSpawnable)
              .toList();

        if (spawnableCouriers.isEmpty()) {
            return 0;
        }

        BackgroundCourier courier = Util.getRandom(spawnableCouriers, level.getRandom());
        trySpawn(level, courier);

        return 0;
    }

    protected void trySpawn(ServerLevel level, BackgroundCourier backgroundCourier) {
        Delivery delivery = backgroundCourier.getDelivery();

        estimateCourierPosition(delivery)
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

    protected Optional<BlockPos> estimateCourierPosition(Delivery delivery) {
        DeliveryRoute.Segment segment = delivery.getRoute().getSegment(delivery.getPhase());
        if (segment.startPos().isPresent() && segment.endPos().isPresent()) {
            Vec3 pos = Position.lerp(segment.startPos().get(), segment.endPos().get(), delivery.getPhaseProgress());
            return Optional.of(BlockPos.containing(pos));
        }
        return Optional.empty();
    }

    protected boolean isSpawnable(BackgroundCourier courier) {
        return !courier.getDelivery().getPhase().isTraveling()
              && courier.getDelivery().getPhase() != DeliveryPhase.FINISHED;
    }
}

package io.github.mortuusars.envelope.world.entity.spawner;

import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import io.github.mortuusars.envelope.world.mail.delivery.TransitionableCourier;
import io.github.mortuusars.envelope.world.mail.delivery.background.BackgroundCourier;
import io.github.mortuusars.envelope.world.mail.delivery.background.BackgroundDelivery;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.Util;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BackgroundCourierSpawner implements Spawner {
    @Override
    public void tick(ServerLevel level) {
        BackgroundDelivery backgroundDelivery = MailService.of(level).getBackgroundDelivery();

        List<BackgroundCourier> spawnableCouriers = backgroundDelivery.getCouriers()
              .stream()
              .filter(this::canSpawn)
              .toList();

        if (spawnableCouriers.isEmpty()) {
            return;
        }

        BackgroundCourier courier = Util.getRandom(spawnableCouriers, level.getRandom());
        trySpawn(level, courier);
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

        int duration = backgroundCourier.getPhaseDuration(level, delivery, delivery.getPhase());
        int progress = delivery.getPhaseProgress();
        float completeness = Mth.clamp(progress / (float) duration, 0f, 1f);

        delivery.getRoute().getSegment(delivery.getPhase()).getCurrentLocation(completeness)
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

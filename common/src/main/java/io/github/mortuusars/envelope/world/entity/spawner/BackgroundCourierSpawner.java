package io.github.mortuusars.envelope.world.entity.spawner;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import io.github.mortuusars.envelope.world.mail.delivery.PhysicalCourier;
import io.github.mortuusars.envelope.world.mail.delivery.background.BackgroundCourier;
import io.github.mortuusars.envelope.world.mail.delivery.background.BackgroundDelivery;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

public class BackgroundCourierSpawner extends Spawner {
    private static final Logger LOGGER = LogUtils.getLogger();

    public BackgroundCourierSpawner() {
        super(10);
    }

    @Override
    public boolean worksIn(ServerLevel level) {
        return MailService.operatesIn(level);
    }

    @Override
    public void spawn(ServerLevel level) {
        if (Config.Server.DELIVERY_SPAWNING_RESPECTS_DOMOBSPAWNING_RULE.get()
              && !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }

        BackgroundCourier courier = selectCourier(level);
        if (courier != null) {
            trySpawn(level, courier);
        }
    }

    private @Nullable BackgroundCourier selectCourier(ServerLevel level) {
        BackgroundDelivery backgroundDelivery = MailService.of(level).getBackgroundDelivery();

        List<BackgroundCourier> spawnableCouriers = backgroundDelivery.getActiveCouriers()
              .stream()
              .filter(this::canSpawn)
              .toList();

        if (spawnableCouriers.isEmpty()) {
            return null;
        }

        return Util.getRandom(spawnableCouriers, level.getRandom());
    }

    protected boolean canSpawn(BackgroundCourier courier) {
        if (courier.getOrigin().isService()
              && courier.getDelivery().getMail().isEmpty()
              && !courier.getDelivery().getPhase().isOnRecipientSide()) {
            return false; // Don't spawn service couriers when it doesn't make sense
        }

        return courier.getDelivery().getPhase().isSpawnable();
    }

    protected Optional<BlockPos> getSpawnPos(ServerLevel level, BackgroundCourier courier) {
        Delivery delivery = courier.getDelivery();

        int duration = courier.getPhaseDuration(level, delivery, delivery.getPhase());
        int progress = delivery.getPhaseProgress();
        float completeness = Mth.clamp(progress / (float) duration, 0f, 1f);

        return delivery.getRoute().getSegment(delivery.getPhase()).getCurrentLocation(level, completeness)
              .filter(level::isLoaded)
              .map(pos -> Position.aboveGround(level, pos, 2))
              .filter(pos -> Position.isInSimulationDistance(level, pos));
    }

    protected void trySpawn(ServerLevel level, BackgroundCourier backgroundCourier) {
        getSpawnPos(level, backgroundCourier)
              .ifPresent(pos -> {
                  @Nullable Entity entity = backgroundCourier.getSpawnableEntityData().createEntity(level);
                  backgroundCourier.setRemoved();

                  if (entity == null) {
                      return;
                  }

                  entity.moveTo(pos, entity.getYRot(), entity.getXRot());
                  level.addFreshEntityWithPassengers(entity);

                  if (entity instanceof PhysicalCourier courier) {
                      courier.onAppeared(level);
                      courier.setDelivery(backgroundCourier.getDelivery());
                  } else {
                      ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                      LOGGER.error("Failed to spawn BackgroundCourier properly: entity is not a Courier, but {}. Tag: {}",
                            id, backgroundCourier.getSpawnableEntityData().data());
                  }
              });
    }
}

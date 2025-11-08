package io.github.mortuusars.envelope.world.entity.spawner;

import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.TransitionableCourier;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundCourier;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundDelivery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DeliveringCourierSpawner implements CustomSpawner {
    @Override
    public int tick(ServerLevel level, boolean spawnEnemies, boolean spawnFriendlies) {
        if (!spawnFriendlies || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return 0;
        }

        BackgroundDelivery backgroundDelivery = level.getEnvelopeContext().getBackgroundDelivery();
        List<BackgroundCourier> couriers = backgroundDelivery.getCouriers();

        if (couriers.isEmpty()) {
            return 0;
        }

        BackgroundCourier backgroundCourier = couriers.getFirst();
        Delivery delivery = backgroundCourier.delivery();

        if (delivery.getCurrentPhase().isTraveling()) {
            return 0;
        }

        @Nullable BlockPos spawnPos = delivery.estimateCurrentPos()
              .map(pos -> Position.aboveGround(level, pos, 2))
              .filter(pos -> Position.isInSafeSimulationDistance(level, pos))
              .orElse(null);

        if (spawnPos == null) {
            return 0;
        }

        @Nullable Entity entity = backgroundCourier.getEntityData().createEntity(level);
        if (entity instanceof TransitionableCourier courier) {
            entity.moveTo(spawnPos, entity.getYRot(), entity.getXRot());
            if (backgroundCourier.getEntityData().isNew() && entity instanceof Mob mob) {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.SPAWN_EGG, null);
            }

            if (backgroundCourier.isService()) {
                courier.setService(true);
            }

            level.addFreshEntityWithPassengers(entity);

            courier.onAppeared(level);
            courier.continueDelivery(level, delivery);

            backgroundDelivery.removeCourier(backgroundCourier);
        }

        return 0;
    }
}

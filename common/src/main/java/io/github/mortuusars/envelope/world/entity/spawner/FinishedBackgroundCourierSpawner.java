package io.github.mortuusars.envelope.world.entity.spawner;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.world.GameTime;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.delivery.background.FinishedBackgroundCourier;
import io.github.mortuusars.envelope.world.mail.delivery.background.BackgroundDelivery;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FinishedBackgroundCourierSpawner extends Spawner {
    public FinishedBackgroundCourierSpawner() {
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

        if (entity instanceof Pigeon pigeon) {
            long tiredUntil = courier.finishedAt() + Config.Server.PIGEON_TIRED_AFTER_DELIVERY_TICKS.get();
            int tiredTicks = Math.max(0, GameTime.of(level).remainingTo(tiredUntil).getAsInt());
            pigeon.setTiredTicks(tiredTicks);
        }

        backgroundDelivery.removeFinishedCourier(courier);
    }
}
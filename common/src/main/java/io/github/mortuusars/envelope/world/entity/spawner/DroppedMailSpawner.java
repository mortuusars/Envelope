package io.github.mortuusars.envelope.world.entity.spawner;

import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.entity.spawning.SpawnableItem;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.delivery.background.BackgroundDelivery;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DroppedMailSpawner extends Spawner {
    public DroppedMailSpawner() {
        super(10);
    }

    @Override
    public boolean worksIn(ServerLevel level) {
        return MailService.operatesIn(level);
    }

    @Override
    public void spawn(ServerLevel level) {
        BackgroundDelivery backgroundDelivery = MailService.of(level).getBackgroundDelivery();
        List<SpawnableItem> items = backgroundDelivery.getDroppedMail();

        if (items.isEmpty()) {
            return;
        }

        SpawnableItem item = Util.getRandom(items, level.getRandom());

        @Nullable BlockPos spawnPos = getSpawnPos(level, item);
        if (spawnPos == null) {
            return;
        }

        Entity entity = new ItemEntity(level,
              item.pos().getX() + 0.5,
              item.pos().getY() + 0.5,
              item.pos().getZ() + 0.5,
              item.item());
        entity.moveTo(spawnPos, entity.getYRot(), entity.getXRot());
        level.addFreshEntityWithPassengers(entity);

        backgroundDelivery.removeDroppedMail(item);
    }

    public @Nullable BlockPos getSpawnPos(ServerLevel level, SpawnableItem item) {
        if (Position.isInSimulationDistance(level, item.pos())) {
            return item.pos();
        }
        return null;
    }
}

package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class PigeonSearchForFoodGoal extends Goal {
    protected final Pigeon pigeon;

    public PigeonSearchForFoodGoal(Pigeon pigeon) {
        this.pigeon = pigeon;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!Config.Server.PIGEON_EATS_SEEDS.get()
              || pigeon.isDelivering()
              || pigeon.isSitting()
              || !pigeon.getMainHandItem().isEmpty()
              || pigeon.getEatingTicks() > 0
              || pigeon.getLastHurtByMob() != null
              || pigeon.getRandom().nextInt(reducedTickDelay(10)) != 0) {
            return false;
        }
        return !findItems().isEmpty();
    }

    @Override
    public void tick() {
        if (!pigeon.getMainHandItem().isEmpty()) {
            return;
        }
        List<ItemEntity> items = findItems();
        if (!items.isEmpty()) {
            @Nullable Path path = pigeon.getNavigation().createPath(items.getFirst(), 0);
            if (path != null) pigeon.getNavigation().moveTo(path, 0.5);
        }
    }

    @Override
    public void start() {
        List<ItemEntity> items = findItems();
        if (!items.isEmpty()) {
            @Nullable Path path = pigeon.getNavigation().createPath(items.getFirst(), 0);
            if (path != null) pigeon.getNavigation().moveTo(path, 0.5);
        }
    }

    protected List<ItemEntity> findItems() {
        return pigeon.level().getEntitiesOfClass(
              ItemEntity.class,
              pigeon.getBoundingBox().inflate(12),
              Pigeon.SEARCHED_FOOD_ITEMS_PREDICATE);
    }
}
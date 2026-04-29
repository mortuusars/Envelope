package io.github.mortuusars.envelope.world;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.Ticks;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class VillagerPigeonFeeding {
    public static final int VILLAGER_PIGEON_FOOD_PICKUP_DELAY = (int) Ticks.fromSeconds(30);
    public static final int VILLAGER_PIGEON_FEED_BASE_COOLDOWN = (int) Ticks.fromSeconds(5);

    public static boolean tryFeed(Villager villager) {
        if (!Config.Server.PIGEON_EATS_SEEDS.get()) {
            return false;
        }

        if (Config.Server.VILLAGER_FEEDING_PIGEONS_NITWIT_ONLY.get()
              && villager.getVillagerData().getProfession() != VillagerProfession.NITWIT) {
            return false;
        }

        @Nullable PositionTracker tracker = villager.getBrain().getMemory(MemoryModuleType.LOOK_TARGET).orElse(null);
        if (!(tracker instanceof EntityTracker entityTracker && entityTracker.getEntity() instanceof Pigeon pigeon)
              || villager.distanceTo(pigeon) > 4
              || !villager.level().canSeeSky(villager.blockPosition())
              || villager.getRandom().nextInt(60) != 0) {
            return false;
        }

        BehaviorUtils.throwItem(villager,
              getFoodItem(villager),
              pigeon.position().offsetRandom(villager.getRandom(), 1.5f),
              new Vec3(0.3, 0.3, 0.3),
              0.5f);
        villager.playSound(Envelope.SoundEvents.VILLAGER_THROW_PIGEON_FOOD.get());

        // Adding delay so nearby villagers (including the feeding one) won't pick up thrown food immediately
        AABB area = new AABB(villager.blockPosition()).inflate(16);
        villager.level().getEntitiesOfClass(Villager.class, area)
              .forEach(entity -> {
                  if (entity instanceof FeedingVillager feedingVillager) {
                      feedingVillager.envelope$addPigeonFoodPickupDelay(getPigeonFoodPickupDelay(entity));
                  }
              });

        return true;
    }

    public static int getPigeonFoodPickupDelay(Villager villager) {
        return VILLAGER_PIGEON_FOOD_PICKUP_DELAY;
    }

    public static int getFeedCooldown(Villager villager) {
        return VILLAGER_PIGEON_FEED_BASE_COOLDOWN * (villager.getRandom().nextInt(1, 3));
    }

    public static ItemStack getFoodItem(Villager villager) {
        float chance = villager.getRandom().nextFloat();

        TagKey<Item> pool = chance >= 0.98f ? Envelope.Tags.Items.VILLAGER_FEEDING_PIGEON_FOOD_RARE
              : chance >= 0.78f ? Envelope.Tags.Items.VILLAGER_FEEDING_PIGEON_FOOD_UNCOMMON
              : Envelope.Tags.Items.VILLAGER_FEEDING_PIGEON_FOOD_COMMON;

        Item food = BuiltInRegistries.ITEM.getRandomElementOf(pool, villager.getRandom())
              .map(Holder::value)
              .orElse(Items.WHEAT_SEEDS);

        return new ItemStack(food, getFoodCount(villager));
    }

    public static int getFoodCount(Villager villager) {
        int pigeonsNearby = villager.level().getEntitiesOfClass(Pigeon.class, villager.getBoundingBox().inflate(8)).size();
        return villager.getRandom().nextInt(1, Mth.clamp(pigeonsNearby, 1, 3) + 1);
    }

    public interface FeedingVillager {
        void envelope$addPigeonFoodPickupDelay(int delay);
    }
}

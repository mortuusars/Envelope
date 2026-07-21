package io.github.mortuusars.envelope.event;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.dispenser.PlaceBlockDispenseItemBehavior;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.PigeonVariant;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class CommonEvents {
    public static void commonSetup() {
        MailService.init();

        PlaceBlockDispenseItemBehavior placeBlockBehavior = new PlaceBlockDispenseItemBehavior();
        DispenserBlock.registerBehavior(Envelope.Items.PACKAGE.get(), placeBlockBehavior);
        DispenserBlock.registerBehavior(Envelope.Items.SEALED_PACKAGE.get(), placeBlockBehavior);
    }

    public static void levelTick(Level level) {
    }

    /**
     * Fired before level data storage is saved.
     */
    public static void saveLevelData(ServerLevel level) {
    }

    public static void entityLeaveLevel(Level level, Entity entity) {
    }

    public static void livingDeath(LivingEntity entity, DamageSource source) {
        if (entity.getRandom().nextDouble() < Config.Server.ARCHIMEDES_CHANCE.get()
              && entity.level().dimension() == Level.OVERWORLD
              && source.getEntity() instanceof ServerPlayer player
              && entity.getType().is(Envelope.Tags.EntityTypes.SPAWNS_ARCHIMEDES)
              && source.is(Envelope.Tags.DamageTypes.SPAWNS_ARCHIMEDES)
              && player.serverLevel().getEntitiesOfClass(
                    Pigeon.class, new AABB(entity.blockPosition()).inflate(48, 32, 48)).size() < 8) {
            @Nullable Pigeon pigeon = Envelope.EntityTypes.PIGEON.get().spawn(player.serverLevel(), entity.blockPosition(), MobSpawnType.TRIGGERED);
            if (pigeon == null) {
                Envelope.LOGGER.warn("Cannot spawn Archimedes :(");
                return;
            }

            pigeon.setVariant(PigeonVariant.getOrThrow(entity.registryAccess(), PigeonVariant.ARCHIMEDES));
            pigeon.moveTo(entity.getBoundingBox().getCenter());
            pigeon.getWanderGoal().trigger();
            pigeon.onAppeared(player.serverLevel());
            Vec3 direction = entity.position().subtract(source.getSourcePosition());
            pigeon.setDeltaMovement(direction.normalize().scale(0.75));

            Envelope.CriteriaTriggers.SPAWN_ARCHIMEDES.get().trigger(player);
        }
    }
}
